package com.example.kafka.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${kafka.topic.name}")
    private String topicName;

    public String getTopicName() {
        return topicName;
    }

}
