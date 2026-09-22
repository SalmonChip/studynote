package com.studynote.notes.service;

import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.model.base.EmptyVO;
import com.studynote.notes.model.entity.SeckillOrder;

import java.util.Date;
import java.util.List;

public interface SeckillService {

    /**
     * 用户抢购秒杀活动（②b 朴素版，故意不防并发，会超卖）。
     *
     * @param activityId 活动ID
     * @return 抢购结果
     */
    ApiResponse<EmptyVO> seckill(Integer activityId);

    /**
     * 查询当前登录用户的全部秒杀订单，按下单时间倒序。
     * <p>
     * <b>为什么需要它：</b>{@link #seckill(Integer)} 只返回"抢购成功"，不返回 orderId ——
     * 订单是在 Redis Stream 消费者里异步落库的，Lua 脚本扣完库存就返回了，
     * 此刻 MySQL 里的自增主键还没生成。所以调用方必须能反查自己的订单，
     * 否则拿到"成功"也不知道该拿哪个 orderId 去调 {@link #pay(Integer)}。
     * <p>
     * <b>身份来源：</b>用户ID 只从 token 解析（{@code RequestScopeData}），
     * <b>不接受前端传入的 userId</b> —— 收了就是可以查看任意用户订单的越权漏洞。
     *
     * @return 订单列表（含已取消的，那是用户的历史记录）；无订单时返回空列表而非 error
     */
    ApiResponse<List<SeckillOrder>> myOrders();

    /**
     * 支付秒杀订单。
     * <p>
     * <b>这个方法必须是一个原子操作</b>，一次要完成三件事：
     * <ol>
     *   <li>把订单状态从 0（待支付）改成 1（已支付）；</li>
     *   <li>写一条 payment 支付流水；</li>
     *   <li>给用户发放课程（插一条 user_course）。</li>
     * </ol>
     * 三件事缺一不可，且必须同生共死 —— 所以整个方法要挂
     * {@code @Transactional(rollbackFor = Exception.class)}。
     * <p>
     * <b>为什么这里能用一个本地事务，而 {@code cancelTimeoutOrderOne} 不能？</b>
     * 因为这三件事<b>全是 MySQL</b>，属于同一个原子域，一个事务天然覆盖。
     * 而那个方法要同时改 MySQL 和 Redis —— 两个系统，本地事务管不到 Redis，
     * 所以只能拆开、靠对账任务兜底。
     * <p>
     * <b>划事务边界的依据是「是不是同一个原子域」，不是「操作有几个」。</b>
     * <p>
     * <b>前置校验（顺序在写操作之前）：</b>
     * 除了原有的"订单存在 / 归属正确 / 状态允许"之外，还要查一次
     * {@code user_course} —— 用户已经拥有这门课时直接拒绝支付。
     * <p>
     * ★ 为什么这道查询要放在【写操作之前】：{@code @Transactional} 方法里
     * {@code return} 是<b>提交</b>不是回滚。一旦在写操作之后 return，
     * 前面已经写进去的东西会被老老实实提交掉，事务保护等于没有。
     * 所以所有 {@code return ApiResponseUtil.error(...)} 都必须排在写操作前面。
     *
     * @param orderId 订单ID
     * @return 支付结果
     */
    ApiResponse<EmptyVO> pay(Integer orderId);

    List<SeckillOrder> findTimeoutOrders(Date deadline, int limit);

    boolean cancelTimeoutOrderOne(SeckillOrder seckillOrder);
    /**
     * 【预热】把 MySQL 中某个活动的库存 + 已下单用户，装载到 Redis。
     *
     * 为什么要"已下单用户"也要预热？
     *   活动开始前可能已经有人下过单（比如活动提前建好、用户提前能点）。
     *   如果只灌库存不灌已购集合，这些人就能在 Redis 里再抢一次。
     *
     * @param activityId 活动ID
     * @return 预热结果
     */
    ApiResponse<EmptyVO> warmUp(Integer activityId);

    /**
     * 【异步落库】把 Redis Stream 里的一条抢购消息，真正写进 MySQL。
     * <p>
     * 调用方是 SeckillOrderStreamConsumer（在 @Scheduled 的消费循环里）。
     * <p>
     * 为什么必须定义在 Service 上，而不是写成消费者自己的私有方法？
     * 因为 {@code @Transactional} 靠 Spring 动态代理生效，只有"从外面调进来"才会被包事务。
     * 同类里 {@code this.xxx()} 自调用走的是原始对象，代理插不进去 → 事务失效。
     * <p>
     * 这个方法必须做到【幂等】：同一条消息被投递两次、三次，结果都要和一次一样。
     *
     * @param userId     抢到的用户
     * @param activityId 活动ID
     * @return {@code true}  = 这是新单，真的插进库了
     *         {@code false} = 库里已经有这单了（重复投递），本次幂等跳过
     * @throws RuntimeException 落库失败（比如扣减库存扣到 0）。抛出去让消费者【不 ACK】，
     *                          消息留在 pending 里等重投，绝不能让异常被静默吞掉
     */
    boolean persistSeckillOrder(Long userId, Integer activityId);

    void syncRedisAfterCancel(Integer activityId, Long userId);

    /**
     * 【对账】找出这一轮需要参与对账的活动ID。
     * <p>
     * 返回的活动必须都【不在】秒杀时间窗口内（已结束 / 未开始）。
     * 为什么不能碰进行中的活动，见 {@link #reconcileOne(Integer)}。
     *
     * @param now   当前时间
     * @param from  只关心 end_time 在这个时间之后的活动
     * @param limit 单次最多几个
     * @return 活动ID列表，可能为空
     */
    List<Integer> findReconcileCandidates(Date now, Date from, int limit);

    /**
     * 【对账】以 MySQL 为准，重建某个活动在 Redis 里的全部状态。
     * <p>
     * <b>方向：MySQL → Redis，不可逆。</b>MySQL 是真相（source of truth），
     * Redis 只是它的派生视图。反过来拿 Redis 的值去覆盖 MySQL，
     * 等于把已经卖掉的库存还回去 —— 真超卖。
     * <p>
     * 这个方法职责上就是"自动跑的 warmUp"（同样重建那三个 key），差别有两点：
     * <ol>
     *   <li>warmUp 由管理员手动触发，本方法由定时任务触发；</li>
     *   <li><b>warmUp 不检查活动是否正在进行中，本方法必须检查。</b></li>
     * </ol>
     * <b>为什么本方法绝不能碰"正在进行中"的活动：</b>
     * <ol>
     *   <li>重建已购集合是 {@code DEL} + 重新 {@code SADD}，中间有一个"集合为空"的窗口。
     *       此刻若真实流量进来，已抢过的用户 {@code SISMEMBER} 得到 0，能再抢一次 → 重复下单。</li>
     *   <li>更致命：进行中的活动，MySQL 的 stock <b>是滞后的</b> ——
     *       扣库存在异步 Stream 消费里做（见 {@link #persistSeckillOrder}），
     *       此刻"以 MySQL 为准"会把 Redis 覆盖成<b>过时</b>的值 → 凭空多出库存 → 真超卖。</li>
     * </ol>
     * 所以本方法发现活动正在进行中时，应当<b>主动跳过并告警</b>，宁可不修也不能修错。
     *
     * @param activityId 活动ID
     * @return {@code true}  = 这一轮确实重建了 Redis 状态（含"活动已被删、清理了残留 key"）
     *         {@code false} = 主动跳过（比如活动刚好开始了）
     */
    boolean reconcileOne(Integer activityId);
}
