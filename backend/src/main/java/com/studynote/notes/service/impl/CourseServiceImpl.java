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
        // insert 之后 courseId 由 MyBatis 回填（XML 里的 useGeneratedKeys）
        Course course = new Course();
        BeanUtils.copyProperties(body, course);
        courseMapper.insert(course);
        CreateCourseVO vo = new CreateCourseVO();
        vo.setCourseId(course.getCourseId());
        return ApiResponseUtil.success("创建成功", vo);
    }

    @Override
    public ApiResponse<List<CourseVO>> courseList() {
        List<Course> list = courseMapper.courseList();
        List<CourseVO> voList = list.stream().map(
                course -> {
                    CourseVO courseVO = new CourseVO();
                    BeanUtils.copyProperties(course, courseVO);
                    return courseVO;
                }).collect(Collectors.toList());
        return  ApiResponseUtil.success("获取课程列表成功", voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<EmptyVO> updateCourse(Integer courseId, UpdateCourseBody body) {
        Course existing  = courseMapper.findById(courseId);
        if (existing  == null) {
            return ApiResponseUtil.error("课程不存在");
        }

        // body 里非 null 的字段才 set，配合 XML 里的 <if> 动态更新
        Course course = new Course();
        BeanUtils.copyProperties(body, course);
        course.setCourseId(courseId);

        courseMapper.update(course);
        return ApiResponseUtil.success("更新成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<EmptyVO> deleteCourse(Integer courseId) {
        Course existing = courseMapper.findById(courseId);
        if (existing == null) {
            return ApiResponseUtil.error("课程不存在");
        }
        courseMapper.deleteById(courseId);
        return ApiResponseUtil.success("删除成功");
    }
}
