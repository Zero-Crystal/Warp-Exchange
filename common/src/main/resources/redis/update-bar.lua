--[[
  根据sequenceId和已合并的tick更新Bar数据

  参数：
  KEYS:
    1. sec-bar的key
    2. min-bar的key
    3. hour-bar的key
    4. day-bar的key
  ARGV:
    1. sequenceId
    2. secTimestamp
    3. minTimestamp
    4. hourTimestamp
    5. dayTimestamp
    6. openPrice
    7. highPrice
    8. lowPrice
    9. closePrice
    10. quantity

  Redis存储的Bar数据结构：[timestamp, open, high, low, close, quantity, volume]
  ZScoredSet:
    key: '_day_bars_'
    key: '_hour_bars_'
    key: '_min_bars_'
    key: '_sec_bars_'
  Key: _BarSeq_ 存储上次更新的SequenceId
--]]

local function merge(existBar, newBar)
    --highPrice
    existBar[3] = Math.max(existBar[3], newBar[3])
    --lowPrice
    existBar[4] = Math.min(existBar[4], newBar[4])
    --closePrice
    existBar[5] = newBar[5]
    --quantity
    existBar[6] = existBar[6] + newBar[6]
end

local function tryMergeLast(barType, seqId, zsetBars, timestamp, newBar)
    local topic = 'notification'
    local popedScore, popedBar
    --ZPOPMAX命令用于从有序集合中弹出（pop）分值最高的一个或多个成员，并返回这些成员及其分值
    -- 查找最后一个Bar:
    local poped = redis.call('ZPOPMAX', zsetBars)
    if #poped == 0 then
        --使用ZADD命令指定要操作的有序集合的键名和分值-成员对
        redis.call('ZADD', zsetBars, timestamp, cjson.encode(newBar))
        redis.call('PUBLISH', topic, '{"type":"bar","resolution":"'..barType..'","sequenceId":'..seqId..',"data":'.. cjson.encode(newBar) .. '}')
    else
        popedScore = poped[1]
        popedBar = poped[2]
        if popedScore == timestamp then
            -- 合并Bar并发送通知:
            merge(popedBar, newBar)
            redis.call('PUBLISH', topic, '{"type":"bar","resolution":"'..barType..'","sequenceId":'..seqId..',"data":'.. cjson.encode(newBar) .. '}')
        else
            if popedScore < timestamp then
                -- 可持久化最后一个Bar，生成新的Bar:
                redis.call('ZADD', zsetBars, timestamp, cjson.encode(newBar))
                redis.call('PUBLISH', topic, '{"type":"bar","resolution":"'..barType..'","sequenceId":'..seqId..',"data":'.. cjson.encode(newBar) .. '}')
            end
        end
    end
    return nil
end

local seqId = ARGV[1]
local KEY_BAR_SEQ = '_BarSeq_'

local zsetBars, topics, barTypeStartTimes
local openPrice, highPrice, lowPrice, closePrice, quantity
local persistBars = {}

-- 检查sequence:
local lastSeqId = redis.call('GET', KEY_BAR_SEQ)
if not lastSeqId or tonumber(seqId) > tonumber(lastSeqId) then
    zsetBars = { KEYS[1], KEYS[2], KEYS[3], KEYS[4] }
    barTypeStartTimes = { ARGV[2], ARGV[3], ARGV[4], ARGV[5] }
    openPrice = ARGV[6]
    highPrice = ARGV[7]
    lowPrice = ARGV[8]
    closePrice = ARGV[9]
    quantity = ARGV[10]

    local i, bar
    local names = { 'SEC', 'MIN', 'HOUR', 'DAY' }
    -- 检查是否可以merge
    for i = 1, 4 do
        bar = tryMergeLast(names[i], seqId, zsetBars[i], barTypeStartTimes[i], { barTypeStartTimes[i], openPrice, highPrice, lowPrice, closePrice, quantity})
        if bar then
            barTypeStartTimes[names[i]] = bar
        end
    end
    redis.call('SET', KEY_BAR_SEQ, seqId)
    return cjson.encode(persistBars)
end

redis.log(redis.LOG_WARNING, 'sequence ignored: exist seq => ' .. lastSeqId .. ' >= ' .. seqId .. ' <= new seq')

return '{}'