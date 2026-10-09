-- 商品库存原子预扣脚本
-- KEYS[1]: 商品库存键 product:stock:{productId}
-- KEYS[2]: 预扣记录键 product:reserve:{productId}:{reserveId}
-- ARGV[1]: 购买数量
-- ARGV[2]: 预扣超时秒数（如300表示5分钟）
-- ARGV[3]: 预扣ID（唯一标识）

-- 返回值：
-- 0: 库存不足
-- 1: 预扣成功
-- -1: 预扣已存在（幂等性处理）

local stockKey = KEYS[1]
local reserveKey = KEYS[2]
local buyCount = tonumber(ARGV[1])
local timeoutSeconds = tonumber(ARGV[2])
local reserveId = ARGV[3]

-- 1. 检查库存是否充足
local currentStock = tonumber(redis.call('GET', stockKey))
if not currentStock or currentStock < buyCount then
    return 0  -- 库存不足
end

-- 2. 检查是否已经存在预扣（幂等性）
local existingReserve = redis.call('GET', reserveKey)
if existingReserve then
    return -1  -- 预扣已存在，返回-1但不报错（幂等）
end

-- 3. 预扣库存
redis.call('DECRBY', stockKey, buyCount)

-- 4. 设置预扣记录，包含预扣ID和超时时间
local reserveData = {
    count = buyCount,
    reserveId = reserveId,
    timestamp = redis.call('TIME')[1], -- Redis服务器时间戳
    timeout = timeoutSeconds
}
redis.call('HMSET', reserveKey,
    'count', buyCount,
    'reserveId', reserveId,
    'timestamp', reserveData.timestamp,
    'timeout', timeoutSeconds
)
redis.call('EXPIRE', reserveKey, timeoutSeconds)

return 1  -- 预扣成功