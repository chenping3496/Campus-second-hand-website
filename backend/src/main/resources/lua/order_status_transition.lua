-- KEYS[1] = order:status:{orderId}
-- ARGV[1] = expectedCurrentStatus
-- ARGV[2] = newStatus
-- 原子状态转换：CAS 操作，仅当当前状态等于预期状态时才更新
local current = redis.call('GET', KEYS[1])
if current == ARGV[1] then
    redis.call('SET', KEYS[1], ARGV[2])
    return 1
end
return 0
