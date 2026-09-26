package com.studynote.notes.service;

import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.model.base.EmptyVO;
import com.studynote.notes.model.entity.SeckillOrder;
import com.studynote.notes.model.vo.seckill.SeckillActivityDetailVO;
import com.studynote.notes.model.vo.seckill.SeckillOrderVO;

import java.util.Date;
import java.util.List;

public interface SeckillService {

    /**
     * 用户抢购秒杀活动。
     *
     * @param activityId 活动ID
     * @return 抢购结果
     */
    ApiResponse<EmptyVO> seckill(Integer activityId);

    /**
     * 查询当前登录用户的全部秒杀订单，按下单时间倒序。
     * {@link #seckill(Integer)} 不返回 orderId（订单由 Redis Stream 消费者异步落库），
     * 调用方靠本方法反查订单，再拿 orderId 调 {@link #pay(Integer)}。
     * 用户ID 只从 token 解析，不接受前端传入，否则可查看任意用户的订单。
     *
     * @return 订单列表（含已取消的，那是用户的历史记录）；无订单时返回空列表而非 error
     */
    ApiResponse<List<SeckillOrderVO>> myOrders();

    /**
     * 查询当前可参与的秒杀活动列表（含课程信息）。
     * 过滤条件（上架、未结束）全在 SQL 里，改条件改 SQL 的 WHERE，不要用 Java 过滤，
     * 那等于把整张表拉进内存再筛。课程信息由同一条 SQL JOIN 带出（用户还没买课时
     * 查不到已购课程）。不需要登录：浏览秒杀页对未登录用户开放，需要身份的是抢购那一步。
     *
     * @return 可参与的活动列表；无可参与活动时返回空列表而非 error
     */
    ApiResponse<List<SeckillActivityDetailVO>> activityListForUser();

    /**
     * 支付秒杀订单。三件事必须原子完成：订单状态 0（待支付）改成 1（已支付）、
     * 写一条 payment 流水、给用户发放课程（插一条 user_course）。
     * 三者全是 MySQL 写，属同一原子域，所以用 {@code @Transactional(rollbackFor = Exception.class)} 覆盖；
     * cancelTimeoutOrderOne 要同时改 MySQL 和 Redis，本地事务管不到 Redis，只能拆开靠对账兜底。
     * 划事务边界的依据是"是不是同一个原子域"，不是"操作有几个"。
     * 除原有的订单存在 / 归属正确 / 状态允许外，还要查一次 user_course，已拥有该课程时直接拒绝支付。
     * 注意事务里 return 是提交不是回滚，所有 error 返回都必须排在写操作之前。
     *
     * @param orderId 订单ID
     * @return 支付结果
     */
    ApiResponse<EmptyVO> pay(Integer orderId);

    List<SeckillOrder> findTimeoutOrders(Date deadline, int limit);

    boolean cancelTimeoutOrderOne(SeckillOrder seckillOrder);
    /**
     * 把 MySQL 中活动的库存、时间窗和已下单用户装载到 Redis。
     * 已下单用户也要预热：活动开始前可能已经有人下过单，只灌库存不灌已购集合，
     * 这些人就能在 Redis 里再抢一次。
     *
     * @param activityId 活动ID
     * @return 预热结果
     */
    ApiResponse<EmptyVO> warmUp(Integer activityId);

    /**
     * 异步落库：把 Redis Stream 里的一条抢购消息写进 MySQL，调用方是 SeckillOrderStreamConsumer。
     * 必须定义在 Service 上：@Transactional 靠 Spring 动态代理生效，同类 this.xxx() 自调用不走代理，事务会失效。
     * 必须幂等：同一条消息投递两次、三次，结果都要和一次一样。
     *
     * @param userId     抢到的用户
     * @param activityId 活动ID
     * @return true = 新单插入成功；false = 库里已有这单（重复投递），本次幂等跳过
     * @throws RuntimeException 落库失败（如扣减库存扣到 0）。抛出让消费者不 ACK，消息留在 pending 等重投
     */
    boolean persistSeckillOrder(Long userId, Integer activityId);

    void syncRedisAfterCancel(Integer activityId, Long userId);

    /**
     * 找出这一轮需要参与对账的活动ID，返回的活动都不在秒杀时间窗口内（已结束 / 未开始）。
     * 不能碰进行中的活动的原因见 {@link #reconcileOne(Integer)}。
     *
     * @param now   当前时间
     * @param from  只关心 end_time 在这个时间之后的活动
     * @param limit 单次最多几个
     * @return 活动ID列表，可能为空
     */
    List<Integer> findReconcileCandidates(Date now, Date from, int limit);

    /**
     * 以 MySQL 为准重建某个活动在 Redis 里的全部状态，方向 MySQL → Redis 不可逆。
     * MySQL 是真相，Redis 只是它的派生视图；反过来用 Redis 覆盖 MySQL 等于把卖掉的库存还回去，真超卖。
     * 职责上就是"自动跑的 warmUp"（同样重建那三个 key），差别是：warmUp 由管理员手动触发、不检查活动是否进行中，
     * 本方法由定时任务触发、必须检查。不能碰进行中的活动：重建已购集合是 DEL + SADD，中间有集合为空的窗口，
     * 会造成重复下单；且此时 MySQL 库存滞后于 Redis（扣库存在异步消费里做），覆盖会凭空多出库存造成超卖。
     * 所以发现活动进行中时应当主动跳过并告警。
     *
     * @param activityId 活动ID
     * @return true = 本轮重建了 Redis 状态（含活动已删、清理残留 key）；false = 主动跳过（如活动刚好开始）
     */
    boolean reconcileOne(Integer activityId);
}
