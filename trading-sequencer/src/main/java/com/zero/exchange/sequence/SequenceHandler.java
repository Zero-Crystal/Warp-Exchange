package com.zero.exchange.sequence;

import com.zero.exchange.message.event.AbstractEvent;
import com.zero.exchange.messaging.MessageConvert;
import com.zero.exchange.model.trade.EventEntity;
import com.zero.exchange.model.trade.UniqueEventEntity;
import com.zero.exchange.support.AbstractDbService;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Transactional(rollbackFor = Throwable.class)
public class SequenceHandler extends AbstractDbService {

    private long lastTimeStamp = 0;

    /**
     * 给上游消息定序，并返回定序后的消息
     *
     * @param messageConvert
     * @param sequence
     * @param messages
     * @return List<AbstractEvent>
     * */
    public List<AbstractEvent> sequenceMessage(final MessageConvert messageConvert, final AtomicLong sequence,
                                               final List<AbstractEvent> messages) throws Exception {
        long ct = System.currentTimeMillis();
        if (ct < lastTimeStamp) {
            log.warn("[Sequence] current time {} is turned back from {}!", ct, this.lastTimeStamp);
        } else {
            this.lastTimeStamp = ct;
        }
        // message去重
        List<UniqueEventEntity> uniqueEventEntities = null;
        Set<String> uniqueKeys = null;
        List<AbstractEvent> sequenceMessages = new ArrayList<>();
        List<EventEntity> eventEntities = new ArrayList<>();
        for (AbstractEvent message : messages) {
            UniqueEventEntity uniqueEvent = null;
            String uniqueId = message.uniqueId;
            // 去重
            if (Strings.isNotEmpty(uniqueId)) {
                if ((uniqueKeys != null && uniqueKeys.contains(uniqueId))
                        || db.fetch(UniqueEventEntity.class, uniqueId) != null) {
                    continue;
                }
                uniqueEvent = new UniqueEventEntity();
                uniqueEvent.uniqueId = uniqueId;
                uniqueEvent.createdAt = message.createAt;
                if (uniqueEventEntities == null) {
                    uniqueEventEntities = new ArrayList<>();
                }
                uniqueEventEntities.add(uniqueEvent);
                if (uniqueKeys == null) {
                    uniqueKeys = new HashSet<>();
                }
                uniqueKeys.add(uniqueId);
                uniqueEvent.sequenceId = sequence.get();
            }
            final long previousId = sequence.get();
            final long currentId = sequence.incrementAndGet();

            if (uniqueEvent != null) {
                uniqueEvent.sequenceId = currentId;
            }

            message.previousId = previousId;
            message.sequenceId = currentId;
            message.createAt = this.lastTimeStamp;
            sequenceMessages.add(message);

            EventEntity event = new EventEntity();
            event.sequenceId = currentId;
            event.previousId = previousId;
            event.createAt = this.lastTimeStamp;
            event.data = messageConvert.serialize(message);
            eventEntities.add(event);
        }

        if (uniqueEventEntities != null) {
            db.insert(uniqueEventEntities);
        }

        if (!eventEntities.isEmpty()) {
            db.insert(eventEntities);
        }

        return sequenceMessages;
    }

    /**
     * 获取最新的定序id
     *
     * @return long
     * */
    public long getMaxSequenceId() {
        System.out.println(db.exportDDL());
        EventEntity last = db.from(EventEntity.class).orderBy("sequenceId").desc().first();
        if (last == null) {
            log.info("can not find the max sequence Id");
            return 0;
        }
        this.lastTimeStamp = last.createAt;
        log.info("find max sequenceId = {}, last timestamp = {}", last.sequenceId, this.lastTimeStamp);
        return last.sequenceId;
    }
}
