package com.restapi.service;

public interface KafkaService {
    public void publish(String message, String operation);
}
