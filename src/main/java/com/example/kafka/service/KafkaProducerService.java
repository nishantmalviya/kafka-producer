package com.example.kafka.service;

import com.example.kafka.config.KafkaConfig;
import com.example.kafka.dto.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private KafkaConfig kafkaConfig;

    /**
     * Send message to Kafka topic asynchronously with timing measurement
     * @param key Message key
     * @param messageContent Message content
     * @return CompletableFuture with MessageResponse containing timing information
     */
    public CompletableFuture<MessageResponse> sendMessage(String key, String messageContent) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Sending message to topic '{}' with key '{}'. Message: {}", 
                kafkaConfig.getTopicName(), key, messageContent);

            // Build the Kafka message
            Message<String> message = MessageBuilder
                .withPayload(messageContent)
                .setHeader(KafkaHeaders.TOPIC, kafkaConfig.getTopicName())
                .setHeader("kafka_messageKey", key)
                .build();

            // Send message and handle callback using new Spring Boot 3.1 API
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.executeInTransaction(ops -> 
                ops.send(message)
            );

            return future.handle((result, ex) -> {
                long endTime = System.currentTimeMillis();
                long timeTaken = endTime - startTime;
                
                if (ex == null && result != null) {
                    // Success
                    int partition = result.getRecordMetadata().partition();
                    long offset = result.getRecordMetadata().offset();
                    String topic = result.getRecordMetadata().topic();
                    long timestamp = result.getRecordMetadata().timestamp();
                    
                    logger.info("Message sent successfully to topic '{}' in {} ms. " +
                            "Partition: {}, Offset: {}, Timestamp: {}", 
                        topic, timeTaken, partition, offset, timestamp);

                    return MessageResponse.builder()
                        .status("SUCCESS")
                        .message("Message sent successfully")
                        .timeTakenMs(timeTaken)
                        .offset(offset)
                        .partition(partition)
                        .topic(topic)
                        .timestamp(timestamp)
                        .build();
                } else if (ex != null) {
                    // Error
                    logger.error("Failed to send message to topic '{}' after {} ms. Error: {}", 
                        kafkaConfig.getTopicName(), timeTaken, ex.getMessage(), ex);

                    return MessageResponse.builder()
                        .status("FAILED")
                        .message("Failed to send message: " + ex.getMessage())
                        .timeTakenMs(timeTaken)
                        .build();
                } else {
                    // Should not happen
                    logger.error("Unknown error occurred after {} ms", timeTaken);
                    return MessageResponse.builder()
                        .status("ERROR")
                        .message("Unknown error occurred")
                        .timeTakenMs(timeTaken)
                        .build();
                }
            });

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long timeTaken = endTime - startTime;
            
            logger.error("Exception while sending message after {} ms. Error: {}", 
                timeTaken, e.getMessage(), e);

            return CompletableFuture.completedFuture(MessageResponse.builder()
                .status("ERROR")
                .message("Error: " + e.getMessage())
                .timeTakenMs(timeTaken)
                .build());
        }
    }

}
