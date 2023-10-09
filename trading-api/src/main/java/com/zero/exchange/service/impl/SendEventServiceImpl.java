package com.zero.exchange.service.impl;

import com.zero.exchange.message.event.AbstractEvent;
import com.zero.exchange.messaging.MessageProducer;
import com.zero.exchange.messaging.MessageTopic;
import com.zero.exchange.messaging.MessagingFactory;
import com.zero.exchange.service.SendEventService;
import com.zero.exchange.support.LoggerSupport;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SendEventServiceImpl extends LoggerSupport implements SendEventService {

    @Autowired
    private MessagingFactory messagingFactory;

    private MessageProducer<AbstractEvent> producer;

    @PostConstruct
    public void init() {
        producer = messagingFactory.createMessageProducer(MessageTopic.Topic.SEQUENCE, AbstractEvent.class);
    }

    @Override
    public void sendMessage(AbstractEvent event) {
        producer.sendMessage(event);
    }
}
