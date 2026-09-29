package com.studynote.notes.service;

import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.model.vo.course.UserCourseVO;

import java.util.List;

/**
 * 已购课程（用户端）。
 */
public interface UserCourseService {

    /**
     * 查询当前登录用户的已购课程列表。
     */
    ApiResponse<List<UserCourseVO>> myCourses();
}
