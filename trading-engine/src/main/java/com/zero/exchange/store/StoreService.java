package com.zero.exchange.store;

import com.zero.exchange.message.event.AbstractEvent;
import com.zero.exchange.model.support.EntitySupport;

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
