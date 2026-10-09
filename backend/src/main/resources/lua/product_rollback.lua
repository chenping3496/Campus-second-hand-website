-- 商品库存回滚脚本
-- KEYS[1]: 商品库存键 product:stock:{productId}
-- KEYS[2]: 预扣记录键 product:reserve:{productId}:{reserveId}
-- ARGV[1]: 预扣ID（用于验证）

-- 返回值：
-- 0: 预扣记录不存在或已过期
-- 1: 回滚成功
-- -1: 预扣ID不匹配

local stockKey = KEYS[1]
local reserveKey = KEYS[2]
local expectedReserveId = ARGV[1]

-- 1. 获取预扣记录
local reserveData = redis.call('HGETALL', reserveKey)
if #reserveData == 0 then
    return 0  -- 预扣记录不存在
end

-- 2. 解析预扣记录（Redis HGETALL返回的是交替的字段名和值）
local reserveMap = {}
for i = 1, #reserveData, 2 do
    reserveMap[reserveData[i]] = reserveData[i + 1]
end

-- 3. 验证预扣ID匹配
local actualReserveId = reserveMap['reserveId']
if actualReserveId ~= expectedReserveId then
    return -1  -- 预扣ID不匹配，可能已经处理过或被其他请求抢占了
end

-- 4. 获取预扣数量
local reservedCount = tonumber(reserveMap['count'])
if not reservedCount then
    return 0  -- 数据格式错误，当作不存在处理
end

-- 5. 回滚库存
redis.call('INCRBY', stockKey, reservedCount)

-- 6. 删除预扣记录
redis.call('DEL', reserveKey)

return 1  -- 回滚成功