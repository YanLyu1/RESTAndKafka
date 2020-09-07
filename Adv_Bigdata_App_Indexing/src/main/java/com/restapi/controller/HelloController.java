package com.restapi.controller;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.restapi.configuration.ConstantsConfiguration;
import com.restapi.json.JsonUtil;
import com.restapi.service.RedisService;

@RestController
@RequestMapping("/operation")
public class HelloController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	

	@Autowired
	private RedisService redisService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String getValue() {
    	logger.info("This is welcome page");
    	String hintInformation = "Welcome to Medical Plan. \n"
    			+ "Post: /object \n"
    			+ "Get: /object/id \n"
    			+ "Delete: /object/id";
        return hintInformation;
    }

	@RequestMapping(value = "/{objectType}/{objectId}", method = RequestMethod.GET)
	public ResponseEntity<String> getValue(@PathVariable("objectType") String objectType, @PathVariable("objectId") String objectId) {
		String msg = "GET object from database. " + "object is: " + objectType + " key is: " + objectId;
		logger.info(msg + " START");

		String databaseID = ConstantsConfiguration.ID + objectType + "_" + objectId;
		String value = redisService.search(databaseID);

		if (value == null) {
			return new ResponseEntity<String>("\"ERROR MESSAGE\": \"No Data Found. Please check your OBJECT or ID\" ", HttpStatus.NOT_FOUND);
		}

		try {
			JsonNode node = JsonUtil.getJsonNode(value);
			redisService.populateNestedData(node, null);
			value = node.toString();
			BodyBuilder bb = ResponseEntity.ok();
			ResponseEntity<String> responseBody = bb.body(value);
			return responseBody;
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		logger.info(msg + " END");

		return new ResponseEntity<>("\"ERROR MESSAGE\": \"Internal server error.\" ", HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@RequestMapping(value = "/{objectType}", method = RequestMethod.POST)
	public ResponseEntity<String> postValue(@PathVariable("objectType") String objectType, HttpEntity<String> input) {
		String msg = "POST object to database. " + "object type is: " + objectType + "value is: " + input.getBody();
		logger.info(msg + " START");

		String databaseId = "";
		//validate format and convert to JSON object
		JsonNode rootNode = JsonUtil.validateJSON(input.getBody());
		if (null != rootNode) {
			String objectId = rootNode.get("objectId").textValue();
			String rootType = rootNode.get("objectType").textValue();
			//get the id that as the database required
			databaseId = ConstantsConfiguration.ID + rootType + "_" + objectId;

			if (redisService.search(databaseId) != null) {
				return ResponseEntity.status(HttpStatus.CONFLICT)
						.body(" \"ERROR MESSAGE\": \"Object ID confliction: " + databaseId + "\" ");
			}

			redisService.traverseInput(rootNode);
			redisService.postValue(databaseId, rootNode.toString());

		} else {
			return ResponseEntity.ok().body(" \"ERROR MESSAGE\": \"Please check your Json Object!\" ");
		}

		logger.info(msg + " End");

		return ResponseEntity.ok().body(" \"SUCCESS\": \"Insert object with key: " + databaseId + "\" ");
	}

	@RequestMapping(value = "/{objectType}/{objectId}", method = RequestMethod.DELETE)
	public ResponseEntity<String> deleteValue(@PathVariable("objectType") String objectType,
			@PathVariable("objectId") String objectId) throws IOException {
		String msg = "DELETE object from database: " + "object is: " + objectType + "id is: " + objectId;
		logger.info(msg + " Start");

		String internalID = ConstantsConfiguration.ID + objectType + "_" + objectId;
		String masterObject = redisService.search(internalID);
		Set<String> childIdSet = new HashSet<String>();
		childIdSet.add(internalID);
		redisService.populateNestedData(JsonUtil.getJsonNode(masterObject), childIdSet);
		boolean deleteSuccess = false;

		for (String id : childIdSet) {
			deleteSuccess = redisService.deleteValue(id);
		}

		logger.info(msg + " End");
		if (deleteSuccess)
			return new ResponseEntity<>(" \"SUCCESS\": \"Object and its child objects has been deleted\" ", HttpStatus.OK);

		return new ResponseEntity<>(" \"ERROR MESSAGE\": \"Nothing can be deleted, please check your object and id.\" ", HttpStatus.NOT_FOUND);
	}

}
