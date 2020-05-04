package com.api.dao;

import java.io.IOException;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class ElasticDao {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private RestHighLevelClient restHighLevelClient;

    public ElasticDao(RestHighLevelClient restHighLevelClient) {
        this.restHighLevelClient = restHighLevelClient;
    }

    public void index(String id, String document) {
        IndexRequest request = new IndexRequest("plan", "_doc", id);
        request.source(document, XContentType.JSON);
        try {
            restHighLevelClient.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public void delete(String id) {
        try {
            DeleteRequest deleteRequest = new DeleteRequest("plan", "_doc", id);
            restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }
    
    public void update(String id, String document) {
    	try {
    		UpdateRequest updateRequest = new UpdateRequest("plan", "_doc", id);
    		updateRequest.docAsUpsert(true);
            updateRequest.detectNoop(false);
    		updateRequest.fetchSource(true);
    		updateRequest.upsert(updateRequest, RequestOptions.DEFAULT);
    		updateRequest.doc(document, XContentType.JSON);
    		restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
    	}catch (IOException e) {
    		logger.error(e.getMessage());
    	}
    }
    
    public void patch(String id, String document) {
    	try {
    		UpdateRequest updateRequest = new UpdateRequest("plan", "_doc", id);
    		updateRequest.docAsUpsert(true);
            updateRequest.detectNoop(false);
    		updateRequest.fetchSource(true);
    		updateRequest.upsert(updateRequest, RequestOptions.DEFAULT);
    		updateRequest.doc(document, XContentType.JSON);
    		restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
    	}catch (IOException e) {
    		logger.error(e.getMessage());
    	}
    }
}
