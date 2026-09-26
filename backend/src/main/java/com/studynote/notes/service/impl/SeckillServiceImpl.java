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
import com.studynote.notes.model.vo.seckill.SeckillActivityDetailVO;
import com.studynote.notes.model.vo.seckill.SeckillOrderVO;
import com.studynote.notes.scope.RequestScopeData;
import com.studynote.notes.service.SeckillService;
import com.studynote.notes.utils.ApiResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /** 秒杀 Lua 脚本，setLocation 懒加载：首次执行才读文件，之后复用 SHA1 */
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

    // 订单是异步落库的，seckill() 不返回 orderId，抢完之后要反查自己的订单才能去支付
    @Override
    @NeedLogin
    public ApiResponse<List<SeckillOrderVO>> myOrders() {

        // userId 只从 token 解析，不接受前端传参，否则可越权查看他人订单
        Long userId = requestScopeData.getUserId();
        // 返回 VO：SQL 里 JOIN 了 course，带出课程标题/封面，也不泄漏 activeFlag 等内部字段
        List<SeckillOrderVO> orders = seckillOrderMapper.findVOByUserId(userId);
        return ApiResponseUtil.success("查询成功", orders);
        // 空列表不是错误：新用户没抢过就是空的，返回 error 会让前端把正常情况当异常处理

    }

    // 用户端活动列表：可参与的过滤条件（上架 + 未结束）全在 SQL 里，Java 侧不过滤，避免全表拉进内存
    // 不加 @NeedLogin：浏览秒杀页对未登录用户也应开放，真正需要身份的是抢购那一步
    // 不能返回 null：Spring MVC 会发 HTTP 200 + 空 body，前端解析失败
    @Override
    public ApiResponse<List<SeckillActivityDetailVO>> activityListForUser() {

        return ApiResponseUtil.success("查询成功", seckillActivityMapper.findAvailableWithCourse());
    }

    // pay() 要一次完成三件事：订单状态 0 → 1、写 payment 流水、发放 user_course，
    // 三者全是 MySQL 写，属同一原子域，所以用一个本地事务覆盖
    // （cancelTimeoutOrderOne 要同时改 MySQL 和 Redis，本地事务管不到 Redis，只能拆开靠对账兜底）
    // 注意事务里 return 是提交不是回滚，所有 return error 都必须排在写操作之前
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

        // 已拥有该课程则拒绝支付（同一门课可能出现在两个活动里），必须放在写操作之前
        // 这只是体验层校验，并发下（TOCTOU）真正兜底的是 user_course 的唯一索引 uk_user_course
        if (userCourseMapper.countByUserIdAndCourseId(userId, seckillOrder.getCourseId()) > 0) {
            return ApiResponseUtil.error("你已拥有该课程，无需重复购买");
        }

        if (seckillOrder.getStatus() == 0) {
            int rows = seckillOrderMapper.updateStatus(orderId);
            if (rows <= 0) {
                return ApiResponseUtil.error("订单状态不允许支付");
            }
        }

        // 订单价和支付金额都统一用元，同类型直接赋值，不做换算
        Payment payment = new Payment();
        payment.setOrderId(seckillOrder.getOrderId());
        payment.setUserId(userId);
        payment.setAmount(seckillOrder.getPrice());
        payment.setChannel("mock");
        payment.setStatus(PaymentStatus.PAID);
        paymentMapper.insert(payment);

        // createTime 由 XML 里的 NOW() 填，user_course.create_time 是 DEFAULT NULL，SQL 里必须显式写时间
        UserCourse userCourse = new UserCourse();
        userCourse.setUserId(userId);
        userCourse.setCourseId(seckillOrder.getCourseId());
        userCourse.setSource(UserCourseSource.SECKILL);
        try {
            userCourseMapper.insert(userCourse);
        } catch (DuplicateKeyException e) {
            // 前置查询被并发绕过（TOCTOU）时由唯一索引兜住。吞掉异常可以保住已收的钱和课程，
            // 抛出去则订单回退成待支付、名额作废。MySQL 唯一键冲突不会让事务失效，
            // 且异常在 pay() 内部被接住，没穿过 @Transactional 代理，不会标 rollback-only。
            // WARN 频繁出现说明并发窗口被撞得凶，该回头看前置校验。
            log.warn("发放课程撞唯一索引，视为已拥有。orderId={} userId={} courseId={}",
                    seckillOrder.getOrderId(), userId, seckillOrder.getCourseId());
        }

        // 这行 return 在写操作之后，意味着提交；前面任何一步出错都必须靠 throw 触发回滚，
        // 而不是 return error（那只会提交半截）
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

        // 预热：把活动时间窗和库存写进 Redis，再把已下单用户（status != 2）灌进已购集合
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
     * 异步落库：调用链为 Lua XADD → SeckillOrderStreamConsumer 消费 → 调本方法 → 成功才 ACK。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean persistSeckillOrder(Long userId, Integer activityId) {

        // 查不到活动必须抛异常而不是 return false，后者会被当成"已处理过"而 ACK，用户永远拿不到订单
        SeckillActivity seckillActivity = seckillActivityMapper.findById(activityId);
        if (seckillActivity == null) {
            throw new IllegalArgumentException("活动不存在 activityId=" + activityId);
        }
        // mysql版 同步扣库存
        // 先插订单、后扣库存：uk_activity_user 唯一索引做幂等闸门，重复单在扣库存之前就抛异常，无需补偿
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
            // 只接重复键异常（重复投递，幂等跳过）；其他异常必须外抛，让消费者不 ACK
            return false;
        }
        // 扣库存失败必须抛异常触发回滚：宁可少卖，也不能出现"有订单但没扣库存"的超卖
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

    }

    // 对账：以 MySQL 为准修复 Redis 漂移

    @Override
    public List<Integer> findReconcileCandidates(Date now, Date from, int limit) {

        // 保持分层：Task 只做编排，取数归 Service；"哪些活动值得对账"是业务决策，将来改条件不动 Task
        // 时间窗参数由 Task 算好传入，与 findTimeoutOrders(deadline, limit) 风格一致

        return seckillActivityMapper.findReconcileCandidates(now, from, limit);
    }

    @Override
    public boolean reconcileOne(Integer activityId) {

        // 方向只能是 MySQL → Redis，反过来会把已经卖掉的库存还回去，造成超卖。
        // 重建的三个 key（时间窗 Hash、库存 String、已购用户 Set）与 warmUp 相同，差别是
        // 本方法由定时器触发，且必须先确认活动不在秒杀窗口内。
        // 不加 @Transactional：跨 MySQL 和 Redis，本身幂等，中途失败下一轮重跑即可。
        // 流程是 先读 Redis 旧值 → 与 MySQL 新值比对 → WARN 出差异 → 再覆盖，差异日志就是对账可运维的证据。
        // 这段重建逻辑与 warmUp 重复，可抽成一个私有方法（同类自调用不走代理，别标 @Transactional）
        String activityKey = RedisKey.seckillActivity(activityId);
        String stockKey = RedisKey.seckillStock(activityId);
        String userSetKey = RedisKey.seckillOrderUserSet(activityId);

        // 步骤 1：查 MySQL（真相）
        SeckillActivity activity = seckillActivityMapper.findById(activityId);

        // 活动已删，这三个 key 就是垃圾，清掉才算修完
        // 删整个 key 要用 delete，不要用 opsForSet().remove(key)：后者不传成员时 Lettuce 会抛异常
        if (activity == null) {
            redisTemplate.delete(Arrays.asList(activityKey, stockKey, userSetKey));
            log.warn("对账清理 activityId={} 活动已不存在，删除 Redis 残留 key", activityId);
            return true;
        }

        // 从捞出候选到走到这里可能已过了时间，活动或许刚好开始，必须在写 Redis 之前拦下：
        // 此时 MySQL 库存滞后于 Redis，强行覆盖会造成超卖（且刚 DEL 集合时有人抢会重复下单）
        long now = System.currentTimeMillis();
        long startMillis = activity.getStartTime().getTime();
        long endMillis = activity.getEndTime().getTime();
        if (now >= startMillis && now <= endMillis) {
            log.warn("对账跳过 activityId={} 活动已进入秒杀窗口 [{}, {}]，"
                            + "此时 MySQL 库存滞后于 Redis，强行覆盖会造成超卖",
                    activityId, startMillis, endMillis);
            return false;
        }

        // 步骤 2：读 Redis 旧值，只用于比对
        String oldStock = redisTemplate.opsForValue().get(stockKey);
        // size() 对不存在的 key 返回 0，但返回类型是 Long，接住 null 以免下面比较时拆箱 NPE
        Long oldSizeRaw = redisTemplate.opsForSet().size(userSetKey);
        long oldSize = oldSizeRaw == null ? 0L : oldSizeRaw;
        // entries() 对不存在的 key 返回空 Map，判"未预热过"用 isEmpty()；类型见证 <String, String> 不能省
        Map<String, String> oldWindow = redisTemplate.<String, String>opsForHash().entries(activityKey);

        // 步骤 3：从 MySQL 算新值，已购名单即 status != 2 的用户
        List<Long> userIds = seckillOrderMapper.findActiveUserIdsByActivityId(activityId);
        String newStock = String.valueOf(activity.getStock());
        String newStart = String.valueOf(startMillis);
        String newEnd = String.valueOf(endMillis);

        // 步骤 4：逐项比对并 WARN 出差异，没有这些日志就无从知道对账修过什么、修对没有
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

        // 步骤 5：覆盖写 Redis。已购集合必须 delete 再 SADD，只 add 删不掉已取消(status=2)的用户，
        // 集合会越滚越大，漂移永远修不回来
        Map<String, String> window = new HashMap<>();
        window.put("startTime", newStart);
        window.put("endTime", newEnd);
        redisTemplate.opsForHash().putAll(activityKey, window);
        redisTemplate.opsForValue().set(stockKey, newStock);
        redisTemplate.delete(userSetKey);
        // 空列表要跳过：opsForSet().add(key) 不传成员会报错
        if (!userIds.isEmpty()) {
            for (Long uid : userIds) {
                redisTemplate.opsForSet().add(userSetKey, String.valueOf(uid));
            }
        }

        return true;
    }


}
