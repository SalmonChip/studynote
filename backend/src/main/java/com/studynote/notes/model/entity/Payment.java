package com.studynote.notes.model.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付流水实体类（payment 表），只追加不修改，order_id 唯一约束防重复收钱。
 */
@Data
public class Payment {

    /** 主键 */
    private Long paymentId;

    /** 关联订单ID */
    private Integer orderId;

    /** 付款用户ID */
    private Long userId;

    /** 支付金额（元） */
    private BigDecimal amount;

    /** 支付渠道 */
    private String channel;

    /** 支付状态：0 待支付，1 已支付，2 已退款 */
    private Integer status;

    /** 支付完成时间 */
    private Date payTime;
}
