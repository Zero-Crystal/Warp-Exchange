package com.zero.exchange.store;

import com.zero.exchange.message.event.AbstractEvent;
import com.zero.exchange.messaging.MessageConvert;
import com.zero.exchange.entity.support.EntitySupport;
import com.zero.exchange.entity.trade.EventEntity;
import com.zero.exchange.support.AbstractDbService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
public class StoreServiceImpl extends AbstractDbService implements StoreService {

    @Autowired
    private MessageConvert messageConvert;

    @Override
    public List<AbstractEvent> loadEventsFromDB(long lastSequenceId) {
        List<EventEntity> eventList = db.from(EventEntity.class).where("sequenceId > ", lastSequenceId)
                .orderBy("sequenceId").limit(1000).list();
        return eventList.stream().map(e -> (AbstractEvent) messageConvert.deserialize(e.data)).collect(Collectors.toList());
    }

    @Override
    public void insertIgnoreList(List<? extends EntitySupport> list) {
        db.insetIgnore(list);
    }
}
