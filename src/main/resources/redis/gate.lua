-- KEYS[1] = stockKey (String)
-- KEYS[2] = issuedKey (set)
-- ARGV[1] = userId

local stockKey = KEYS[1]
local issuedKey = KEYS[2]
local userId = ARGV[1]

-- already issued? 1: 이미 발급 됨, 0: 아직 발급되지 않음
if redis.call("SISMEMBER", issuedKey, userId) == 1 then return 2
end

local stock = redis.call("GET", stockKey)
if(not stock) then return 0
end

local n = tonumber(stock)
if(not n) or (n<=0) then return 0
end

-- decrement and mark issued atomically
redis.call("DECR", stockKey)
redis.call("SADD", issuedKey, userId)

return 1