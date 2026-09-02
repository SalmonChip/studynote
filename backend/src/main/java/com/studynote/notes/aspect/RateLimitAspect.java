package com.studynote.notes.aspect;

import com.studynote.notes.annotation.RateLimit;
import com.studynote.notes.scope.RequestScopeData;
import com.studynote.notes.utils.ApiResponseUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;

/**
 * 接口限流切面：基于 Redis 原子自增 + 过期时间实现固定窗口限流
 * 通过 Lua 脚本保证「计数 + 首次设置过期时间」的原子性，避免并发下计数与过期时间不一致
 */
@Aspect
@Component
public class RateLimitAspect {

    /**
     * 固定窗口限流 Lua 脚本：
     * 1. INCR 自增当前窗口计数
     * 2. 首次访问时设置过期时间（即窗口时长）
     * 3. 超过阈值返回 0（拒绝），否则返回 1（放行）
     */
    private static final String RATE_LIMIT_LUA =
            "local current = redis.call('INCR', KEYS[1]) " +
            "if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[2]) end " +
            "if current > tonumber(ARGV[1]) then return 0 end " +
            "return 1";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RequestScopeData requestScopeData;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = buildKey(joinPoint);

        RedisScript<Long> script = new DefaultRedisScript<>(RATE_LIMIT_LUA, Long.class);
        Long allowed = stringRedisTemplate.execute(
                script,
                Collections.singletonList(key),
                String.valueOf(rateLimit.limit()),
                String.valueOf(rateLimit.window()));

        if (allowed != null && allowed == 0L) {
            return ApiResponseUtil.error("操作过于频繁，请稍后再试");
        }
        return joinPoint.proceed();
    }

    /**
     * 构造限流 key：方法维度 + 用户维度（未登录则退化为 IP）
     */
    private String buildKey(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String method = signature.getDeclaringType().getSimpleName() + "." + signature.getName();

        Long userId = requestScopeData.getUserId();
        if (userId != null) {
            return "rate_limit:" + method + ":u" + userId;
        }

        String ip = getClientIp();
        return "rate_limit:" + method + ":ip" + ip;
    }

    /**
     * 获取客户端 IP，优先取 X-Forwarded-For 的首个地址
     */
    private String getClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        String forwarded = attrs.getRequest().getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return attrs.getRequest().getRemoteAddr();
    }
}
