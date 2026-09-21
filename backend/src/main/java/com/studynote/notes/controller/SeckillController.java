package com.studynote.notes.controller;

import javax.validation.constraints.Min;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.model.base.EmptyVO;
import com.studynote.notes.service.SeckillService;

/**
 * 秒杀接口（用户端）。
 *
 * 这一层只做「接参数 → 调 Service → 返回」，业务逻辑全在 SeckillService。
 */
@RestController
@RequestMapping("/api")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    /**
     * 抢购秒杀活动。
     *
     * @param activityId 活动ID，必须为正整数。
     * @return 抢购结果。
     */
    @PostMapping("/seckill/{activityId}")
    public ApiResponse<EmptyVO> seckill(
            @Min(value = 1, message = "activityId 必须为正整数") @PathVariable Integer activityId) {
        return seckillService.seckill(activityId);
    }
    /**
     * 订单支付。
     *
     * @param orderId 活动ID，必须为正整数。
     * @return 支付结果。
     */
    @PostMapping("/seckill/orders/{orderId}/pay")
    public ApiResponse<EmptyVO> pay(
            @Min(1) @PathVariable Integer orderId) {
        return seckillService.pay(orderId);
    }

}
