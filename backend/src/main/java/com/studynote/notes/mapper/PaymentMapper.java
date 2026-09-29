package com.studynote.notes.mapper;

import com.studynote.notes.model.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaymentMapper {

    /**
     * 插入一条支付流水，唯一索引 uk_order 防重复收钱。
     */
    int insert(Payment payment);

    /**
     * 按订单ID查询支付流水。
     */
    Payment findByOrderId(@Param("orderId") Integer orderId);
}
