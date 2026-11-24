package com.example.kafka.controller;

import com.example.kafka.dto.MessageRequest;
import com.example.kafka.dto.MessageResponse;
import com.example.kafka.service.KafkaProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/kafka")
public class KafkaProducerController {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerController.class);

    @Autowired
    private KafkaProducerService kafkaProducerService;

    /**
     * API endpoint to post a message to Kafka topic
     * @param messageRequest Request containing message and optional key
     * @return ResponseEntity with MessageResponse containing timing information
     */
    @PostMapping("/publish")
    public CompletableFuture<ResponseEntity<MessageResponse>> publishMessage(
            @RequestBody MessageRequest messageRequest) {
        
        logger.info("Received request to publish message: {}", messageRequest);
        
        if (messageRequest.getMessage() == null || messageRequest.getMessage().isEmpty()) {
            MessageResponse errorResponse = MessageResponse.builder()
                .status("INVALID_REQUEST")
                .message("Message cannot be empty")
                .timeTakenMs(0)
                .build();
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(errorResponse)
            );
        }

        // Use provided key or generate one
        String key = messageRequest.getKey() != null && !messageRequest.getKey().isEmpty() 
            ? messageRequest.getKey() 
            : "key-" + System.currentTimeMillis();

        return kafkaProducerService.sendMessage(key, messageRequest.getMessage())
            .thenApply(response -> {
                if ("SUCCESS".equals(response.getStatus())) {
                    return ResponseEntity.ok(response);
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
                }
            });
    }

    /**
     * Health check endpoint
     * @return Response indicating the service is running
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Kafka Producer Service is running");
    }

    /**
     * API endpoint to post multiple messages in a batch
     * @param messages List of messages to publish
     * @return ResponseEntity with batch status
     */
    @PostMapping("/publish-batch")
    public CompletableFuture<ResponseEntity<?>> publishBatch(
            @RequestBody java.util.List<MessageRequest> messages) {
        
        logger.info("Received batch request with {} messages", messages.size());
        
        java.util.List<CompletableFuture<MessageResponse>> futures = new java.util.ArrayList<>();
        
        for (MessageRequest msg : messages) {
            String key = msg.getKey() != null && !msg.getKey().isEmpty() 
                ? msg.getKey() 
                : "key-" + System.currentTimeMillis();
            futures.add(kafkaProducerService.sendMessage(key, msg.getMessage()));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                java.util.List<MessageResponse> responses = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
                
                long totalTime = responses.stream()
                    .mapToLong(MessageResponse::getTimeTakenMs)
                    .sum();
                
                return ResponseEntity.ok(java.util.Map.of(
                    "totalMessages", responses.size(),
                    "totalTimeTakenMs", totalTime,
                    "responses", responses
                ));
            });
    }

}
