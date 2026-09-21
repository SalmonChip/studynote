// ============================================================
// 秒杀压测 + 全链路诊断脚本
//
// 用法：
//   node scripts/seckill-loadtest.js             # 默认 50 并发，活动 1
//   node scripts/seckill-loadtest.js 20 1        # 20 并发，活动 1
//
// 它做四件事：
//   1. 打之前：读 Redis 的库存 / 已购集合
//   2. 并发打 N 个请求（每个用户一个 token，全并发，不排队）
//   3. 按「HTTP状态 + 业务code + message」分组，列出每个返回码下有谁
//   4. 打之后：再读一次 Redis，并算出 Redis 库存掉了多少
//
// 最后把「Redis 掉了几 / 成功几单」摆在一起对比 —— 如果两个数对不上，
// 就说明有请求根本没走到 Lua 那一步。
// ============================================================

const fs = require('fs');
const net = require('net');

// ---------- 配置 ----------
const CONCURRENCY = parseInt(process.argv[2] || '50', 10);
const ACTIVITY_ID = parseInt(process.argv[3] || '1', 10);
const CSV_PATH = 'C:/Users/fish/Desktop/50用户并发token.csv';
const BASE_URL = 'http://localhost:8080';
const REDIS = { host: '127.0.0.1', port: 6379 };

// ---------- 1. 读 CSV ----------
function loadUsers() {
  const raw = fs.readFileSync(CSV_PATH, 'utf8').replace(/^\uFEFF/, ''); // 去 UTF-8 BOM
  return raw
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter(Boolean)
    .slice(1) // 去表头
    .map((line) => {
      const i = line.indexOf(',');
      return { userId: line.slice(0, i).trim(), token: line.slice(i + 1).trim() };
    });
}

// ---------- 2. 极简 Redis 客户端（RESP 协议，只够读几个 key）----------
function redisCmd(cmds) {
  return new Promise((resolve, reject) => {
    const sock = net.connect(REDIS.port, REDIS.host, () => {
      sock.write(cmds.map((c) => c + '\r\n').join('')); // 用 inline 命令，省得拼 RESP
    });
    let buf = Buffer.alloc(0);
    sock.on('data', (d) => (buf = Buffer.concat([buf, d])));
    sock.on('error', reject);
    setTimeout(() => {
      sock.destroy();
      try {
        let i = 0;
        const readLine = () => {
          const j = buf.indexOf('\r\n', i);
          const s = buf.slice(i, j).toString();
          i = j + 2;
          return s;
        };
        const readVal = () => {
          const head = readLine();
          const type = head[0];
          const rest = head.slice(1);
          if (type === '$') {
            const n = parseInt(rest, 10);
            if (n === -1) return null;
            const v = buf.slice(i, i + n).toString();
            i += n + 2;
            return v;
          }
          if (type === ':') return parseInt(rest, 10);
          if (type === '+') return rest;
          if (type === '-') return 'ERR ' + rest;
          if (type === '*') {
            const n = parseInt(rest, 10);
            if (n === -1) return null;
            return Array.from({ length: n }, readVal);
          }
          return head;
        };
        resolve(cmds.map(readVal));
      } catch (e) {
        reject(e);
      }
    }, 600);
  });
}

const stockKey = `seckill:stock:${ACTIVITY_ID}`;
const setKey = `seckill:order:${ACTIVITY_ID}`;

async function snapshot(label) {
  try {
    const [stock, card, members] = await redisCmd([`GET ${stockKey}`, `SCARD ${setKey}`, `SMEMBERS ${setKey}`]);
    console.log(`[${label}] Redis  库存=${stock === null ? '(key不存在 → 未预热)' : stock}  已购集合=${card} 人`);
    return { stock: stock === null ? null : parseInt(stock, 10), card, members: members || [] };
  } catch (e) {
    console.log(`[${label}] Redis 读取失败: ${e.message}（Redis 没起？）`);
    return null;
  }
}

