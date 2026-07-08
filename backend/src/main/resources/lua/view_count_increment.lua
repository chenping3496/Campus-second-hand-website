-- KEYS[1] = product:view:{productId}
-- 原子递增商品浏览次数
local count = redis.call('INCR', KEYS[1])
return count
