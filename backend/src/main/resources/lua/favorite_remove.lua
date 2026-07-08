-- KEYS[1] = product:fav:{productId}
-- KEYS[2] = user:fav:{userId}
-- ARGV[1] = productId
-- 原子移除收藏：检查是否存在 → 移除用户收藏集合 → 递减商品收藏计数
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 0 then
    return -1
end
redis.call('SREM', KEYS[2], ARGV[1])
local count = redis.call('DECR', KEYS[1])
if count < 0 then
    redis.call('SET', KEYS[1], 0)
    return 0
end
return count
