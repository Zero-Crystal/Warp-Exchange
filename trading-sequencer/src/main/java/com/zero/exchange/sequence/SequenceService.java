package com.zero.exchange.sequence;

import com.zero.exchange.message.event.AbstractEvent;
import com.zero.exchange.messaging.*;
import com.zero.exchange.support.LoggerSupport;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SequenceService extends LoggerSupport implements CommonErrorHandler {

    private final String GROUP_ID = "SequencerGroup";

    @Autowired
    private SequenceHandler sequenceHandler;

    @Autowired
    private MessageConvert messageConvert;

    @Autowired
    private MessagingFactory messagingFactory;

    private MessageProducer<AbstractEvent> producer;

    private Thread sequenceThread;

    private AtomicLong sequenceId;
    private boolean isRunning;
    private boolean isCrash = false;

    @PostConstruct
    public void init() {
        sequenceThread = new Thread(this::runOnSequenceThread, "async-sequence");
        sequenceThread.start();
    }

    @Override
    public void handleBatch(Exception thrownException, ConsumerRecords<?, ?> data, Consumer<?, ?> consumer, MessageListenerContainer container, Runnable invokeListener) {
        log.error("sequence thread batch error! ", thrownException);
        panic();
    }

    @PreDestroy
    public void shutdown() {
        log.info("start to shutdown sequence thread...");
        isRunning = false;
        if (sequenceThread != null) {
            sequenceThread.interrupt();
            try {
                sequenceThread.join(5000);
            } catch (InterruptedException e) {
                log.error("interrupt thread[sequence] failed, {}", e);
            }
            sequenceThread = null;
        }
    }

    private void runOnSequenceThread() {
        log.info("start sequence thread...");
        producer = messagingFactory.createMessageProducer(Messaging.Topic.TRADE, AbstractEvent.class);

        // init max sequence id
        sequenceId = new AtomicLong(sequenceHandler.getMaxSequenceId());
        log.info("current max sequence id is {}", sequenceId.get());

        // create consumer and share same group id
        log.info("create consumer for [{}]", getClass().getName());
        MessageConsumer consumer = messagingFactory.createBatchMessageListener(Messaging.Topic.SEQUENCE,
                GROUP_ID, this::processMessage, this);

        //start running
        isRunning = true;
        while (isRunning) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }

        // stop consumer
        log.info("stop consumer of [{}]...", getClass().getName());
        consumer.stop();
        System.exit(1);
    }

    /**
     * 处理上游发来的消息
     *
     * @param messages
     * */
    private synchronized void processMessage(List<AbstractEvent> messages) {
        if (!isRunning || isCrash) {
            panic();
            return;
        }
        log.info("----> start sequence");
        long startTime = System.currentTimeMillis();
        List<AbstractEvent> sequenceMessages = null;
        try {
            sequenceMessages = sequenceHandler.sequenceMessage(messageConvert, sequenceId, messages);
        } catch (Exception e) {
            log.error("exception happened in Sequence!");
            e.printStackTrace();
            shutdown();
            panic();
            throw new Error(e);
        }
        log.info(sequenceMessages.toString());
        log.info("----> sequence cost {} ms", System.currentTimeMillis() - startTime);
        sendMessage(sequenceMessages);
    }

    /**
     * 发送消息
     * */
    private void sendMessage(List<AbstractEvent> message) {
        this.producer.sendMessage(message);
    }

    /**
     * 异常退出
     * */
    private void panic() {
        isCrash = true;
        isRunning = false;
        System.exit(1);
    }
}
