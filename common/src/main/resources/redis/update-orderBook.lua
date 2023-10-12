--[[
刷新OrderBook快照:

KEYS:
  1: key: OrderBook快照的key

ARGV:
  1: seqId: 本次的SequenceId
  2: data: OrderBook快照的JSON数据
]]--

local KEY_LAST_SEQ = '_OBLastSeqId_'
local key = KEYS[1]
local seqId = ARGV[1]
local data = ARGV[2]

--获取orderBook上次更新的定序id
local lastSeqId = redis.call('GET', KEY_LAST_SEQ)

if not lastSeqId and tonumber(seqId) > lastSeqId then
    --保存新的订单簿定序id
    redis.call("SET", KEY_LAST_SEQ, seqId)
    --保存新的订单簿JSON
    redis.call("SET", key, data)
    --发送通知
    redis.call("PUBLISH", "notification", '{"type": "orderbook", "data": '..data..'}')
    return true
end

return false