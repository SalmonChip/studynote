package com.studynote.notes.utils;

import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.model.base.Pagination;
import com.studynote.notes.model.base.PaginationApiResponse;
import com.studynote.notes.model.base.TokenApiResponse;
import org.springframework.http.HttpStatus;

public class ApiResponseUtil {
    /**
     * 构建成功的响应
     *
     * @param message 响应消息
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.success(message, null);
    }

    /**
     * 构建成功的响应（带数据）
     * <p>
     * 注意第二个参数是 data，不是 message —— message 是第一个参数。
     * 两个参数以前都被丢掉了（一个丢 message、一个直接调 success(data)），
     * 现在都透传给 ApiResponse。
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.success(message, data);
    }

    /**
     * 构建参数错误的响应
     */
    public static <T> ApiResponse<T> error(String msg) {
        return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), msg);
    }

    /**
     * 构建 TokenApiResponse
     */
    public static <T> TokenApiResponse<T> success(String msg, T data, String token) {
        return new TokenApiResponse<>(HttpStatus.OK.value(), msg, data, token);
    }

    /**
     * 构建 PaginationApiResponse
     */
    public static <T> PaginationApiResponse<T> success(String msg, T data, Pagination pagination) {
        return new PaginationApiResponse<>(HttpStatus.OK.value(), msg, data, pagination);
    }
}
