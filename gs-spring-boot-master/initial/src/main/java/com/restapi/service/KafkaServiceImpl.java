package com.restapi.service;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Service
public class KafkaServiceImpl implements KafkaService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${spring.kafka.topic.name}")
    private String topicName;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;


    /**
     * Publish message on Kafka topic
     *
     * @param message
     */
    public void publish(String message, String operation) {

        //ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(topicName, message);
        ProducerRecord<String, String> record = new ProducerRecord<String, String>(topicName, operation, message);
        ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);

        future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {

            @Override
            public void onSuccess(SendResult<String, String> result) {
                logger.info("Sent message=[" + message +
                        "] with offset=[" + result.getRecordMetadata().offset() + "]");
            }

            @Override
            public void onFailure(Throwable ex) {
                logger.error("Unable to send message=["
                        + message + "] due to : " + ex.getMessage());
            }
        });
    }
}
