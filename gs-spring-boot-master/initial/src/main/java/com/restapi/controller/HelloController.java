package com.restapi.controller;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;
import java.util.Map.Entry;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.restapi.configuration.ConstantsConfiguration;
import com.restapi.json.JsonUtil;
import com.restapi.service.KafkaService;
import com.restapi.service.RedisService;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@RestController
@RequestMapping("/")
public class HelloController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	

	@Autowired
	private RedisService redisService;
	@Autowired
    private KafkaService kafkaService;
	
	private String key = "indexingkeyqwertyuihjpoi";
	private String algorithm = "DESede";

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
	public ResponseEntity<String> getValue(@PathVariable("objectType") String objectType, @PathVariable("objectId") String objectId, @RequestHeader HttpHeaders requestHeaders) {
		String msg = "GET object from database. " + "object is: " + objectType + " key is: " + objectId;
		logger.info(msg + " START");

		String databaseID = ConstantsConfiguration.ID + objectType + "_" + objectId;
		String value = redisService.search(databaseID);
		
		if(!authorize(requestHeaders)) {
			return new ResponseEntity<String>("Token authorization failed", HttpStatus.UNAUTHORIZED);
		}

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
	public ResponseEntity<String> postValue(@PathVariable("objectType") String objectType, HttpEntity<String> input, @RequestHeader HttpHeaders requestHeaders) {
		String msg = "POST object to database. " + "object type is: " + objectType + "value is: " + input.getBody();
		logger.info(msg + " START");
		
		if(!authorize(requestHeaders)) {
			return new ResponseEntity<String>("Token authorization failed", HttpStatus.UNAUTHORIZED);
		}

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
			//enqueue the data in kafka
			kafkaService.publish(input.getBody(), "index");

		} else {
			return ResponseEntity.ok().body(" \"ERROR MESSAGE\": \"Please check your Json Object!\" ");
		}

		logger.info(msg + " End");

		return ResponseEntity.ok().body(" \"SUCCESS\": \"Insert object with key: " + databaseId + "\" ");
	}

	@RequestMapping(value = "/{objectType}/{objectId}", method = RequestMethod.DELETE)
	public ResponseEntity<String> deleteValue(@PathVariable("objectType") String objectType,
			@PathVariable("objectId") String objectId,
			@RequestHeader HttpHeaders requestHeaders) throws IOException {
		String msg = "DELETE object from database: " + "object is: " + objectType + "id is: " + objectId;
		logger.info(msg + " Start");
		
		if(!authorize(requestHeaders)) {
			return new ResponseEntity<String>("Token authorization failed", HttpStatus.UNAUTHORIZED);
		}

		String internalID = ConstantsConfiguration.ID + objectType + "_" + objectId;
		String masterObject = redisService.search(internalID);
		Set<String> childIdSet = new HashSet<String>();
		childIdSet.add(internalID);
		redisService.populateNestedData(JsonUtil.getJsonNode(masterObject), childIdSet);
		boolean deleteSuccess = false;

		for (String id : childIdSet) {
			deleteSuccess = redisService.deleteValue(id);
		}
		//Enqueue the data in kafka
        kafkaService.publish(objectId, "delete");
		logger.info(msg + " End");
		if (deleteSuccess)
			return new ResponseEntity<>(" \"SUCCESS\": \"Object and its child objects has been deleted\" ", HttpStatus.OK);

		return new ResponseEntity<>(" \"ERROR MESSAGE\": \"Nothing can be deleted, please check your object and id.\" ", HttpStatus.NOT_FOUND);
	}
	
	@RequestMapping(value = "/{object}/{key}", method = RequestMethod.PUT)
    public ResponseEntity<String> putValue(@PathVariable String object, HttpEntity<String> input,
                                           @PathVariable String key,
                                           @RequestHeader HttpHeaders requestHeaders) {

        logger.info("postValue(String object : " + object + " input : " + input.getBody() + " - Start");
        
        if(!authorize(requestHeaders)) {
			return new ResponseEntity<String>("Token authorization failed", HttpStatus.UNAUTHORIZED);
		}

        String internalID = ConstantsConfiguration.ID + object + "_" + key;
        String masterObject = redisService.search(internalID);

        if (masterObject == null) {
            return new ResponseEntity<String>("\"ERROR MESSAGE\": \"No Data Found\" ", HttpStatus.NOT_FOUND);
        }

        Set<String> childIdSet = new HashSet<String>();
        childIdSet.add(internalID);
        try {
			redisService.populateNestedData(JsonUtil.getJsonNode(masterObject), childIdSet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        boolean deleteSuccess = false;

        for (String id : childIdSet) {
            deleteSuccess = redisService.deleteValue(id);
        }

        if (deleteSuccess) {
            String planId = "";
            JsonNode rootNode = JsonUtil.validateJSON(input.getBody());
            if (null != rootNode) {
                String objectId = rootNode.get("objectId").textValue();
                planId = ConstantsConfiguration.ID + rootNode.get("objectType").textValue() + "_" + objectId;

                if (redisService.search(planId) != null) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(" \"ERROR MESSAGE\": \"A data already exisits with the id: " + planId + "\" }");
                }

                redisService.traverseInput(rootNode);
                redisService.postValue(planId, rootNode.toString());
            } else {
                return ResponseEntity.ok().body(" \"ERROR MESSAGE\": \"Check Your Json\" ");
            }

            logger.info("postValue(String object : " + object + " input : " + input.getBody() + " - End");
            kafkaService.publish(input.getBody(), "index");

//            return ResponseEntity.ok().eTag(Long.toString(masterObject.hashCode())).body("\"SUCCESS\":\"Updated data with key: " + planId + "\" ");
            return ResponseEntity.ok().body("\"SUCCESS\":\"Updated data with key: " + planId + "\" ");
        }

//        return ResponseEntity.ok().eTag(Long.toString(masterObject.hashCode())).body("\"ERROR MESSAGE\": \"Error updating the object \"");
        return ResponseEntity.ok().body("\"ERROR MESSAGE\": \"Error updating the object \"");
    }
	
	@RequestMapping(value = "/{object}/{key}", method = RequestMethod.PATCH)
    public ResponseEntity<String> patchValue(@PathVariable String object, @PathVariable String key,
                                             HttpEntity<String> input,
                                             @RequestHeader HttpHeaders requestHeaders) {

        logger.info("patchValue(String object : " + object + "String objectId : " + key + " input : " + input.getBody() + " - Start");
        
        if(!authorize(requestHeaders)) {
			return new ResponseEntity<String>("Token authorization failed", HttpStatus.UNAUTHORIZED);
		}

        String internalID = ConstantsConfiguration.ID + object + "_" + key;
        String value = redisService.search(internalID);

        if (value == null) {
            return new ResponseEntity<String>("\"ERROR MESSAGE\":\"No Data Found\" }", HttpStatus.NOT_FOUND);
        }

        JsonNode newNode = null;
        try {
            //Get the old node from redis using the object Id
            JsonNode oldNode = JsonUtil.getJsonNode(value);
            redisService.populateNestedData(oldNode, null);
            value = oldNode.toString();

            //Construct the new node from the input body
            String inputData = input.getBody();
            newNode = JsonUtil.getJsonNode(inputData);

            ArrayNode planServicesNew = (ArrayNode) newNode.get("linkedPlanServices");
            Set<JsonNode> planServicesSet = new HashSet<>();
            Set<String> objectIds = new HashSet<String>();

            planServicesNew.addAll((ArrayNode) oldNode.get("linkedPlanServices"));

            for (JsonNode node : planServicesNew) {
                Iterator<Entry<String, JsonNode>> sitr = node.fields();
                while (sitr.hasNext()) {
                    Entry<String, JsonNode> val = sitr.next();
                    if (val.getKey().equals("objectId")) {
                        if (!objectIds.contains(val.getValue().toString())) {
                            planServicesSet.add(node);
                            objectIds.add(val.getValue().toString());
                        }
                    }
                }
            }

            planServicesNew.removeAll();

            if (!planServicesSet.isEmpty())
                planServicesSet.forEach(s -> {
                    planServicesNew.add(s);
                });

            redisService.traverseInput(newNode);
            redisService.postValue(internalID, newNode.toString());

        } catch (Exception e) {
            logger.error(e.getMessage());
            return new ResponseEntity<>(" \"ERROR MESSAGE\":\"Invalid Data\" ", HttpStatus.BAD_REQUEST);
        }

        logger.info("patchValue(String object : " + object + "String objectId : " + key + " input : " + input.getBody() + " - End");
        kafkaService.publish(input.getBody(), "index");
        
//        return ResponseEntity.ok().eTag(Long.toString(newNode.hashCode())).body(" \"SUCCESS\":\"Updated data with key: " + internalID + "\" ");
        return ResponseEntity.ok().body(" \"SUCCESS\":\"Updated data with key: " + internalID + "\" ");
    }
	
	@GetMapping("/token")
	public ResponseEntity<String> createToken() {
		
		JSONObject jsonToken = new JSONObject ();
		jsonToken.put("organization", "NEU");
		jsonToken.put("issuer", "YANLYU");
		
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
		df.setTimeZone(tz);
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());            
		calendar.add(Calendar.MINUTE, 30);
		Date date = calendar.getTime();		

		jsonToken.put("expiry", df.format(date));
		String token = jsonToken.toString();
		System.out.println(token);
		
		SecretKey spec = loadKey();
		
		try {
			Cipher c = Cipher.getInstance(algorithm);
			c.init(Cipher.ENCRYPT_MODE, spec);
			byte[] encrBytes = c.doFinal(token.getBytes());
			String encoded = Base64.getEncoder().encodeToString(encrBytes);
			return new ResponseEntity<String>(encoded, HttpStatus.ACCEPTED);
			
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<String>("Token creation failed", HttpStatus.NOT_ACCEPTABLE);
		}
		
	}
	
	private SecretKey loadKey() {
		return new SecretKeySpec(key.getBytes(), algorithm);
	}
	
	private boolean authorize(HttpHeaders headers) {
		if(headers.getFirst("Authorization") == null || headers.getFirst("Authorization").length() < 7) {
			return false;
		}
		String token = headers.getFirst("Authorization").substring(7);
		byte[] decrToken = Base64.getDecoder().decode(token);
		SecretKey spec = loadKey();
		try {
			Cipher c = Cipher.getInstance(algorithm);
			c.init(Cipher.DECRYPT_MODE, spec);
			String tokenString = new String(c.doFinal(decrToken));
			JSONObject jsonToken = new JSONObject(tokenString);
			System.out.println(tokenString);
			System.out.println("Inside authorize");
			System.out.println(jsonToken.toString());
			
			String ttldateAsString = jsonToken.get("expiry").toString();
			Date currentDate = Calendar.getInstance().getTime();
			
			TimeZone tz = TimeZone.getTimeZone("UTC");
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
			formatter.setTimeZone(tz);
			
			Date ttlDate = formatter.parse(ttldateAsString);
			currentDate = formatter.parse(formatter.format(currentDate));
			
			if(currentDate.after(ttlDate)) {
				return false;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
