package com.studynote.notes.service;

import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.model.base.EmptyVO;
import com.studynote.notes.model.dto.course.CreateCourseBody;
import com.studynote.notes.model.dto.course.UpdateCourseBody;
import com.studynote.notes.model.vo.course.CourseVO;
import com.studynote.notes.model.vo.course.CreateCourseVO;

import java.util.List;

public interface CourseService {

    /**
     * 创建课程（管理员端）。
     *
     * @param body 课程创建信息
     * @return 创建成功后的课程信息（含回填的 courseId）
     */
    ApiResponse<CreateCourseVO> createCourse(CreateCourseBody body);

    /**
     * 获取课程列表（管理员端，当前不分页）。
     *
     * @return 课程列表
     */
    ApiResponse<List<CourseVO>> courseList();

    /**
     * 更新课程信息（动态更新，只改非 null 字段）。
     *
     * @param courseId 课程ID
     * @param body     待更新的字段
     * @return 空结果
     */
    ApiResponse<EmptyVO> updateCourse(Integer courseId, UpdateCourseBody body);

    /**
     * 删除课程。
     *
     * @param courseId 课程ID
     * @return 空结果
     */
    ApiResponse<EmptyVO> deleteCourse(Integer courseId);
}
