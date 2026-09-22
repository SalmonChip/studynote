package com.studynote.notes.service;

import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.model.base.EmptyVO;
import com.studynote.notes.model.dto.seckillActivity.CreateSeckillActivityBody;
import com.studynote.notes.model.dto.seckillActivity.UpdateSeckillActivityBody;
import com.studynote.notes.model.vo.seckill.CreateSeckillActivityVO;
import com.studynote.notes.model.vo.seckill.SeckillActivityVO;

import java.util.List;

public interface SeckillActivityService {

    ApiResponse<CreateSeckillActivityVO> createActivity(CreateSeckillActivityBody body);

    ApiResponse<List<SeckillActivityVO>> activityList();

    ApiResponse<EmptyVO> updateActivity(Integer activityId, UpdateSeckillActivityBody body);

    ApiResponse<EmptyVO> deleteActivity(Integer activityId);
}
