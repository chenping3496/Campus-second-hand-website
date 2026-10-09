-- 商品库存确认脚本（支付成功后确认预扣）
-- KEYS[1]: 商品库存键 product:stock:{productId}
-- KEYS[2]: 预扣记录键 product:reserve:{productId}:{reserveId}
-- ARGV[1]: 预扣ID（用于验证）

-- 返回值：
-- 0: 预扣记录不存在或已过期
-- 1: 确认成功
-- -1: 预扣ID不匹配

local stockKey = KEYS[1]
local reserveKey = KEYS[2]
local expectedReserveId = ARGV[1]

-- 1. 获取预扣记录
local reserveData = redis.call('HGETALL', reserveKey)
if #reserveData == 0 then
    return 0  -- 预扣记录不存在
end

-- 2. 解析预扣记录
local reserveMap = {}
for i = 1, #reserveData, 2 do
    reserveMap[reserveData[i]] = reserveData[i + 1]
end

-- 3. 验证预扣ID匹配
local actualReserveId = reserveMap['reserveId']
if actualReserveId ~= expectedReserveId then
    return -1  -- 预扣ID不匹配
end

-- 4. 获取超时时间，检查是否已过期
local timestamp = tonumber(reserveMap['timestamp'])
local timeout = tonumber(reserveMap['timeout'])
local currentTime = redis.call('TIME')[1]

if timestamp + timeout < currentTime then
    -- 预扣已过期，回滚库存
    local reservedCount = tonumber(reserveMap['count'])
    if reservedCount then
        redis.call('INCRBY', stockKey, reservedCount)
    end
    redis.call('DEL', reserveKey)
    return 0  -- 已过期
end

-- 5. 确认预扣（只需删除预扣记录，库存已在预扣时扣减）
redis.call('DEL', reserveKey)

return 1  -- 确认成功