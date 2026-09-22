package com.studynote.notes.service.impl;

import com.studynote.notes.mapper.CourseMapper;
import com.studynote.notes.mapper.SeckillActivityMapper;
import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.model.base.EmptyVO;
import com.studynote.notes.model.dto.seckill.CreateSeckillActivityBody;
import com.studynote.notes.model.dto.seckill.UpdateSeckillActivityBody;
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

    // 校验「课程是否存在」要跨表查 course，所以这里注入第二个 Mapper
    @Autowired
    private CourseMapper courseMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<CreateSeckillActivityVO> createActivity(CreateSeckillActivityBody body) {
        // 【第1步】业务校验1：courseMapper.findById(body.getCourseId())
        //          为 null → return ApiResponseUtil.error("课程不存在")

        // 【第2步】业务校验2：body.getSeckillPrice() >= course.getPrice()
        //          → return ApiResponseUtil.error("秒杀价必须低于原价")
        //          （跨表比价：拿 body 的秒杀价，和第1步查出来的 course 原价比）

        // 【第3步】业务校验3：开始时间不早于结束时间
        //          → return ApiResponseUtil.error("开始时间必须早于结束时间")
        //          （提示：Date 有 before() 方法；失败条件是 !startTime.before(endTime)）

        // 【第4步】组装实体：new SeckillActivity()，BeanUtils.copyProperties(body, activity)
        //          status 为 null 时 setStatus(1)（默认上架，跟 course 一样）

        // 【第5步】插入：seckillActivityMapper.insert(activity)，回填 activityId

        // 【第6步】组装 VO：new CreateSeckillActivityVO()，setActivityId(activity.getActivityId())

        // 【第7步】返回：return ApiResponseUtil.success("创建成功", vo)
        Course course = courseMapper.findById(body.getCourseId());
        if(course == null ) {
            return ApiResponseUtil.error("课程不存在");
        }
        // ★ BigDecimal 的比大小必须用 compareTo，不能用 >= / < 运算符。
        //   Java 里没有为 BigDecimal 重载运算符（它不是基本类型），写 >= 直接编译不过。
        //   compareTo 返回 -1 / 0 / 1，判断"小于"就是 < 0。
        //   也别用 equals —— 它连标度一起比，2.0 和 2.00 会判为不等，
        //   金额比较要用 compareTo。
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
        // 【第1步】查全表：seckillActivityMapper.activityList()

        // 【第2步】Entity → VO：stream + map + copyProperties + Collectors.toList()

        // 【第3步】返回：return ApiResponseUtil.success("获取活动列表成功", voList)
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
        // 【第1步】业务校验：seckillActivityMapper.findById(activityId)
        //          为 null → return ApiResponseUtil.error("活动不存在")

        // 【第2步】组装实体：new SeckillActivity()，copyProperties(body, activity)
        //          然后 setActivityId(activityId)（别忘了，否则 WHERE 是 null）

        // 【第3步】更新：seckillActivityMapper.update(activity)

        // 【第4步】返回：return ApiResponseUtil.success("更新成功")
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
        // 【第1步】业务校验：seckillActivityMapper.findById(activityId)
        //          为 null → return ApiResponseUtil.error("活动不存在")

        // 【第2步】删除：seckillActivityMapper.deleteById(activityId)

        // 【第3步】返回：return ApiResponseUtil.success("删除成功")

        // TODO 你来实现，替换下面这行
        SeckillActivity existing = seckillActivityMapper.findById(activityId);
        if(existing == null){
            return ApiResponseUtil.error("活动不存在");
        }
        seckillActivityMapper.deleteById(activityId);
        return ApiResponseUtil.success("删除成功");
    }
}
