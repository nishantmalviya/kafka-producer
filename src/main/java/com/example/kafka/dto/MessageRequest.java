package com.example.kafka.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MessageRequest {
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("key")
    private String key;

    public MessageRequest() {}

    public MessageRequest(String message, String key) {
        this.message = message;
        this.key = key;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    @Override
    public String toString() {
        return "MessageRequest{" +
                "message='" + message + '\'' +
                ", key='" + key + '\'' +
                '}';
    }

}
