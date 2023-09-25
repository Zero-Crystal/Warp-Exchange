package com.zero.exchange.model.messaging;

import com.zero.exchange.message.AbstractMessage;
import com.zero.exchange.support.LoggerSupport;
import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpoint;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.BatchMessageListener;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.TopicPartitionOffset;
import org.springframework.kafka.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/**
 * 接收/发送消息的入口
 * */
@Component
public class MessagingFactory extends LoggerSupport {

    @Autowired
    private MessageConvert messageConvert;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ConcurrentKafkaListenerContainerFactory<String, String> listenerContainerFactory;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @PostConstruct
    public void init() throws ExecutionException, InterruptedException {
        log.info("init kafka admin...");
        try(AdminClient client = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            Set<String> allTopics = client.listTopics().names().get();
            List<NewTopic> newTopicList = new ArrayList<>();
            for (MessageTopic.Topic topic : MessageTopic.Topic.values()) {
                if (!allTopics.contains(topic.name())) {
                    newTopicList.add(new NewTopic(topic.name(), topic.getPartition(), (short) 1));
                }
            }
            if (newTopicList.size() > 0) {
                client.createTopics(newTopicList);
                newTopicList.forEach(newTopic -> {
                    log.warn("auto create topic[{}] when init MessagingFactory", newTopic.name());
                });
            }
        }
        log.info("init kafka is ok");
    }

    public <T extends AbstractMessage> MessageProducer<T> createMessageProducer(MessageTopic.Topic topic, Class<T> messageClass) {
        log.info("init message producer of topic[{}]...", topic);
        final String topicName = topic.name();
        return new MessageProducer<T>() {
            @Override
            public void sendMessage(T message) {
                kafkaTemplate.send(topicName, messageConvert.serialize(message));
            }
        };
    }

    public <T extends AbstractMessage> MessageConsumer createBatchMessageListener(MessageTopic.Topic topic, String groupId, BatchHandlerMessage<T> batchHandlerMessage) {
        return createBatchMessageListener(topic, groupId, batchHandlerMessage, null);
    }

    public <T extends AbstractMessage> MessageConsumer createBatchMessageListener(MessageTopic.Topic topic, String groupId, BatchHandlerMessage<T> batchHandlerMessage,
                                                                                  CommonErrorHandler errorHandler) {
        log.info("init message consumer of topic[{}]...", topic);
        ConcurrentMessageListenerContainer<String, String> listenerContainer = listenerContainerFactory.createListenerContainer(new KafkaListenerEndpointAdapter() {
            @Override
            public String getGroupId() {
                return groupId;
            }

            @Override
            public Collection<String> getTopics() {
                return List.of(topic.name());
            }
        });
        listenerContainer.setupMessageListener(new BatchMessageListener<String, String>() {
            @Override
            public void onMessage(List<ConsumerRecord<String, String>> consumerRecords) {
                List<T> messages = new ArrayList<>(consumerRecords.size());
                for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
                    AbstractMessage message = messageConvert.deserialize(consumerRecord.value());
                    messages.add((T) message);
                }
                batchHandlerMessage.processMessage(messages);
            }
        });
        if (errorHandler != null) {
            listenerContainer.setCommonErrorHandler(errorHandler);
        }
        listenerContainer.start();
        return listenerContainer::stop;
    }
}

class KafkaListenerEndpointAdapter implements KafkaListenerEndpoint {

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getGroupId() {
        return null;
    }

    @Override
    public String getGroup() {
        return null;
    }

    @Override
    public Collection<String> getTopics() {
        return null;
    }

    @Override
    public TopicPartitionOffset[] getTopicPartitionsToAssign() {
        return new TopicPartitionOffset[0];
    }

    @Override
    public Pattern getTopicPattern() {
        return null;
    }

    @Override
    public String getClientIdPrefix() {
        return null;
    }

    @Override
    public Integer getConcurrency() {
        return null;
    }

    @Override
    public Boolean getAutoStartup() {
        return null;
    }

    @Override
    public void setupListenerContainer(MessageListenerContainer messageListenerContainer, MessageConverter messageConverter) {

    }

    @Override
    public boolean isSplitIterables() {
        return false;
    }
}
