package com.studynote.notes.task.seckill;

import com.studynote.notes.model.enums.redisKey.RedisKey;
import com.studynote.notes.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 秒杀订单消费者：定时从 Redis Stream 里取消息，异步落库。
 * <p>
 * 和 MessageTaskConsumer（List 版队列）的区别：
 * List 版：rightPop 一拿出来消息就没了，进程崩了 = 消息永久丢失
 * Stream 版：消息取走后仍挂在「待确认」列表里，ACK 之前崩了会被重新投递
 * <p>
 * 秒杀订单丢一条 = 一个用户抢到了却没有订单，所以必须用能重投的 Stream。
 */
@Slf4j
@Component
public class SeckillOrderStreamConsumer {

    /**
     * 消费者组名：同组的多个消费者【分工】读消息，一条只投给其中一个人
     */
    private static final String GROUP = "seckill-order-group";

    /**
     * 消费者名：单机写死即可；多实例部署时要各不相同（比如用 IP 或主机名）
     */
    private static final String CONSUMER = "consumer-1";

    @Autowired
    SeckillService seckillService;

    /**
     * 每批最多取几条
     */
    private static final int BATCH_SIZE = 10;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 注意这里【显式】用 StringRedisTemplate，不用 RedisTemplate<String, String>。
     * 原因：项目里 RedisConfig 同时注册了两个模板，泛型匹配那套很脆弱，
     * 万一解析到 JSON 序列化的那个，读出来的键值就会带上引号，白白踩坑。
     */
    public SeckillOrderStreamConsumer(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 应用启动时把消费者组建好。
     * <p>
     * 为什么必须单独建？因为 XREADGROUP 要求组【先存在】，否则报 NOGROUP 错误。
     * Spring Data Redis 的 createGroup 底层自带 MKSTREAM，所以 stream 还不存在时
     * 会顺手把它建出来，不会报「key 不存在」。
     */
    @PostConstruct
    public void initGroup() {
        String streamKey = RedisKey.seckillOrderStream();

        // 步骤 1：建组
        //   - stringRedisTemplate.opsForStream().createGroup(streamKey, GROUP)
        //   - 不需要接收返回值
        try {
            stringRedisTemplate.opsForStream().createGroup(streamKey, GROUP);
        } catch (Exception e) {
            String msg = String.valueOf(e.getMessage());   // null 会变成字符串 "null"
            if (msg.contains("BUSYGROUP")) {
                log.info("消费者组已存在，跳过创建");
            } else {
                log.error("创建消费者组失败", e);            // ← 把 e 一起传，才有堆栈
            }

        }
        // 步骤 2：捕获异常
        //   - 组已经存在时 Redis 会返回 BUSYGROUP 错误，Spring 会抛异常。
        //     这是【正常情况】（比如应用重启），必须吞掉，否则每次启动都报错。
        //   - 判断 e.getMessage() 里是否包含 "BUSYGROUP"：
        //       是   → log.info 记一句「消费者组已存在，跳过创建」就行
        //       不是 → log.error 记下来（可能是 Redis 没连上之类的真问题）
    }

    /**
     * 定时消费：每 1 秒跑一轮。
     * <p>
     * 【B5 改造后】一轮里读两次，顺序不能反：
     * 第一轮：先把自己名下【没 ACK 的旧消息】捞回来重投 —— 欠下的债先还
     * 第二轮：再读【全新的消息】
     * <p>
     * 为什么必须先旧后新？旧消息是卡住的订单。如果永远优先读新消息，
     * 债主就永远排不上队，卡住的消息会一直卡到天荒地老。
     * <p>
     * 用 fixedDelay 轮询而不用 XREAD 的 BLOCK 阻塞读，是为了跟项目里
     * MessageTaskConsumer 保持一致的做法。以后想降延迟再改成阻塞读。
     */
    @Scheduled(fixedDelay = 1000)
    public void consume() {
        String streamKey = RedisKey.seckillOrderStream();

        // 步骤 1：先重投自己名下没 ACK 的旧消息
        //   - 调 readRecords(streamKey, 偏移量)，结果交给 processRecords(streamKey, ...)
        //   - ★ 偏移量填什么？
        //       ReadOffset.lastConsumed() 是 ">"，只给【没投递过】的新消息
        //       我们要的是「我名下投递过、但还没 ACK 的」
        //       → 用 ReadOffset.from(...)，括号里填一个字符串
        //       想一下：消息 ID 是递增的，要「从最早的开始全要」，填什么？
        processRecords(streamKey,readRecords(streamKey,ReadOffset.from("0")));
        // 步骤 2：再读全新的消息（这行原来就有，已经给你了）
        processRecords(streamKey, readRecords(streamKey, ReadOffset.lastConsumed()));
    }

    /**
     * 读一批消息（附带 NOGROUP 自愈）。
     * <p>
     * 抽成方法是因为 consume() 要用两种不同的偏移量各调一次。
     *
     * @param offset 决定读什么：
     *               {@code ReadOffset.lastConsumed()}（">"）→ 没投递过的新消息
     *               {@code ReadOffset.from("0")}          → 自己名下没 ACK 的 pending
     */
    private List<MapRecord<String, String, String>> readRecords(String streamKey, ReadOffset offset) {
        List<MapRecord<String, String, String>> records;
        try{

            records = stringRedisTemplate.<String, String>opsForStream().read(
                    Consumer.from(GROUP, CONSUMER),
                    StreamReadOptions.empty().count(BATCH_SIZE),
                    StreamOffset.create(streamKey, offset));
        }catch (Exception e){
            String msg = String.valueOf(e.getMessage());
            if (msg.contains("NOGROUP")) {
                log.warn("队列不存在,重新创建");
                stringRedisTemplate.opsForStream().createGroup(streamKey,ReadOffset.latest(), GROUP);
                return null;
            }else{
                throw e;
            }
        }
        return records;
        // 步骤 1：调 read(...)，参数和原来一样，只有最后那个 StreamOffset 换成传进来的 offset
        //   - stringRedisTemplate.<String, String>opsForStream().read(
        //         Consumer.from(GROUP, CONSUMER),
        //         StreamReadOptions.empty().count(BATCH_SIZE),
        //         StreamOffset.create(streamKey, offset))
        //   - 结果赋给变量，因为 try 块外面要 return 它

        // 步骤 2：try / catch —— 把原来那段 NOGROUP 自愈搬进来
        //   - 捕获到异常后判断消息里有没有 "NOGROUP"
        //       有   → log.warn 一句，然后 createGroup(streamKey, ReadOffset.latest(), GROUP)
        //              重建完 return null（本轮跳过，下一轮就好了）
        //       没有 → throw e   （Redis 连不上之类的真故障，别吞）
        //   - 提示：createGroup 用三参数的版本，第二个参数是「建组后从哪开始读」

        // 步骤 3：return 读到的结果
        //   - 可能是 null，你不用管 —— processRecords 里会判空

    }

    /**
     * 统一处理一批消息。
     * <p>
     * 「重投的旧消息」和「新消息」处理流程完全一样，所以抽成一个方法只写一份。
     */
    private void processRecords(String streamKey, List<MapRecord<String, String, String>> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (MapRecord<String, String, String> record : records) {
            try {
                Map<String, String> body = record.getValue();
                Long userId = Long.valueOf(body.get("userId"));
                Integer activityId = Integer.valueOf(body.get("activityId"));
                boolean fresh = seckillService.persistSeckillOrder(userId, activityId);
                if (fresh) {
                    log.info("秒杀订单落库成功 id={} userId={} activityId={}", record.getId(), userId, activityId);
                } else {
                    log.info("订单已存在，幂等跳过 id={} userId={} activityId={}", record.getId(), userId, activityId);
                }
                stringRedisTemplate.opsForStream().acknowledge(streamKey, GROUP, record.getId());
            } catch (Exception e) {
                // 关键：单条失败不能影响这一批里后面的消息，
                // 而且【不要】在这里 ACK —— 不 ACK 的消息下一轮会被重投
                log.error("处理秒杀订单消息失败 id={}", record.getId(), e);
            }
        }
    }
}
