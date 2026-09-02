package com.studynote.notes.annotation;

import java.lang.annotation.*;

/**
 * 接口限流注解，基于 Redis + Lua 实现固定窗口限流
 *
 * @author kama
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /**
     * 时间窗口内允许的最大请求次数
     */
    int limit() default 10;

    /**
     * 时间窗口长度，单位秒
     */
    int window() default 1;
}
