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
import com.studynote.notes.model.vo.seckill.SeckillActivityDetailVO;
import com.studynote.notes.model.vo.seckill.SeckillOrderVO;
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
     * 没有路径参数：查谁由 token 决定，不由前端指定，加了 userId 参数就是越权漏洞。
     * 与 POST /api/seckill/{activityId} 段数相同但方法不同，且 Spring MVC 优先匹配字面量路径而非路径变量，不会冲突。
     *
     * @return 订单列表，按下单时间倒序
     */
    @GetMapping("/seckill/orders")
    public ApiResponse<List<SeckillOrderVO>> myOrders() {
        return seckillService.myOrders();
    }

    /**
     * 查询当前可参与的秒杀活动列表（含课程信息），不需要登录。
     * 路径用复数名词、与 /api/seckill/orders 保持一致；与 POST /seckill/{activityId} 不会冲突
     * （方法不同，且字面量路径优先于路径变量）。
     *
     * @return 可参与的活动列表
     */
    @GetMapping("/seckill/activities")
    public ApiResponse<List<SeckillActivityDetailVO>> activityListForUser() {
        return seckillService.activityListForUser();
    }

    /**
     * 订单支付。
     *
     * @param orderId 订单ID，必须为正整数。
     * @return 支付结果。
     */
    @PostMapping("/seckill/orders/{orderId}/pay")
    public ApiResponse<EmptyVO> pay(
            @Min(1) @PathVariable Integer orderId) {
        return seckillService.pay(orderId);
    }

}
