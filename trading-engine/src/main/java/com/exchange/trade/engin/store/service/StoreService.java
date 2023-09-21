package com.exchange.trade.engin.store.service;

import com.exchange.common.message.event.AbstractEvent;
import com.exchange.common.model.support.EntitySupport;

import java.util.List;

public interface StoreService {

    /**
     * 获取历史订阅消息
     * @return List<AbstractEvent>
     * */
    List<AbstractEvent> loadEventsFromDB(long lastSequenceId);

    /**
     * 批量插入
     * @param list List<? extends EntitySupport>
     * */
    void insertIgnoreList(List<? extends EntitySupport> list);
}
