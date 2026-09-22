package com.studynote.notes.mapper;

import com.studynote.notes.model.entity.SeckillOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Mapper
public interface SeckillOrderMapper {

    /**
     * 插入一条秒杀订单。主键自增，插入后回填到 order.orderId。
     *
     * @param order 订单实体
     * @return 受影响行数
     */
    int insert(SeckillOrder order);

    /**
     * 根据用户id和活动id查询是否已存在该订单
     *
     * @param activityId,userId 订单实体
     * @return
     */
    SeckillOrder findByActivityIdAndUserId(int activityId, Long userId);

    /**
     * 根据订单id查询该订单
     *
     * @param orderId 订单实体
     * @return
     */
    SeckillOrder findByOrderId(int orderId);

    /**
     * 查询某个用户的全部秒杀订单，按下单时间倒序（最新的在前）。
     * <p>
     * 【和 findByActivityIdAndUserId 的区别】
     * 那个方法过滤了 {@code status != 2}，因为它的用途是"判断这人还能不能再抢"——
     * 已取消的订单不该挡着他重抢。本方法不做这个过滤：它是给"我的订单"列表用的，
     * 用户需要看到自己取消过的订单，那是他的历史记录，藏起来只会让他困惑
     * （"我明明抢过，怎么没了？"）。
     * <p>
     * 【为什么要有这个方法】
     * 抢购接口只返回"抢购成功"，订单是异步落库的（走 Redis Stream 消费），
     * 所以调用方当时拿不到 orderId。要支付就必须能反查出来。
     * <p>
     * 排序用 {@code created_at DESC, order_id DESC}：created_at 是业务时间，
     * order_id 作兜底 —— 同一秒下的单靠自增主键仍能排出确定顺序，
     * 否则翻页时可能出现重复或漏项。
     *
     * @param userId 用户ID（从 token 解析，绝不由前端传入）
     * @return 该用户的订单列表；无订单时返回【空集合而非 null】
     */
    List<SeckillOrder> findByUserId(@Param("userId") Long userId);


    List<Long> findActiveUserIdsByActivityId(int activityId);

    int updateStatus(int orderId);

    List<SeckillOrder> findTimeoutOrders(Date deadline, int limit);

    int cancelOrder(int orderId);

}
