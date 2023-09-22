package com.zero.exchange.model.messaging;

import com.zero.exchange.message.AbstractMessage;
import com.zero.exchange.util.JsonUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class MessageConvert {

    private final String messagePackage = AbstractMessage.class.getPackageName();

    private final String SEP = "#";

    /**
     * key: class name, value: class
     * */
    final Map<String, Class<? extends AbstractMessage>> messageTypes = new HashMap<>();

    @PostConstruct
    public void Init() {
        ClassPathScanningCandidateComponentProvider classPathScanProvider = new ClassPathScanningCandidateComponentProvider(false);
        classPathScanProvider.addIncludeFilter(new TypeFilter() {
            @Override
            public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
                String clasName = metadataReader.getClassMetadata().getClassName();
                Class messageClass = null;
                try {
                    messageClass = Class.forName(clasName);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
                return AbstractMessage.class.isAssignableFrom(messageClass);
            }
        });
        Set<BeanDefinition> beanDefinitionSet = classPathScanProvider.findCandidateComponents(messagePackage);
        for (BeanDefinition bean : beanDefinitionSet) {
            try {
                Class clazz = Class.forName(bean.getBeanClassName());
                if (messageTypes.put(clazz.getName(), clazz) != null) {
                    throw new RuntimeException("message class 重复![" + clazz.getName() + "]");
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * message convert to string
     * */
    public String serialize(AbstractMessage message) {
        String messageType = message.getClass().getName();
        String jsonData = JsonUtil.writeJson(messageType);
        return messageType + SEP + jsonData;
    }

    /**
     * string convert to message
     * */
    public AbstractMessage deserialize(String data) {
        int pos = data.indexOf(SEP);
        if (pos == -1) {
            throw new RuntimeException("Unable to handler message with data: " + data);
        }
        String messageType = data.substring(0, pos);
        Class<? extends AbstractMessage> clazz = messageTypes.get(messageType);
        if (clazz == null) {
            throw new RuntimeException("Unable to handler message with type: " + messageType);
        }
        String json = data.substring(pos + 1);
        return JsonUtil.readJson(json, clazz);
    }

    /**
     * batch convert strings to messages
     * */
    public List<AbstractMessage> deserialize(List<String> dataList) {
        List<AbstractMessage> abstractMessages = new ArrayList<>();
        for (String data : dataList) {
            abstractMessages.add(deserialize(data));
        }
        return abstractMessages;
    }
}
