--[[

根据sequenceId判断是否需要发送tick通知

KEYS:
  1: 最新Ticks的Key

ARGV:
  1: sequenceId
  2: JSON字符串表示的tick数组："[{...},{...},...]"
  3: JSON字符串表示的tick数组："["{...}","{...}",...]"
--]]

-- 上次更新的SequenceID
local KEY_LAST_SEQ = '_TickSeq_'
-- 最新Ticks的Key
local LIST_RECENT_TICKS = KEYS[1]

-- 输入的SequenceID
local seqId = ARGV[1]
-- 输入的JSON字符串表示的tick数组："[{...},{...},...]"
local jsonData = ARGV[2]
-- 输入的JSON字符串表示的tick数组："["{...}","{...}",...]"
local strData = ARGV[3]

-- 获取上次更新的sequenceId:
local lastSeqId = redis.call('GET', KEY_LAST_SEQ)
local ticks, len

if not lastSeqId or tonumber(seqId) > tonumber(lastSeqId) then
    --发送广播
    redis.call('PUBLISH', 'notification', '{"type":"tick","sequenceId":'..seqId..',"data":'..jsonData..'}')
    --更新定序Id
    redis.call('SET', KEY_LAST_SEQ, seqId)
    -- 更新最新tick列表:
    ticks = cjson.decode(strData)
    --RPUSH命令用于将一个或多个元素插入到列表的末尾。如果列表不存在，则会创建一个新列表
    len = redis.call('RPUSH', LIST_RECENT_TICKS, unpack(ticks))
    if len > 100 then
        -- 裁剪LIST以保存最新的100个Tick; LTRIM命令用于修剪（trim）一个列表（list），仅保留列表中指定范围内的元素
        redis.call('LTRIM', LIST_RECENT_TICKS, len-100, len-1)
    end
    return true
end

-- 无更新返回false
return false