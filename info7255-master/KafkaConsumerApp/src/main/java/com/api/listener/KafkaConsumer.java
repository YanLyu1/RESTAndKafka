package com.api.listener;

import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.api.dao.ElasticDao;
import com.api.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class KafkaConsumer {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ElasticDao elasticDao;

    @KafkaListener(topics = "bigdataindexing", groupId = "group_id")
    public void consume(ConsumerRecord<String, String> record) throws ExecutionException, InterruptedException {
        logger.info("Consumed Message - {} ", record);
        System.out.println("Consumed message: Key" + record.key().toString() + " Value : " + record.value().toString());

        // Send Message to elastic search
        if (record.key().toString().equals("index")) {
            JsonNode rootNode = JsonUtil.validateAgainstSchema(record.value().toString());
            String objectId = rootNode.get("objectId").textValue();
            elasticDao.index(objectId, record.value().toString());
        } else if (record.key().toString().equals("delete")) {
            elasticDao.delete(record.value().toString());
        }
    }

}