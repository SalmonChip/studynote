package com.studynote.notes.service.impl;

import com.studynote.notes.mapper.CourseMapper;
import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.model.base.EmptyVO;
import com.studynote.notes.model.dto.course.CreateCourseBody;
import com.studynote.notes.model.dto.course.UpdateCourseBody;
import com.studynote.notes.model.entity.Course;
import com.studynote.notes.model.vo.course.CourseVO;
import com.studynote.notes.model.vo.course.CreateCourseVO;
import com.studynote.notes.service.CourseService;
import com.studynote.notes.utils.ApiResponseUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseServiceImpl implements CourseService {

    @Autowired
    private CourseMapper courseMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<CreateCourseVO> createCourse(CreateCourseBody body) {
        // 【第1步】组装实体：new 一个 Course，用 BeanUtils.copyProperties(body, course)
        //          （body 和 course 同名字段自动拷贝：title/description/coverUrl/price/status）

        // 【第2步】插入：courseMapper.insert(course)
        //          注意：insert 之后 course.getCourseId() 已被 MyBatis 回填（XML 里 useGeneratedKeys）

        // 【第3步】组装 VO：new 一个 CreateCourseVO，把 courseId 塞进去（setCourseId(course.getCourseId())）

        // 【第4步】返回：return ApiResponseUtil.success("创建成功", vo)

        // TODO 你来实现，替换下面这行
        Course course = new Course();
        BeanUtils.copyProperties(body, course);
        courseMapper.insert(course);
        CreateCourseVO vo = new CreateCourseVO();
        vo.setCourseId(course.getCourseId());
        return ApiResponseUtil.success("创建成功", vo);
    }

    @Override
    public ApiResponse<List<CourseVO>> courseList() {
        // 【第1步】查全表：List<Course> list = courseMapper.courseList()
        List<Course> list = courseMapper.courseList();
        // 【第2步】Entity → VO：把 list 转成 List<CourseVO>
        //          用 stream + map：每个 Course 用 BeanUtils.copyProperties(course, new CourseVO())
        List<CourseVO> voList = list.stream().map(
                course -> {
                    CourseVO courseVO = new CourseVO();
                    BeanUtils.copyProperties(course, courseVO);
                    return courseVO;
                }).collect(Collectors.toList());
        // 【第3步】返回：return ApiResponseUtil.success("获取课程列表成功", voList)
        return  ApiResponseUtil.success("获取课程列表成功", voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<EmptyVO> updateCourse(Integer courseId, UpdateCourseBody body) {
        // 【第1步】业务校验：courseMapper.findById(courseId)
        //          若返回 null，return ApiResponseUtil.error("课程不存在")
        Course existing  = courseMapper.findById(courseId);
        if (existing  == null) {
            return ApiResponseUtil.error("课程不存在");
        }

        // 【第2步】组装实体：new 一个 Course，只 set 要改的字段 + courseId
        //          （body 里非 null 的字段才 set，配合 XML 里的 <if> 动态更新）
        Course course = new Course();
        BeanUtils.copyProperties(body, course);
        course.setCourseId(courseId);

        // 【第3步】更新：courseMapper.update(course)
        courseMapper.update(course);
        return ApiResponseUtil.success("更新成功");
        // 【第4步】返回：return ApiResponseUtil.success("更新成功")

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<EmptyVO> deleteCourse(Integer courseId) {
        // 【第1步】业务校验：courseMapper.findById(courseId)
        //          若返回 null，return ApiResponseUtil.error("课程不存在")

        // 【第2步】删除：courseMapper.deleteById(courseId)

        // 【第3步】返回：return ApiResponseUtil.success("删除成功")

        Course existing = courseMapper.findById(courseId);
        if (existing == null) {
            return ApiResponseUtil.error("课程不存在");
        }
        courseMapper.deleteById(courseId);
        return ApiResponseUtil.success("删除成功");
    }
}