// ---------- 3. 发一个请求 ----------
async function fire({ userId, token }) {
  const t0 = Date.now();
  try {
    const res = await fetch(`${BASE_URL}/api/seckill/${ACTIVITY_ID}`, {
      method: 'POST',
      headers: { Authorization: token },
    });
    const text = await res.text();
    let body;
    try {
      body = JSON.parse(text);
    } catch {
      body = { code: '(非JSON)', message: text.slice(0, 120) };
    }
    return { userId, http: res.status, code: body.code, msg: body.message, ms: Date.now() - t0 };
  } catch (e) {
    return { userId, http: 'FETCH-ERR', code: 'ERR', msg: e.message, ms: Date.now() - t0 };
  }
}

// ---------- 主流程 ----------
(async () => {
  const users = loadUsers();
  const batch = users.slice(0, CONCURRENCY);
  console.log(`CSV 读到 ${users.length} 个用户，本次并发 ${batch.length} 个，活动 ${ACTIVITY_ID}\n`);

  // token 过期时间自查（免得把"token 过期"误判成业务 bug）
  // JWT 的 payload 是 base64url 编码，要把 - _ 换回 + / 才能用标准 base64 解
  const b64 = batch[0].token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
  const payload = JSON.parse(Buffer.from(b64, 'base64').toString());
  console.log(
    `首个 token: userId=${payload.userId} iat=${new Date(payload.iat * 1000).toLocaleString()} ` +
      `exp=${new Date(payload.exp * 1000).toLocaleString()}\n`
  );

  const before = await snapshot('压测前');

  console.log(`\n开始并发 ${batch.length} 个请求 ...`);
  const t0 = Date.now();
  const results = await Promise.all(batch.map(fire));
  const wall = Date.now() - t0;

  const after = await snapshot('压测后');

  // ---------- 汇总 ----------
  const tally = new Map();
  for (const r of results) {
    const k = `HTTP ${r.http} | code=${r.code} | ${r.msg}`;
    if (!tally.has(k)) tally.set(k, []);
    tally.get(k).push(r.userId);
  }

  console.log(`\n总耗时 ${wall}ms（平均 ${Math.round(wall / results.length)}ms/请求）\n`);
  console.log('================ 返回码分布 ================');
  [...tally.entries()]
    .sort((a, b) => b[1].length - a[1].length)
    .forEach(([k, ids]) => {
      console.log(`\n  ${String(ids.length).padStart(3)} 条  ${k}`);
      console.log(`          ${ids.join(', ')}`);
    });

  const ok = results.filter((r) => r.code === 200);
  console.log('\n================ 结论 ================');
  console.log(`  成功抢到:  ${ok.length} 单`);
  if (before && after && before.stock !== null && after.stock !== null) {
    const drop = before.stock - after.stock;
    console.log(`  Redis 库存: ${before.stock} → ${after.stock}  (掉了 ${drop})`);
    console.log(`  Redis 集合: ${before.card} → ${after.card}  (加了 ${after.card - before.card})`);
    if (drop === ok.length && after.card - before.card === ok.length) {
      console.log('\n  ✅ Redis 侧完全一致：进去几次 = 扣几次 = 成功几单。');
      console.log('     说明失败请求压根没走到 Lua —— 问题在请求没打进来，不在秒杀逻辑。');
    } else {
      console.log('\n  ❌ Redis 侧对不上：扣减次数 != 成功单数，Lua / 补偿逻辑有问题。');
    }
  } else {
    console.log('  （Redis 没读到，跳过一致性校验）');
  }

  console.log('\n  MySQL 侧请用这条 SQL 对账（剩余 + 有效订单 应 = 初始库存）：');
  console.log('  mysql -u root -p123456 studynote_tech -e "');
  console.log(`    SELECT (SELECT stock FROM seckill_activity WHERE activity_id=${ACTIVITY_ID}) AS 剩余库存,`);
  console.log(`           (SELECT COUNT(*) FROM seckill_order WHERE activity_id=${ACTIVITY_ID} AND status!=2) AS 有效订单;"`);
})();
