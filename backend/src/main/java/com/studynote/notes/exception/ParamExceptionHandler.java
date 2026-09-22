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
     * ⚠️ 这里的 403 是【响应体里的 code】，不是 HTTP 状态码 —— 别搞混。
     * Spring MVC 里 @ExceptionHandler 返回普通对象时，HTTP 状态码固定是 200，
     * 语义全靠 body 里的 code 字段传达。整个项目都是这个约定
     * （ApiResponse.success 塞 200，ApiResponseUtil.error 塞 400）。
     * <p>
     * 想真的返回 HTTP 403，得再加 @ResponseStatus(HttpStatus.FORBIDDEN)，
     * 但那就跟项目其余接口不一致了，前端也得跟着改。暂时不动。
     * <p>
     * ⚠️ 为什么必须有这个方法：下面那个 catch-all 会把【所有】异常都渲染成 code=500，
     * 但「你没权限」根本不是服务器内部错误，而是【这个请求本来就不该发】。
     * 对调用方来说这是两件完全不同的事：
     *     403 → 别重试了，去登录 / 去要权限
     *     500 → 服务器出问题了，可以稍后重试
     * 把 403 伪装成 500，前端就只能瞎猜。
     * <p>
     * Spring 会自动挑【最具体】的 handler，所以这个比下面的 Exception 优先。
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
