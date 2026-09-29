package com.studynote.notes.service.impl;

import com.studynote.notes.annotation.NeedLogin;
import com.studynote.notes.mapper.UserCourseMapper;
import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.model.vo.course.UserCourseVO;
import com.studynote.notes.scope.RequestScopeData;
import com.studynote.notes.service.UserCourseService;
import com.studynote.notes.utils.ApiResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class UserCourseServiceImpl implements UserCourseService {

    private final RequestScopeData requestScopeData;

    public UserCourseServiceImpl(RequestScopeData requestScopeData) {
        this.requestScopeData = requestScopeData;
    }

    @Autowired
    UserCourseMapper userCourseMapper;

    @Override
    @NeedLogin
    public ApiResponse<List<UserCourseVO>> myCourses() {

        // 用户ID 从 RequestScopeData 取，不由前端传参，防越权
        Long userId = requestScopeData.getUserId();

        // 一次 JOIN 查完，不在 Java 里循环查课程
        List<UserCourseVO> courses = userCourseMapper.findByUserId(userId);

        return ApiResponseUtil.success("查询成功", courses);
    }
}
