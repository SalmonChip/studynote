package com.studynote.notes.controller;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Min;

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
import com.studynote.notes.model.dto.course.CreateCourseBody;
import com.studynote.notes.model.dto.course.UpdateCourseBody;
import com.studynote.notes.model.vo.course.CourseVO;
import com.studynote.notes.model.vo.course.CreateCourseVO;
import com.studynote.notes.service.CourseService;

/**
 * 课程管理接口（管理员端）。
 *
 * 注意：这一层只做「接参数 → 调 Service → 返回」，不写任何业务逻辑。
 */
@RestController
@RequestMapping("/api")
public class CourseController {

    @Autowired
    private CourseService courseService;

    /**
     * 获取课程列表（管理员端）。
     *
     * @return 包含课程列表的响应。
     */
    @GetMapping("/admin/courses")
    public ApiResponse<List<CourseVO>> courseList() {
        return courseService.courseList();
    }

    /**
     * 创建课程。
     *
     * @param body 课程创建信息。
     * @return 包含创建成功课程信息的响应（含回填的 courseId）。
     */
    @PostMapping("/admin/courses")
    public ApiResponse<CreateCourseVO> createCourse(@Valid @RequestBody CreateCourseBody body) {
        return courseService.createCourse(body);
    }

    /**
     * 更新课程。
     *
     * @param courseId 课程ID，必须为正整数。
     * @param body     待更新的字段。
     * @return 包含更新结果的响应。
     */
    @PatchMapping("/admin/courses/{courseId}")
    public ApiResponse<EmptyVO> updateCourse(
            @Min(value = 1, message = "courseId 必须为正整数") @PathVariable Integer courseId,
            @Valid @RequestBody UpdateCourseBody body) {
        return courseService.updateCourse(courseId, body);
    }

    /**
     * 删除课程。
     *
     * @param courseId 课程ID，必须为正整数。
     * @return 包含删除结果的响应。
     */
    @DeleteMapping("/admin/courses/{courseId}")
    public ApiResponse<EmptyVO> deleteCourse(
            @Min(value = 1, message = "courseId 必须为正整数") @PathVariable Integer courseId) {
        return courseService.deleteCourse(courseId);
    }
}
