package com.studynote.notes.controller;

import javax.validation.constraints.Min;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.model.base.EmptyVO;
import com.studynote.notes.model.entity.SeckillOrder;
import com.studynote.notes.service.SeckillService;

import java.util.List;

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
     * 查询当前登录用户的秒杀订单列表。
     * <p>
     * 【注意没有路径参数】：查谁由 token 决定，不由前端指定。
     * 加了 userId 参数就是越权漏洞。
     * <p>
     * 【和 pay 的路由为什么不会打架】
     * 本接口是 {@code GET /api/seckill/orders}（2 段），
     * pay 是 {@code POST /api/seckill/orders/{orderId}/pay}（4 段）。
     * 段数不同，且 HTTP 方法也不同，Spring MVC 不会搞混。
     * <p>
     * 顺带一提 {@code POST /api/seckill/{activityId}} 和本路径都是 2 段，
     * 但方法不同（POST vs GET），且即使同方法 Spring 也优先匹配
     * <b>更具体的字面量路径</b>而不是路径变量。
     *
     * @return 订单列表，按下单时间倒序
     */
    @GetMapping("/seckill/orders")
    public ApiResponse<List<SeckillOrder>> myOrders() {
        return seckillService.myOrders();
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
