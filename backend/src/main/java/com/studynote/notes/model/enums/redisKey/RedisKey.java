package com.studynote.notes.model.enums.redisKey;

/**
 * Redis 键名管理类
 * 用于统一管理和生成 Redis 中使用的各种键名
 * 遵循 Redis 键名命名规范 : 使用冒号分隔的层级结构
 */
public class RedisKey {
    /**
     * 生成注册验证码的 Redis 键名
     * 
     * @param email 用户邮箱地址
     * @return 格式为 "verification_code:register:{email}" 的 Redis 键名
     */
    public static String registerVerificationCode(String email) {
        return "email:register_verification_code:" + email;
    }

    /**
     * 生成注册验证码限制的 Redis 键名 <br/>
     * 用于记录用户发送验证码的频率限制
     * 
     * @param email 用户邮箱地址
     * @return 格式为 "email:register_verification_code:limit:{email}" 的 Redis 键名
     */
    public static String registerVerificationLimitCode(String email) {
        return "email:register_verification_code:limit:" + email;
    }

    /**
     * 生成邮件任务队列的 Redis 键名
     * 
     * @return 格式为 "queue:email:task" 的 Redis 键名
     */
    public static String emailTaskQueue() {
        return "queue:email:task";
    }

    /**
     * 生成站内消息任务队列的 Redis 键名
     *
     * @return 格式为 "queue:message:task" 的 Redis 键名
     */
    public static String messageTaskQueue() {
        return "queue:message:task";
    }

    /**
     * 秒杀库存 key
     * 格式："seckill:stock:{activityId}"
     * 值类型：String（数字）
     */
    public static String seckillStock(Integer activityId) {
        return  "seckill:stock:"+activityId;
    }

    /**
     * 秒杀已下单用户集合 key
     * 格式："seckill:order:{activityId}"
     * 值类型：Set，成员是 userId 字符串
     */
    public static String seckillOrderUserSet(Integer activityId) {
        return "seckill:order:"+activityId;
    }

    /**
     * 秒杀订单消息队列（Redis Stream）key
     * 格式："stream:seckill:order"
     * 注意：不加活动ID，因为消息体里带了 activityId
     */
    public static String seckillOrderStream() {
        return "stream:seckill:order";
    }

    /**
     * 秒杀活动预热集合
     * 格式："stream:seckill:order"
     *
     */
    public static String seckillActivity(Integer activityId) {
        return "seckill:activity:"+activityId;
    }

}
