package com.studynote.notes.service.impl;

import com.studynote.notes.annotation.NeedLogin;
import com.studynote.notes.mapper.SeckillActivityMapper;
import com.studynote.notes.mapper.SeckillOrderMapper;
import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.model.base.EmptyVO;
import com.studynote.notes.model.entity.SeckillActivity;
import com.studynote.notes.model.entity.SeckillOrder;
import com.studynote.notes.model.enums.redisKey.RedisKey;
import com.studynote.notes.scope.RequestScopeData;
import com.studynote.notes.service.SeckillService;
import com.studynote.notes.utils.ApiResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Arrays;

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
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<EmptyVO> seckill(Integer activityId) {
        Long userId = requestScopeData.getUserId();
        SeckillActivity seckillActivity = seckillActivityMapper.findById(activityId);
        if (seckillActivity == null) {
            return ApiResponseUtil.error("活动不存在");
        }
        Date now = new Date();
        if (now.before(seckillActivity.getStartTime()) || now.after(seckillActivity.getEndTime())) {
            return ApiResponseUtil.error("不在秒杀时间内");
        }
        String stockKey = RedisKey.seckillStock(activityId);
        String orderUserKey = RedisKey.seckillOrderUserSet(activityId);
        List<String> keys = Arrays.asList(stockKey, orderUserKey);
        Long result = redisTemplate.execute(SECKILL_SCRIPT, keys, String.valueOf(userId));
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
            // mysql 扣库存
            try {
                int rows = seckillActivityMapper.deduckStock(activityId);
                if (rows <= 0) {
                    throw new RuntimeException("库存扣取失败");
                }
                SeckillOrder seckillOrder = new SeckillOrder();
                seckillOrder.setActivityId(activityId);
                seckillOrder.setUserId(userId);
                seckillOrder.setCourseId(seckillActivity.getCourseId());
                seckillOrder.setPrice(seckillActivity.getSeckillPrice());
                seckillOrder.setStatus(0);
                seckillOrder.setActiveFlag(1);
                seckillOrderMapper.insert(seckillOrder);
                return ApiResponseUtil.success("抢购成功");
            } catch (Exception e) {
                redisTemplate.opsForValue().increment(stockKey);
                redisTemplate.opsForSet().remove(orderUserKey, String.valueOf(userId));
                log.error("秒杀补偿触发，已回滚 Redis;activityId={} userId={}", activityId, userId, e);

                throw e;
            }
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

    @Override
    @NeedLogin
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
        if (seckillOrder.getStatus() == 0) {
            int rows = seckillOrderMapper.updateStatus(orderId);
            if (rows <= 0) {
                return ApiResponseUtil.error("订单状态不允许支付");
            }
        }
        return ApiResponseUtil.success("支付成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTimeoutOrderOne(SeckillOrder seckillOrder) {

        int row = seckillOrderMapper.cancelOrder(seckillOrder.getOrderId());
        if (row <= 0) {
            return;
        }
        seckillActivityMapper.addStock(seckillOrder.getActivityId());
        String stockKey = RedisKey.seckillStock(seckillOrder.getActivityId());
        String orderKey = RedisKey.seckillOrderUserSet(seckillOrder.getActivityId());
        redisTemplate.opsForValue().increment(stockKey);
        redisTemplate.opsForSet().remove(orderKey, String.valueOf(seckillOrder.getUserId()));

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
}
