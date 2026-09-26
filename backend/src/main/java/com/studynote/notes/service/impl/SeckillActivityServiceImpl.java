package com.studynote.notes.service.impl;

import com.studynote.notes.mapper.CourseMapper;
import com.studynote.notes.mapper.SeckillActivityMapper;
import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.model.base.EmptyVO;
import com.studynote.notes.model.dto.seckillActivity.CreateSeckillActivityBody;
import com.studynote.notes.model.dto.seckillActivity.UpdateSeckillActivityBody;
import com.studynote.notes.model.entity.Course;
import com.studynote.notes.model.entity.SeckillActivity;
import com.studynote.notes.model.vo.seckill.CreateSeckillActivityVO;
import com.studynote.notes.model.vo.seckill.SeckillActivityVO;
import com.studynote.notes.service.SeckillActivityService;
import com.studynote.notes.utils.ApiResponseUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SeckillActivityServiceImpl implements SeckillActivityService {

    @Autowired
    private SeckillActivityMapper seckillActivityMapper;

    // 校验课程是否存在要跨表查 course，所以注入第二个 Mapper
    @Autowired
    private CourseMapper courseMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<CreateSeckillActivityVO> createActivity(CreateSeckillActivityBody body) {
        // 依次校验课程存在、秒杀价低于原价、开始时间早于结束时间，然后落库并返回 activityId
        Course course = courseMapper.findById(body.getCourseId());
        if(course == null ) {
            return ApiResponseUtil.error("课程不存在");
        }
        // BigDecimal 比大小必须用 compareTo，不能用 >=，也不能用 equals（equals 连标度一起比，2.0 和 2.00 会判为不等）
        if(body.getSeckillPrice().compareTo(course.getPrice()) >= 0){
            return ApiResponseUtil.error("秒杀价必须低于原价");
        }
        if(!body.getStartTime().before(body.getEndTime())){
            return ApiResponseUtil.error("开始时间必须早于结束时间");
        }
        SeckillActivity seckillActivity = new SeckillActivity();
        BeanUtils.copyProperties(body, seckillActivity);
        if(seckillActivity.getStatus()==null){
            seckillActivity.setStatus(1);
        }
        seckillActivityMapper.insert(seckillActivity);
        CreateSeckillActivityVO vo = new CreateSeckillActivityVO();
        vo.setActivityId(seckillActivity.getActivityId());
        return ApiResponseUtil.success("创建成功", vo);
    }

    @Override
    public ApiResponse<List<SeckillActivityVO>> activityList() {
        // 查全表并把 Entity 转成 VO
        List<SeckillActivity> seckillActivityList = seckillActivityMapper.activityList();
        List<SeckillActivityVO> voList = seckillActivityList.stream().map(
                seckillActivity -> {
                    SeckillActivityVO vo = new SeckillActivityVO();
                    BeanUtils.copyProperties(seckillActivity, vo);
                    return vo;
                }
        ).toList();
        return ApiResponseUtil.success("获取活动列表成功", voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<EmptyVO> updateActivity(Integer activityId, UpdateSeckillActivityBody body) {
        // 校验活动存在后更新，setActivityId 不能漏，否则 WHERE 是 null
        SeckillActivity existing = seckillActivityMapper.findById(activityId);
        if(existing  == null){
            return ApiResponseUtil.error("活动不存在");
        }

        SeckillActivity seckillActivity = new SeckillActivity();
        BeanUtils.copyProperties(body, seckillActivity);
        seckillActivity.setActivityId(activityId);
        seckillActivityMapper.update(seckillActivity);
        return ApiResponseUtil.success("更新成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<EmptyVO> deleteActivity(Integer activityId) {
        // 校验活动存在后删除
        SeckillActivity existing = seckillActivityMapper.findById(activityId);
        if(existing == null){
            return ApiResponseUtil.error("活动不存在");
        }
        seckillActivityMapper.deleteById(activityId);
        return ApiResponseUtil.success("删除成功");
    }
}
