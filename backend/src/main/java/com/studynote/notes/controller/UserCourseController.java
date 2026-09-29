package com.studynote.notes.controller;

import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.model.vo.course.UserCourseVO;
import com.studynote.notes.service.UserCourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 已购课程接口（用户端）。
 */
@RestController
@RequestMapping("/api")
public class UserCourseController {

    @Autowired
    private UserCourseService userCourseService;

    /**
     * 查询当前登录用户的已购课程列表。
     */
    @GetMapping("/users/courses")
    public ApiResponse<List<UserCourseVO>> myCourses() {
        return userCourseService.myCourses();
    }
}
