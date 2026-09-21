-- 秒杀原子脚本（KEYS 和 ARGV 都由 Java 传进来，脚本里不用声明）
--
-- KEYS[1] = 库存 key        (seckill:stock:{activityId})
-- KEYS[2] = 已购用户 Set key (seckill:order:{activityId})
-- ARGV[1] = userId
--
-- 返回码约定：
--   0 = 抢购成功
--   1 = 重复下单
--   2 = 库存售罄
--   3 = 活动未预热

-- 步骤 1：判重
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return 1
end

-- 步骤 2 + 3：取库存、判存在、判售罄
local stock = redis.call('GET', KEYS[1])
if not stock then
    return 3
end
-- ↓ 步骤 3 你来写：判断 tonumber(stock) <= 0 就 return 2
if tonumber(stock) <=0 then
    return 2
end
-- 步骤 4：扣库存    （提示：redis.call('DECR', ...)，不需要判断返回值）
redis.call('DECR',KEYS[1])
-- 步骤 5：记录用户  （提示：redis.call('SADD', ...)）
redis.call('SADD',KEYS[2],ARGV[1])
-- 步骤 6：return 0
return 0