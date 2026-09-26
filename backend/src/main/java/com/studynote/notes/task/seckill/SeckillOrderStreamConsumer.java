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
 * 秒杀订单消费者：定时从 Redis Stream 取消息异步落库。
 * 用 Stream 而不是 List 队列，是因为消息在 ACK 前会留在待确认列表中，
 * 进程崩溃后可重新投递，不会丢单。
 */
@Slf4j
@Component
public class SeckillOrderStreamConsumer {

    /** 消费者组名：同组消费者分摊读取，一条消息只投给其中一个人 */
    private static final String GROUP = "seckill-order-group";

    /** 消费者名：单机写死即可；多实例部署时需各不相同（如 IP 或主机名） */
    private static final String CONSUMER = "consumer-1";

    @Autowired
    SeckillService seckillService;

    /** 每批最多取几条 */
    private static final int BATCH_SIZE = 10;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 显式使用 StringRedisTemplate 而非 RedisTemplate&lt;String, String&gt;：
     * RedisConfig 注册了两个模板，泛型匹配不稳定，若解析到 JSON 序列化的那个，读出的键值会带上引号。
     */
    public SeckillOrderStreamConsumer(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 应用启动时创建消费者组。XREADGROUP 要求组先存在，否则报 NOGROUP；
     * createGroup 底层自带 MKSTREAM，stream 不存在时会一并创建。
     */
    @PostConstruct
    public void initGroup() {
        String streamKey = RedisKey.seckillOrderStream();

        try {
            stringRedisTemplate.opsForStream().createGroup(streamKey, GROUP);
        } catch (Exception e) {
            String msg = String.valueOf(e.getMessage());   // null 会变成字符串 "null"
            if (msg.contains("BUSYGROUP")) {
                // 组已存在属正常情况（如应用重启），吞掉即可
                log.info("消费者组已存在，跳过创建");
            } else {
                log.error("创建消费者组失败", e);            // 带上异常才能保留堆栈
            }

        }
    }

    /**
     * 定时消费，每 1 秒一轮。
     * 一轮读两次且顺序不能反：先重投本消费者名下未 ACK 的旧消息，再读新消息，
     * 否则卡住的订单一直排在队尾，永远处理不到。
     * 用 fixedDelay 轮询而非 XREAD 的 BLOCK 阻塞读，与 MessageTaskConsumer 保持一致。
     */
    @Scheduled(fixedDelay = 1000)
    public void consume() {
        String streamKey = RedisKey.seckillOrderStream();

        // 先重投本消费者名下未 ACK 的旧消息
        processRecords(streamKey,readRecords(streamKey,ReadOffset.from("0")));
        // 再读新消息
        processRecords(streamKey, readRecords(streamKey, ReadOffset.lastConsumed()));
    }

    /**
     * 读取一批消息，带 NOGROUP 自愈。
     * consume() 需要用两种偏移量各调一次，故抽成方法。
     *
     * @param offset {@code ReadOffset.lastConsumed()} 读新消息；{@code ReadOffset.from("0")} 读本消费者未 ACK 的消息
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
    }

    /** 统一处理一批消息；重投的旧消息与新消息流程一致 */
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
                // 单条失败不影响本批其余消息；不在此 ACK，未 ACK 的消息下一轮会被重投
                log.error("处理秒杀订单消息失败 id={}", record.getId(), e);
            }
        }
    }
}
