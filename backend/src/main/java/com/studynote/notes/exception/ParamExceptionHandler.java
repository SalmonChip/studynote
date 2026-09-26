package com.studynote.notes.exception;

import com.studynote.notes.model.base.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ParamExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Validation Failed", errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Map<String, String>> handleConstraintViolationExceptions(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), violation.getMessage())
        );
        return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Validation Failed", errors);
    }

    /**
     * 权限不足（未登录 / 不是管理员）。
     * <p>
     * 这里的 403 是响应体里的 code，不是 HTTP 状态码：@ExceptionHandler 返回普通对象时
     * HTTP 状态码固定是 200，语义全靠 body 里的 code 传达（整个项目都是这个约定）。
     * <p>
     * 必须单独写这个方法，否则会被下面的 catch-all 渲染成 code=500。403（别重试，去登录）
     * 和 500（服务端出错，可重试）对调用方是两回事。
     * <p>
     * Spring 会自动挑最具体的 handler，所以这个比下面的 Exception 优先。
     */
    @ExceptionHandler(NoPermissionException.class)
    public ApiResponse<String> handleNoPermission(NoPermissionException ex) {
        return ApiResponse.error(HttpStatus.FORBIDDEN.value(), "Forbidden", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handleException(Exception ex) {
        return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", ex.getMessage());
    }
}
