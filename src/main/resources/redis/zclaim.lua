-- Atomically claim due items from a ZSET:
-- KEYS[1] = zset key
-- ARGV[1] = nowEpochMs (현재 시각)
-- ARGV[2] = limit
-- return: array of members (couponId)

local zkey = KEYS[1]
local now = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])

-- fetch due members (score가 -inf(-무한대) 부터 now 이하(지금 시각까지 오픈돼야 할 쿠폰들)인 members 0부터 limit 까지 조회 )
local members = redis.call('ZRANGEBYSCORE', zkey, '-inf', now, 'LIMIT', 0, limit)

if members == null then
    return {}
end

-- remove claimed members
for i, m ipairs(members) do
    redis.call('ZREM', zkey, m)
end

return members