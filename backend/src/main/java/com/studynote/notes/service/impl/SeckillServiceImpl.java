package com.studynote.notes.service.impl;

import com.studynote.notes.annotation.NeedLogin;
import com.studynote.notes.mapper.PaymentMapper;
import com.studynote.notes.mapper.SeckillActivityMapper;
import com.studynote.notes.mapper.SeckillOrderMapper;
import com.studynote.notes.mapper.UserCourseMapper;
import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.model.base.EmptyVO;
import com.studynote.notes.model.entity.Payment;
import com.studynote.notes.model.entity.SeckillActivity;
import com.studynote.notes.model.entity.SeckillOrder;
import com.studynote.notes.model.entity.UserCourse;
import com.studynote.notes.model.enums.course.UserCourseSource;
import com.studynote.notes.model.enums.payment.PaymentStatus;
import com.studynote.notes.model.enums.redisKey.RedisKey;
import com.studynote.notes.scope.RequestScopeData;
import com.studynote.notes.service.SeckillService;
import com.studynote.notes.utils.ApiResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    private final RequestScopeData requestScopeData;

    public SeckillServiceImpl(RequestScopeData requestScopeData) {
        this.requestScopeData = requestScopeData;
    }

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    SeckillActivityMapper seckillActivityMapper;

    @Autowired
    SeckillOrderMapper seckillOrderMapper;

    @Autowired
    PaymentMapper paymentMapper;

    @Autowired
    UserCourseMapper userCourseMapper;

    /**
     * 秒杀 Lua 脚本。
     * 与 RateLimitAspect 里的内联脚本等价，只是这个较长，放文件里更好维护。
     * setLocation 是【懒加载】：第一次执行才读文件，之后复用 SHA1。
     */
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT = new DefaultRedisScript<>();

    static {
        SECKILL_SCRIPT.setLocation(new ClassPathResource("lua/seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }


    @Override
    @NeedLogin
    public ApiResponse<EmptyVO> seckill(Integer activityId) {
        Long userId = requestScopeData.getUserId();
        String activityKey = RedisKey.seckillActivity(activityId);
        Map<String, String> activity = redisTemplate.<String, String>opsForHash().entries(activityKey);
        if (activity.isEmpty()) {
            return ApiResponseUtil.error("活动未预热");
        }
        long now = System.currentTimeMillis();
        long start = Long.parseLong(activity.get("startTime"));
        long end = Long.parseLong(activity.get("endTime"));
        if (now < start || now > end) {
            return ApiResponseUtil.error("不在秒杀时间内");
        }
        String stockKey = RedisKey.seckillStock(activityId);
        String orderUserKey = RedisKey.seckillOrderUserSet(activityId);
        String streamKey = RedisKey.seckillOrderStream();
        List<String> keys = Arrays.asList(stockKey, orderUserKey, streamKey);
        Long result = redisTemplate.execute(SECKILL_SCRIPT, keys, String.valueOf(userId), String.valueOf(activityId));
        if (result == null) {
            log.error("秒杀脚本执行失败 activityId={} userId={}", activityId, userId);
            return ApiResponseUtil.error("系统繁忙");
        }

        if (result == 1) {
            return ApiResponseUtil.error("请勿重复下单");
        } else if (result == 2) {
            return ApiResponseUtil.error("库存不足");
        } else if (result == 3) {
            return ApiResponseUtil.error("活动未预热");
        } else if (result != 0) {
            return ApiResponseUtil.error("抢购失败");
        } else {
            return ApiResponseUtil.success("抢购成功");
        }

        // redis 版查询库存
        //旧版 查询库存
//        SeckillOrder order = seckillOrderMapper.findByActivityIdAndUserId(activityId, userId);
//        if (order != null) {
//            return ApiResponseUtil.error("订单已存在");
//        }
//        if (seckillActivity.getStock() <= 0) {
//            return ApiResponseUtil.error("库存不足");
//        }
//
    }

    // ═══════════════════════════════════════════════════════════════════
    //  我的秒杀订单 —— 补上那条断链
    //
    //  【为什么需要这个方法】
    //  seckill() 只回一句"抢购成功"，【不返回 orderId】——
    //  因为订单是异步落库的（Lua 扣完 Redis 就返回，真正 INSERT 由
    //  SeckillOrderStreamConsumer 在 @Scheduled 里慢慢做）。
    //  于是调用方拿到"成功"却不知道订单号，pay(orderId) 根本无从调起。
    //  这个方法就是那条缺掉的查询：抢完之后反查自己的订单，拿 orderId 去支付。
    //
    //  （另一个选项是让 seckill() 直接返回 orderId —— 做不到，
    //    orderId 是 MySQL 自增主键，此刻还没生成。要么改成雪花ID预生成，
    //    要么就接受"异步落库 + 反查"这套，本项目选后者。）
    // ═══════════════════════════════════════════════════════════════════
    @Override
    @NeedLogin
    public ApiResponse<List<SeckillOrder>> myOrders() {

        // 【第1步】拿当前用户
        //   Long userId = requestScopeData.getUserId();
        //   ★ 老规矩：用户ID 从 token 解析出来的 RequestScopeData 里取，
        //     【绝不】让前端当参数传。传了就是"我能看任何人的订单"的越权漏洞（IDOR）。
        //     @NeedLogin 切面已保证走到这里必定已登录，所以不会是 null。
        Long userId = requestScopeData.getUserId();
        List<SeckillOrder> orders = seckillOrderMapper.findByUserId(userId);
        return ApiResponseUtil.success("查询成功", orders);
        // 【第2步】查库
        //   List<SeckillOrder> orders = seckillOrderMapper.findByUserId(userId);
        //   无结果时 MyBatis 返回【空集合，不是 null】（和 HashOperations.entries 一致），
        //   所以这里不需要判 null。

        // 【第3步】返回
        //   return ApiResponseUtil.success("查询成功", orders);
        //   ★ 必须用【两参】重载 success(message, data)。
        //     ApiResponseUtil 上【没有】"只收 data"的 success(T)，
        //     只有 success(String) 和 success(String, T)。
        //     也别顺手去加那个重载 —— 它和 success(String) 在传字符串时重载歧义。
        //
        //   ★ 空列表【不是错误】：新用户没抢过就是空的，返回 error 会让前端
        //     把正常情况当异常处理。

        // 【第4步】思考：这里要加 @Transactional 吗？
        //   不用。它只读不写，开事务纯属白费。
        //   （真要标也是 @Transactional(readOnly = true)，语义上告诉数据库"只读"，
        //    某些驱动会据此走从库；但本项目单库，没必要。）

    }

    // ═══════════════════════════════════════════════════════════════════
    //  已购课程（user_course）③ —— 支付即发放
    //
    //  pay() 现在要一次做完三件事，且必须同生共死：
    //    ① seckill_order.status  0 → 1
    //    ② payment 插一条流水
    //    ③ user_course 插一条「已购」
    //
    //  【加 @Transactional(rollbackFor = Exception.class)】
    //  三件事全是 MySQL 写，属于同一个原子域，一个本地事务天然覆盖。
    //
    //  对照复习：cancelTimeoutOrderOne 要改 MySQL + Redis 两个系统，
    //  本地事务管不到 Redis，所以只能拆开、靠对账任务兜底。
    //  ★ 划事务边界的依据是「是不是同一个原子域」，不是「操作有几个」。
    //
    //  ★★ 事务里最容易踩的坑：return 是【提交】不是回滚。
    //  所以所有的 return ApiResponseUtil.error(...) 都必须排在【写操作之前】，
    //  否则前面写进去的东西会被老老实实提交掉，事务保护等于没有。
    //  下面这个结构就是照这条规矩排的：先全是只读校验 → 再开始写。
    // ═══════════════════════════════════════════════════════════════════
    @Override
    @NeedLogin
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<EmptyVO> pay(Integer orderId) {
        Long userId = requestScopeData.getUserId();
        //1.判断当前用户 和当前订单是否一致
        //2.判断订单是否存在
        //3.判断订单状态
        SeckillOrder seckillOrder = seckillOrderMapper.findByOrderId(orderId);
        if (seckillOrder == null) {
            return ApiResponseUtil.error("订单不存在");
        }
        if (!seckillOrder.getUserId().equals(userId)) {
            return ApiResponseUtil.error("订单不一致");
        }
        if (seckillOrder.getStatus() == 1) {
            return ApiResponseUtil.error("订单已支付");
        }
        if (seckillOrder.getStatus() == 2) {
            return ApiResponseUtil.error("超时订单，支付失败");
        }

        // ── 前置校验：用户是不是已经拥有这门课了 ────────────────────
        // 场景：两个不同的活动卖同一门课。用户先从 A 买到了，
        // 现在又抢到了 B 的资格 —— 不该让他再付一次钱。
        //
        // ★ 位置必须在这里：【所有写操作之前】。
        //   因为事务里的 return 是提交不是回滚（见方法头注释）。
        //
        // ★ 这道校验是【用户体验】，不是【正确性保证】。
        //   并发下两个请求可能都查到 0（TOCTOU），所以真正兜底的是
        //   user_course 上那条唯一索引 uk_user_course ——
        //   它的体现就是下面插 user_course 时那个 catch。
        //   两者各司其职，别以为有了这个查询就可以不管唯一索引。
        if (userCourseMapper.countByUserIdAndCourseId(userId, seckillOrder.getCourseId()) > 0) {
            return ApiResponseUtil.error("你已拥有该课程，无需重复购买");
        }

        if (seckillOrder.getStatus() == 0) {
            int rows = seckillOrderMapper.updateStatus(orderId);
            if (rows <= 0) {
                return ApiResponseUtil.error("订单状态不允许支付");
            }
        }

        // ── ② 写支付流水 ─────────────────────────────────────────────
        // ★ 全项目已统一成"元"，所以这里【没有换算】——
        //   订单价当初就是照着活动价存进来的（见 persistSeckillOrder 的 setPrice），
        //   两边同类型同单位，直接赋值即可。
        //
        //   【这条边界的意义】换算只允许发生在"系统边界"：
        //   钱从外部进来（第三方支付回调给的是分）时转一次，
        //   出去（对接支付网关要求传分）时转一次。
        //   内部所有表、所有实体、所有 VO 一律用元，一条链上不再出现第二次换算。
        //   历史坑：以前这里是 BigDecimal.valueOf(getPrice(), 2)，
        //   而 getPrice() 若是 long 且已被 /100 过，就是"换算两次" —— 金额静默缩小 100 倍。
        //   同类型直接赋值，编译器就替你挡住了这类错误。
        Payment payment = new Payment();
        payment.setOrderId(seckillOrder.getOrderId());
        payment.setUserId(userId);
        payment.setAmount(seckillOrder.getPrice());
        payment.setChannel("mock");
        payment.setStatus(PaymentStatus.PAID);
        paymentMapper.insert(payment);

        // ── ③ 发放课程 ───────────────────────────────────────────────
        // createTime 由 XML 里的 NOW() 填 —— 注意 user_course.create_time 是
        // DEFAULT NULL，没有默认值，所以 SQL 里必须显式写时间。
        UserCourse userCourse = new UserCourse();
        userCourse.setUserId(userId);
        userCourse.setCourseId(seckillOrder.getCourseId());
        userCourse.setSource(UserCourseSource.SECKILL);
        try {
            userCourseMapper.insert(userCourse);
        } catch (DuplicateKeyException e) {
            // 上面那道前置查询被并发绕过了（TOCTOU），唯一索引 uk_user_course 兜住了。
            // 权衡：用户确实该拥有这门课，钱也已经收了。
            // 抛出去 → 整个事务回滚 → 订单退回"待支付"、流水也没了 →
            //          用户拿着一个抢到的名额却再也付不了款，不可挽回。
            // 吞掉   → 他拿到了课，钱也记了账，唯一的问题是"多收了一笔他本来
            //          不用付的钱"（如果他早就拥有这门课）。
            // 两害相权取轻，这里选择吞掉，但一定要留痕 ——
            // 这条 WARN 频繁出现就说明并发窗口被撞得很凶，值得回头看前置校验。
            //
            // ★ 为什么在这里 catch 不会毒化事务：
            //   MySQL 的唯一键冲突【不会】让整个事务失效（这点和 PostgreSQL 不同），
            //   后面的语句照样能执行；
            //   而且这个异常是在 pay() 内部被接住的，没有穿过任何 @Transactional
            //   代理边界，Spring 不会把它标成 rollback-only。
            //   如果 userCourseMapper.insert 是「另一个 Bean 的 @Transactional 方法」，
            //   那这个 catch 就会引发 UnexpectedRollbackException —— 差别在这里。
            log.warn("发放课程撞唯一索引，视为已拥有。orderId={} userId={} courseId={}",
                    seckillOrder.getOrderId(), userId, seckillOrder.getCourseId());
        }

        // ── 三件事都做完，才 return 成功 ─────────────────────────────
        // ★ 注意这行 return 在写操作【之后】—— 它意味着"提交"。
        //   所以前面任何一步出问题都必须靠 throw（触发回滚），
        //   而不是靠 return error（那只会提交半截）。
        //   反过来说：如果将来有人在这行之后又加了一句写操作 + return error，
        //   事务就破了 —— 加代码时盯着这个位置。
        return ApiResponseUtil.success("支付成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTimeoutOrderOne(SeckillOrder seckillOrder) {

        int row = seckillOrderMapper.cancelOrder(seckillOrder.getOrderId());
        if (row <= 0) {
            return false;
        }
        seckillActivityMapper.addStock(seckillOrder.getActivityId());
        return true;

    }

    @Override
    public void syncRedisAfterCancel(Integer activityId, Long userId) {
        String stockKey = RedisKey.seckillStock(activityId);
        String orderKey = RedisKey.seckillOrderUserSet(activityId);
        redisTemplate.opsForValue().increment(stockKey);
        redisTemplate.opsForSet().remove(orderKey, String.valueOf(userId));

    }
    @Override
    public ApiResponse<EmptyVO> warmUp(Integer activityId) {

        // 步骤 1：查活动
        //   - 用 seckillActivityMapper.findById(activityId)
        //   - 为 null → return ApiResponseUtil.error("活动不存在")

        // 步骤 2：把库存写进 Redis
        //   - key  = RedisKey.seckillStock(activityId)
        //   - 值   = 活动的 stock，注意要转成字符串
        //   - 用 stringRedisTemplate.opsForValue().set(key, value)
        //   - 思考：要不要 set 过期时间？
        //       提示：如果活动结束后 key 永远留着，会慢慢吃满内存。
        //       可以考虑 "活动结束时间 - 现在" 作为 TTL（秒）。
        //       但注意 TTL 太短会导致活动还没结束 key 就没了 → Redis 里查不到库存
        //       想不清楚就先不设，我们 ⑤ 再回来处理。

        // 步骤 3：把"已经下过单的用户"灌进 Set
        //   - 先查：这个活动下所有 status != 2 的订单的 userId 列表
        //       → 需要在 SeckillOrderMapper 新增一个方法（下面 6.4 说）
        //   - key = RedisKey.seckillOrderUserSet(activityId)
        //   - 遍历 userId，stringRedisTemplate.opsForSet().add(key, userId.toString())
        //   - 思考：如果列表为空，要不要提前 return？
        //       提示：Spring Data Redis 的 add(key) 不传成员会报错/无意义，
        //             做个空判断更稳。

        // 步骤 4：返回成功，消息里带上关键数字方便你验证
        //   - 比如 "预热成功，库存=100，已购用户=3"
        //   - 注意！ApiResponseUtil.success(String message, T data) 有 bug，
        //     它会丢掉 message。所以要么用 success(data)，
        //     要么就把信息拼进别的地方。这个我们之前发现过。
        SeckillActivity seckillActivity = seckillActivityMapper.findById(activityId);
        if (seckillActivity == null) {
            return ApiResponseUtil.error("活动不存在");
        }
        String activityKey = RedisKey.seckillActivity(activityId);
        HashMap<String, String> map = new HashMap<>();
        map.put("startTime", String.valueOf(seckillActivity.getStartTime().getTime()));
        map.put("endTime", String.valueOf(seckillActivity.getEndTime().getTime()));
        redisTemplate.opsForHash().putAll(activityKey, map);
        String key = RedisKey.seckillStock(activityId);
        redisTemplate.opsForValue().set(key, String.valueOf(seckillActivity.getStock()));
        List<Long> user_list = seckillOrderMapper.findActiveUserIdsByActivityId(activityId);
        String user_key = RedisKey.seckillOrderUserSet(activityId);
        redisTemplate.delete(user_key);
        if (!user_list.isEmpty()) {
            for (Long userId : user_list) {
                redisTemplate.opsForSet().add(user_key, String.valueOf(userId));
            }
        }
        String message = "预热成功，库存=" + seckillActivity.getStock() + "已购用户=" + user_list.size();
        return ApiResponseUtil.success(message);
    }


    @Override
    public List<SeckillOrder> findTimeoutOrders(Date deadline, int limit) {
        return seckillOrderMapper.findTimeoutOrders(deadline, limit);
    }

    /**
     * 【异步落库】B3-b
     * <p>
     * 调用链：Lua 里 XADD 塞消息 → SeckillOrderStreamConsumer 读到 → 调这个方法 → 成功才 ACK。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean persistSeckillOrder(Long userId, Integer activityId) {

        // 步骤 1：查活动，拿到 courseId 和 seckillPrice
        //   - SeckillActivity activity = seckillActivityMapper.findById(activityId);
        //   - activity == null → throw new IllegalStateException("活动不存在 activityId=" + activityId);
        //
        SeckillActivity seckillActivity = seckillActivityMapper.findById(activityId);
        if (seckillActivity == null) {
            throw new IllegalArgumentException("活动不存在 activityId=" + activityId);
        }
        // mysql版 同步扣库存
        try {
            SeckillOrder seckillOrder = new SeckillOrder();
            seckillOrder.setActivityId(activityId);
            seckillOrder.setUserId(userId);
            seckillOrder.setCourseId(seckillActivity.getCourseId());
            seckillOrder.setPrice(seckillActivity.getSeckillPrice());
            seckillOrder.setStatus(0);
            seckillOrder.setActiveFlag(1);
            seckillOrderMapper.insert(seckillOrder);
        } catch (DuplicateKeyException e) {
            return false;
        }
        int rows = seckillActivityMapper.deductStock(activityId);
        if (rows <= 0) {
            throw new RuntimeException("库存扣取失败");
        }
        return true;
//        {
//            redisTemplate.opsForValue().increment(stockKey);
//            redisTemplate.opsForSet().remove(orderUserKey, String.valueOf(userId));
//            log.error("秒杀补偿触发，已回滚 Redis;activityId={} userId={}", activityId, userId, e);
//            throw e;
//        }
        //   ★ 想一想：为什么"查不到活动"不能 return false 当作幂等成功？
        //     因为 return false 的含义是"这单已经处理过了"，而实际情况是"处理不了"。
        //     混为一谈的后果：消息被 ACK 掉，用户永远拿不到订单，而且【不留任何痕迹】。

        // 步骤 2：先 INSERT 订单 —— 拿唯一索引当幂等闸门
        //   - new SeckillOrder()，填 6 个字段：
        //       setActivityId(activityId)
        //       setUserId(userId)
        //       setCourseId(activity.getCourseId())
        //       setPrice(activity.getSeckillPrice())
        //       setStatus(0)                                    // 0 = 待支付
        //       setActiveFlag(1)                                // 1 = 有效（cancelOrder 会置回 NULL）
        //   - seckillOrderMapper.insert(order)
        //
        //   ★★★ 为什么必须【先插订单、后扣库存】？★★★
        //
        //   seckill_order 上有个唯一索引：uk_activity_user (activity_id, user_id, active_flag)
        //   同一用户对同一活动再插一次 → MySQL 报 1062 重复键 →
        //   Spring 把它翻译成 org.springframework.dao.DuplicateKeyException
        //
        //   也就是说"这单是不是已经落过库了"，是【数据库原子地】替我们判断的。
        //   比"先 SELECT 查一下、没有才 INSERT"可靠得多 —— 后者是典型的 TOCTOU 竞态。
        //
        //   那反过来，先扣库存再插订单行不行？
        //   不行。你扣完库存才发现是重复单 → 库存已经白扣了 → 又得写补偿还回去。
        //   先插：重复的话在扣库存【之前】就抛异常了，什么都用不着补。

        // 步骤 3：只接住 DuplicateKeyException
        //   - catch (DuplicateKeyException e) { return false; }
        //   - 绝对不要 catch (Exception e) 一把抓！
        //     数据库断连、字段超长、死锁这些是真故障，必须往外抛让消费者别 ACK。
        //   - 补充知识：MySQL 的唯一键冲突【不会】让整个事务失效，后面的语句照样能跑。
        //     我们这里直接 return false，事务正常提交（其实什么都没改动）。

        // 步骤 4：扣减 MySQL 库存
        //   - int rows = seckillActivityMapper.deductStock(activityId);
        //   - rows <= 0 → throw new RuntimeException("扣减库存失败 activityId=" + activityId);
        //
        //   ★ 为什么这里必须 throw，而不是 return false？
        //   因为方法上有 @Transactional(rollbackFor = Exception.class)：
        //       抛异常 → 步骤 2 插的订单被【回滚】→ 事务里干干净净
        //       异常继续冒泡到消费者 → 消费者不 ACK → 消息留在 pending 等重投
        //   这正是我们要的：宁可"没订单"（少卖），也绝不能"有订单但没扣库存"（超卖）。
        //
        //   反过来想：如果这里 return false 会怎样？
        //   → 订单提交了、库存没扣、消费者还 ACK 了、消息永久消失。
        //   → 库存虚高，后面继续卖 → 超卖。这是最危险的方向。

        // 步骤 5：return true

    }

    // ═══════════════════════════════════════════════════════════════════
    //  【对账】② 以 MySQL 为准，修复 Redis 漂移
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public List<Integer> findReconcileCandidates(Date now, Date from, int limit) {

        // 这一层现在很薄，就一行转发，但它不该被"优化"掉。
        // 为什么不让 SeckillReconcileTask 直接调 Mapper？
        //   ① 保持现有分层：Task 只做编排（算时间、开循环、记日志），取数逻辑归 Service；
        //   ② "哪些活动值得对账"是【业务决策】，不是 SQL 细节。
        //      将来要加条件（比如只对账 status=1 的、只对账有订单的），改这里，Task 不动。
        //
        // 至于 now / from / limit 为什么由 Task 算好再传进来 ——
        // 跟 findTimeoutOrders(deadline, limit) 保持同一种风格：
        // 时间窗的配置项（retainHours）住在 Task 那边，Service 只认参数。

        return seckillActivityMapper.findReconcileCandidates(now, from, limit);
    }

    @Override
    public boolean reconcileOne(Integer activityId) {

        // ───────────────────────────────────────────────────────────────
        //  方向：MySQL → Redis。永远是这个方向，反过来是灾难。
        //
        //  活动在 Redis 里一共三个 key，全部由 MySQL 派生：
        //    seckill:activity:{id}   Hash    ← seckill_activity.start_time / end_time
        //    seckill:stock:{id}      String  ← seckill_activity.stock
        //    seckill:order:{id}      Set     ← seckill_order 里 status != 2 的 user_id
        //
        //  你会发现：这三步和 warmUp 做的是一模一样的事。
        //  对账本质上就是"自动跑的 warmUp"，差别只有两点：
        //    ① warmUp 是人点的（管理员），对账是定时器点的；
        //    ② warmUp 不检查活动是否正在进行中，对账【必须】检查。
        // ───────────────────────────────────────────────────────────────

        // 【欠一次重构，先记账】
        // 与其把 warmUp 那三步抄第二遍，更干净的做法是抽一个私有方法：
        //     private SeckillActivity loadActivityStateToRedis(Integer activityId)
        // 让 warmUp 和 reconcileOne 都调它（返回实体是为了让调用方自己拼消息/比自己想要的字段）。
        // 顺手复习上次那个坑：这个私有方法【不能】标 @Transactional ——
        // 同类自调用不走 Spring 代理，标了也白标。它本来也不需要事务（只写 Redis，不写 MySQL）。
        //
        // 如果你觉得现在动 warmUp 风险大，就地重写三遍也可以，
        // 但必须在注释里写明"这段与 warmUp 重复，欠一次重构"，别装作没看见。

        // ───────────────────────────────────────────────────────────────
        //  ★ 本方法真正的灵魂：不是"修"，而是"知道修了什么"。
        //  一个默默跑、事后无人查看的对账任务，等于没有 ——
        //  下次真出问题时你依然两眼一抹黑。
        //  所以流程必须是：
        //      先读 Redis 旧值 → 和 MySQL 新值比对 → 不一致就 WARN 出差异 → 再覆盖
        //  这样日志里才会出现诸如
        //      "对账修复 activityId=1 库存 Redis=20 → MySQL=1"
        //  这句话就是整套系统"可运维"的证据，也是面试能讲的东西。
        // ───────────────────────────────────────────────────────────────

        // 本方法【不加】@Transactional：它读 MySQL、写 Redis，但两者之间没有
        // "要么都成功要么都失败"的诉求 —— 它本身就是用来擦屁股的，
        // 中途挂了没关系，下一轮重跑一遍就好（幂等）。加了事务反而误导。
        String activityKey = RedisKey.seckillActivity(activityId);
        String stockKey = RedisKey.seckillStock(activityId);
        String userSetKey = RedisKey.seckillOrderUserSet(activityId);

        // ── 步骤 1：拿真相（MySQL） ─────────────────────────────────
        SeckillActivity activity = seckillActivityMapper.findById(activityId);

        // 活动被删了 → Redis 里这三个 key 就是垃圾，清掉才算修完。
        // ★ 用 redisTemplate.delete(Collection) 一次删三个。
        //   不要用 opsForSet().remove(key) 来删整个 Set ——
        //   那个方法的签名是 remove(K key, Object... values)，不传成员时
        //   Lettuce 会直接抛 IllegalArgumentException（Redis 的 SREM 不接受零个成员）。
        //   想删整个 key，就用 delete，语义清楚也不挑数据类型。
        if (activity == null) {
            redisTemplate.delete(Arrays.asList(activityKey, stockKey, userSetKey));
            log.warn("对账清理 activityId={} 活动已不存在，删除 Redis 残留 key", activityId);
            return true;
        }

        // ── 步骤 6 提前到这里：所有 Redis 写入之前，先做"活动是否已开始"的防御 ──
        // 从"捞出候选活动"到"走到这里"之间过了时间，活动可能【刚好开始】。
        // 必须在动 Redis 之前拦下来 —— 宁可不修，也绝不能在有人抢的时候 DEL 集合。
        // 这段判断本身必须放在 activity == null 之后（那时候没有时间窗可比）。
        long now = System.currentTimeMillis();
        long startMillis = activity.getStartTime().getTime();
        long endMillis = activity.getEndTime().getTime();
        if (now >= startMillis && now <= endMillis) {
            log.warn("对账跳过 activityId={} 活动已进入秒杀窗口 [{}, {}]，"
                            + "此时 MySQL 库存滞后于 Redis，强行覆盖会造成超卖",
                    activityId, startMillis, endMillis);
            return false;
        }

        // ── 步骤 2：读 Redis 旧值（只为比对，不参与任何决策） ────────
        String oldStock = redisTemplate.opsForValue().get(stockKey);
        // size() 对不存在的 key 返回 0，但返回类型是 Long，保险起见接住 null，
        // 否则下面的 != 比较会拆箱 NPE。
        Long oldSizeRaw = redisTemplate.opsForSet().size(userSetKey);
        long oldSize = oldSizeRaw == null ? 0L : oldSizeRaw;
        // entries() 对不存在的 key 返回【空 Map，不是 null】—— 所以判"没预热过"用 isEmpty()。
        // ★ 类型见证 <String, String> 不能省，否则退化成 Map<Object,Object> 编译不过。
        Map<String, String> oldWindow = redisTemplate.<String, String>opsForHash().entries(activityKey);

        // ── 步骤 3：从 MySQL 算新值 ────────────────────────────────
        // 复用 warmUp 已经在调的那个方法，它就是"以 MySQL 为准"的已购名单（status != 2）。
        List<Long> userIds = seckillOrderMapper.findActiveUserIdsByActivityId(activityId);
        String newStock = String.valueOf(activity.getStock());
        String newStart = String.valueOf(startMillis);
        String newEnd = String.valueOf(endMillis);

        // ── 步骤 4：比对 + WARN 出每一处差异（本方法的灵魂，不能省） ──
        // 没有这几行日志，这个任务就只是"定时偷偷刷一遍 Redis"，
        // 出了事你依然不知道它修过什么、修对没有。
        if (oldStock == null) {
            log.warn("对账修复 activityId={} 库存 Redis=未预热/已过期 → MySQL={}", activityId, newStock);
        } else if (!oldStock.equals(newStock)) {
            log.warn("对账修复 activityId={} 库存 Redis={} → MySQL={}", activityId, oldStock, newStock);
        }

        if (oldSize != userIds.size()) {
            log.warn("对账修复 activityId={} 已购用户数 Redis={} → MySQL={}",
                    activityId, oldSize, userIds.size());
        }

        if (oldWindow.isEmpty()) {
            log.warn("对账修复 activityId={} 时间窗 Redis=未预热 → MySQL=[{}, {}]",
                    activityId, newStart, newEnd);
        } else if (!newStart.equals(oldWindow.get("startTime"))
                || !newEnd.equals(oldWindow.get("endTime"))) {
            log.warn("对账修复 activityId={} 时间窗 Redis=[{}, {}] → MySQL=[{}, {}]",
                    activityId, oldWindow.get("startTime"), oldWindow.get("endTime"), newStart, newEnd);
        }

        // ── 步骤 5：覆盖写 Redis（顺序无所谓，但集合必须先删再建） ──
        // ★ 已购集合必须 delete 再 SADD，不能只 add。
        //   对账的语义是"让 Redis 等于 MySQL"：MySQL 里已被取消(status=2)的用户，
        //   光 SADD 删不掉他，集合只会越滚越大，漂移永远修不回来。
        Map<String, String> window = new HashMap<>();
        window.put("startTime", newStart);
        window.put("endTime", newEnd);
        redisTemplate.opsForHash().putAll(activityKey, window);
        redisTemplate.opsForValue().set(stockKey, newStock);
        redisTemplate.delete(userSetKey);
        // 空列表要跳过：opsForSet().add(key) 不传成员同样会报错（同步骤 1 那个坑）。
        if (!userIds.isEmpty()) {
            for (Long uid : userIds) {
                redisTemplate.opsForSet().add(userSetKey, String.valueOf(uid));
            }
        }

        return true;
    }


}
