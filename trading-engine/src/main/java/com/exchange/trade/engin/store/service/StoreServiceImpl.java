package com.exchange.trade.engin.store.service;

import com.exchange.common.db.DbTemplate;
import com.exchange.common.message.event.AbstractEvent;
import com.exchange.common.model.messaging.MessageConvert;
import com.exchange.common.model.support.EntitySupport;
import com.exchange.common.model.trade.EventEntity;
import com.exchange.common.support.LoggerSupport;
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
