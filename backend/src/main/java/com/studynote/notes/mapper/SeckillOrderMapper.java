package com.studynote.notes.mapper;

import com.studynote.notes.model.entity.SeckillOrder;
import org.apache.ibatis.annotations.Mapper;

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


    List<Long> findActiveUserIdsByActivityId(int activityId);

    int updateStatus(int orderId);

    List<SeckillOrder> findTimeoutOrders(Date deadline, int limit);

    int cancelOrder(int orderId);

}
