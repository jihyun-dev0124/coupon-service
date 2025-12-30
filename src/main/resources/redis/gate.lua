-- KEYS[1] = openKey (String)
-- KEYS[2] = stockKey (String)
-- KEYS[3] = issuedKey (set)
-- ARGV[1] = userId

-- return codes:
-- 1: OK
-- 0: SOLD_OUT
-- 2: ALREADY_ISSUED
-- 3: NOT_OPEN(closed / not configured)

local openKey = KEYS[1]
local stockKey = KEYS[2]
local issuedKey = KEYS[3]
local userId = ARGV[1]

-- open?
if redis.call("GET", openKey) ~= "1" then
    return 3
end

-- already issued? 1: 이미 발급 됨, 0: 아직 발급되지 않음
if redis.call("SISMEMBER", issuedKey, userId) == 1 then
    return 2
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