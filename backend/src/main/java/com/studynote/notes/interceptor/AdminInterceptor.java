package com.studynote.notes.interceptor;

import com.studynote.notes.exception.NoPermissionException;
import com.studynote.notes.mapper.UserMapper;
import com.studynote.notes.model.entity.User;
import com.studynote.notes.model.enums.user.UserRole;
import com.studynote.notes.scope.RequestScopeData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Objects;

/**
 * 管理员鉴权拦截器，只管 /api/admin/** 路径。
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Autowired
    private RequestScopeData requestScopeData;

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        if(!requestScopeData.isLogin()){
            throw new NoPermissionException("用户未登录");
        }
        System.out.println("管理者校验");
        // isAdmin 实时查库，撤销权限立即生效
        User user = userMapper.findById(requestScopeData.getUserId());
        // isAdmin 是 Integer，用 equals 比较
        if(user == null|| !Objects.equals(user.getIsAdmin(), UserRole.IS_ADMIN)){
            throw new NoPermissionException("无管理员权限");
        }
        return true;
    }
}
