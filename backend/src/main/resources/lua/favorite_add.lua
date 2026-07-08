-- KEYS[1] = product:fav:{productId}
-- KEYS[2] = user:fav:{userId}
-- ARGV[1] = productId
-- 原子添加收藏：检查是否已收藏 → 添加用户收藏集合 → 递增商品收藏计数
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return -1
end
redis.call('SADD', KEYS[2], ARGV[1])
return redis.call('INCR', KEYS[1])
