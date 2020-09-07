/**
 * @Author Aditya Kelkar
 */


package com.api.service;

public interface KafkaService {
    public void publish(String message, String operation);
}
