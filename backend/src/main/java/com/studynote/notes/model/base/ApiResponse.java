package com.studynote.notes.model.base;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API响应类
 *
 * @param <T> 响应数据类型
 */
@Data
@NoArgsConstructor
public class ApiResponse<T> {
    /**
     * 响应码
     */
    private int code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 构造函数
     *
     * @param code 响应码
     * @param message 响应消息
     * @param data 响应数据
     */
    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 创建成功响应
     *
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return success("success", data);
    }

    /**
     * 创建成功响应（自定义 message）
     * <p>
     * 【为什么需要这个重载】
     * 原来的 {@code success(T data)} 把 message 写死成 "success"，
     * 于是 ApiResponseUtil.success(String message) 传进来的消息没地方放，只能被丢掉
     * —— warmUp 的"预热成功，库存=100"就是这么消失的。
     * <p>
     * message 在这个响应模型里是一等公民（前端会拿它弹提示），
     * 那就必须有一个入口能让调用方把它设进去。
     *
     * @param message 响应消息
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return API响应
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage(message);
        response.setData(data);
        return response;
    }

    /**
     * 创建成功响应（无数据）
     *
     * @return API响应
     */
    public static ApiResponse<EmptyVO> success() {
        return success(new EmptyVO());
    }

    /**
     * 创建错误响应
     *
     * @param code 错误码
     * @param message 错误消息
     * @return API响应
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }

    /**
     * 创建带数据的错误响应
     *
     * @param code 错误码
     * @param message 错误消息
     * @param data 错误数据
     * @return API响应
     */
    public static <T> ApiResponse<T> error(int code, String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setData(data);
        return response;
    }
}