package com.zero.exchange.store;

import com.zero.exchange.db.DbTemplate;
import com.zero.exchange.message.event.AbstractEvent;
import com.zero.exchange.model.messaging.MessageConvert;
import com.zero.exchange.model.support.EntitySupport;
import com.zero.exchange.model.trade.EventEntity;
import com.zero.exchange.support.LoggerSupport;
import com.zero.exchange.store.StoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Transactional
@Service
public class StoreServiceImpl extends LoggerSupport implements StoreService {

    @Autowired
    private MessageConvert messageConvert;

    @Autowired
    private DbTemplate dbTemplate;

    @Override
    public List<AbstractEvent> loadEventsFromDB(long lastSequenceId) {
        List<EventEntity> eventList = dbTemplate.from(EventEntity.class).where("sequenceId > ", lastSequenceId)
                .orderBy("sequenceId").limit(1000).list();
        return eventList.stream().map(e -> (AbstractEvent) messageConvert.deserialize(e.data)).collect(Collectors.toList());
    }

    @Override
    public void insertIgnoreList(List<? extends EntitySupport> list) {
        dbTemplate.insetIgnore(list);
    }
}
