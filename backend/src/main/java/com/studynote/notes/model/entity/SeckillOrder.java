package com.studynote.notes.model.entity;

import lombok.Data;

import java.util.Date;

/**
 * 秒杀订单实体类。
 */
@Data
public class SeckillOrder {

    /** 订单ID（主键，自增） */
    private Integer orderId;

    /** 活动ID */
    private Integer activityId;

    /** 用户ID */
    private Long userId;

    /** 课程ID（下单时从活动里带出） */
    private Integer courseId;

    /** 成交价（分） */
    private Long price;

    /** 状态：0 待支付，1 已支付，2 已取消 */
    private Integer status;

    /** 状态：1有效 NULL已取消 */
    private Integer activeFlag;
    /** 下单时间 */
    private Date createdAt;

    /** 支付时间 */
    private Date paidAt;

    /** 取消时间 */
    private Date cancelledAt;


}
