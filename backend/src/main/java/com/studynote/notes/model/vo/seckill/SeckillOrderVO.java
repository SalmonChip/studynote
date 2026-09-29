package com.studynote.notes.model.vo.seckill;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 「我的秒杀订单」列表项。
 */
@Data
public class SeckillOrderVO {

    /** 订单ID */
    private Integer orderId;

    /** 活动ID */
    private Integer activityId;

    /** 课程ID */
    private Integer courseId;

    private String courseTitle;

    private String courseCoverUrl;

    /** 成交价（元），下单时的价格快照 */
    private BigDecimal price;

    /** 0 待支付 / 1 已支付 / 2 已取消 */
    private Integer status;

    /** 下单时间 */
    private Date createdAt;

    /** 支付时间，未支付为 null */
    private Date paidAt;

    /** 取消时间，未取消为 null */
    private Date cancelledAt;
}
