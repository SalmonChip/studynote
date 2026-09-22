package com.studynote.notes.controller;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Min;

import com.studynote.notes.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.model.base.EmptyVO;
import com.studynote.notes.model.dto.seckillActivity.CreateSeckillActivityBody;
import com.studynote.notes.model.dto.seckillActivity.UpdateSeckillActivityBody;
import com.studynote.notes.model.vo.seckill.CreateSeckillActivityVO;
import com.studynote.notes.model.vo.seckill.SeckillActivityVO;
import com.studynote.notes.service.SeckillActivityService;

@RestController
@RequestMapping("/api")
public class SeckillActivityController {

    @Autowired
    private SeckillActivityService seckillActivityService;

    @Autowired
    private SeckillService seckillService;

    @GetMapping("/admin/seckill-activities")
    public ApiResponse<List<SeckillActivityVO>> activityList() {
        return seckillActivityService.activityList();
    }

    @PostMapping("/admin/seckill-activities")
    public ApiResponse<CreateSeckillActivityVO> createActivity(
            @Valid @RequestBody CreateSeckillActivityBody body) {
        return seckillActivityService.createActivity(body);
    }

    @PatchMapping("/admin/seckill-activities/{activityId}")
    public ApiResponse<EmptyVO> updateActivity(
            @Min(value = 1, message = "activityId 必须为正整数") @PathVariable Integer activityId,
            @Valid @RequestBody UpdateSeckillActivityBody body) {
        return seckillActivityService.updateActivity(activityId, body);
    }

    @DeleteMapping("/admin/seckill-activities/{activityId}")
    public ApiResponse<EmptyVO> deleteActivity(
            @Min(value = 1, message = "activityId 必须为正整数") @PathVariable Integer activityId) {
        return seckillActivityService.deleteActivity(activityId);
    }
    /**
     * 预热秒杀活动：把 MySQL 的库存和已下单用户装载到 Redis。
     *
     * 为什么挂在管理端（/admin）？
     *   预热是运营在活动开始前手动触发的动作，普通用户不该能调。
     *
     * 为什么是 POST？
     *   它会写 Redis（库存 key + 已购用户 Set），是有副作用的操作。
     *   另外它天然可重复执行：同一个活动预热两次，结果是"覆盖"不是"叠加"，
     *   所以重复调用是安全的（这点你实现时要保证，见下面提醒）。
     *
     * TODO(⑤ 加固)：目前管理端接口都没有 @NeedLogin，属于已知缺口。
     *   预热接口裸奔意味着任何人 POST 一下就能重置 Redis 库存，是高危操作。
     *   这一批 admin 接口的统一鉴权放到 ⑤ 阶段处理。
     */
    @PostMapping("/admin/seckill-activities/{activityId}/warm-up")
    public ApiResponse<EmptyVO> warmUp(
            @Min(value = 1, message = "activityId 必须为正整数") @PathVariable Integer activityId) {
        return seckillService.warmUp(activityId);
    }
}
