package com.studynote.notes.model.enums.payment;

/**
 * payment.status 支付状态常量。
 */
public class PaymentStatus {

    /** 待支付 */
    public static final Integer PENDING = 0;

    /** 已支付 */
    public static final Integer PAID = 1;

    /** 已退款 */
    public static final Integer REFUNDED = 2;
}
