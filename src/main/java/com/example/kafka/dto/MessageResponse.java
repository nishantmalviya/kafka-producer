package com.example.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MessageResponse {
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("timeTakenMs")
    private long timeTakenMs;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    @JsonProperty("offset")
    private long offset;
    
    @JsonProperty("partition")
    private int partition;
    
    @JsonProperty("topic")
    private String topic;

    public MessageResponse() {}

    public MessageResponse(String status, String message, long timeTakenMs, long timestamp, 
                          long offset, int partition, String topic) {
        this.status = status;
        this.message = message;
        this.timeTakenMs = timeTakenMs;
        this.timestamp = timestamp;
        this.offset = offset;
        this.partition = partition;
        this.topic = topic;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimeTakenMs() { return timeTakenMs; }
    public void setTimeTakenMs(long timeTakenMs) { this.timeTakenMs = timeTakenMs; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getOffset() { return offset; }
    public void setOffset(long offset) { this.offset = offset; }

    public int getPartition() { return partition; }
    public void setPartition(int partition) { this.partition = partition; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public static class Builder {
        private String status;
        private String message;
        private long timeTakenMs;
        private long timestamp;
        private long offset;
        private int partition;
        private String topic;

        public Builder status(String status) { this.status = status; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder timeTakenMs(long timeTakenMs) { this.timeTakenMs = timeTakenMs; return this; }
        public Builder timestamp(long timestamp) { this.timestamp = timestamp; return this; }
        public Builder offset(long offset) { this.offset = offset; return this; }
        public Builder partition(int partition) { this.partition = partition; return this; }
        public Builder topic(String topic) { this.topic = topic; return this; }

        public MessageResponse build() {
            return new MessageResponse(status, message, timeTakenMs, timestamp, offset, partition, topic);
        }
    }

}
