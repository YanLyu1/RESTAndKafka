package com.api.controller;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.util.Set;
import java.util.TimeZone;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.api.constant.CommonConstants;
import com.api.service.RedisService;
import com.api.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

@RestController
@RequestMapping("/")
public class RestApiController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RedisService redisService;
    
    private String key = "abcdefghijklmnopqrstuvwx";
	private String algorithm = "DESede";

   

//    @RequestMapping(value = "/", method = RequestMethod.GET)
//    public String getValue() {
//        return "Hello World!";
//    }

    @RequestMapping(value = "/{object}/{key}", method = RequestMethod.GET)
    public ResponseEntity<String> getValue(@PathVariable String object, @PathVariable String key, @RequestHeader HttpHeaders requestHeaders) {
        logger.info("getValue(String object : " + object + " key : " + key + " - Start");

        String internalID = CommonConstants.ID + object + "_" + key;
        String value = redisService.getValue(internalID);
        
        if(!authorize(requestHeaders)) {
			return new ResponseEntity<String>("Token authorization failed", HttpStatus.NOT_ACCEPTABLE);
		}
        
        if (value == null) {
            return new ResponseEntity<String>("{\"message\": \"No Data Found\" }", HttpStatus.NOT_FOUND);
        }

        try {
            JsonNode node = JsonUtil.nodeFromString(value);
            redisService.populateNestedData(node, null);
            value = node.toString();
            return ResponseEntity.ok().body(value);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        logger.info("getValue(String object : " + object + " key : " + key + " - End");

        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @RequestMapping(value = "/{object}", method = RequestMethod.POST)
    public ResponseEntity<String> postValue(@PathVariable String object, HttpEntity<String> input, @RequestHeader HttpHeaders requestHeaders) {
    	
        logger.info("postValue(String object : " + object + " input : " + input.getBody() + " - Start");
        
        if(!authorize(requestHeaders)) {
			return new ResponseEntity<String>("Token authorization failed", HttpStatus.NOT_ACCEPTABLE);
		}
        
        String planId = "";
        JsonNode rootNode = JsonUtil.validateAgainstSchema(input.getBody());
        if (null != rootNode) {
            String objectId = rootNode.get("objectId").textValue();
            planId = CommonConstants.ID + rootNode.get("objectType").textValue() + "_" + objectId;

            if (redisService.getValue(planId) != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(" {\"message\": \"A resource already exisits with the id: " + planId + "\" }");
            }

            redisService.traverseInput(rootNode);
            redisService.postValue(planId, rootNode.toString());
           
            
        } else {
            return ResponseEntity.ok().body(" {\"message\": \"Error validating the input data\" }");
        }

        logger.info("postValue(String object : " + object + " input : " + input.getBody() + " - End");

        return ResponseEntity.ok().body(" {\"message\": \"Created data with key: " + planId + "\" }");
    }

    @RequestMapping(value = "/{object}/{key}", method = RequestMethod.PUT)
    public ResponseEntity<String> putValue(@PathVariable String object, HttpEntity<String> input,
                                           @PathVariable String key,
                                           @RequestHeader HttpHeaders requestHeaders) {

        logger.info("postValue(String object : " + object + " input : " + input.getBody() + " - Start");

        if(!authorize(requestHeaders)) {
			return new ResponseEntity<String>("Token authorization failed", HttpStatus.NOT_ACCEPTABLE);
		}
        
        String internalID = CommonConstants.ID + object + "_" + key;
        String masterObject = redisService.getValue(internalID);

        if (masterObject == null) {
            return new ResponseEntity<String>("{\"message\": \"No Data Found\" }", HttpStatus.NOT_FOUND);
        }

        Set<String> childIdSet = new HashSet<String>();
        childIdSet.add(internalID);
        redisService.populateNestedData(JsonUtil.nodeFromString(masterObject), childIdSet);
        boolean deleteSuccess = false;

        for (String id : childIdSet) {
            deleteSuccess = redisService.deleteValue(id);
        }

        if (deleteSuccess) {
            String planId = "";
            JsonNode rootNode = JsonUtil.validateAgainstSchema(input.getBody());
            if (null != rootNode) {
                String objectId = rootNode.get("objectId").textValue();
                planId = CommonConstants.ID + rootNode.get("objectType").textValue() + "_" + objectId;

                if (redisService.getValue(planId) != null) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(" {\"message\": \"A resource already exisits with the id: " + planId + "\" }");
                }

                redisService.traverseInput(rootNode);
                redisService.postValue(planId, rootNode.toString());
            } else {
                return ResponseEntity.ok().body(" {\"message\": \"Error validating the input data\" }");
            }

            logger.info("postValue(String object : " + object + " input : " + input.getBody() + " - End");

            return ResponseEntity.ok().body(" {\"message\": \"Updated data with key: " + planId + "\" }");
        }

        return ResponseEntity.ok().body(" {\"message\": \"Error updating the object }");
    }

    @RequestMapping(value = "/{object}/{key}", method = RequestMethod.PATCH)
    public ResponseEntity<String> patchValue(@PathVariable String object, @PathVariable String key,
                                             HttpEntity<String> input,
                                             @RequestHeader HttpHeaders requestHeaders) {

        logger.info("patchValue(String object : " + object + "String objectId : " + key + " input : " + input.getBody() + " - Start");

        if(!authorize(requestHeaders)) {
			return new ResponseEntity<String>("Token authorization failed", HttpStatus.NON_AUTHORITATIVE_INFORMATION);
		}
        
        String internalID = CommonConstants.ID + object + "_" + key;
        String value = redisService.getValue(internalID);

        if (value == null) {
            return new ResponseEntity<String>("{\"message\": \"No Data Found\" }", HttpStatus.NOT_FOUND);
        }

        try {
            //Get the old node from redis using the object Id
            JsonNode oldNode = JsonUtil.nodeFromString(value);
            redisService.populateNestedData(oldNode, null);
            value = oldNode.toString();

            //Construct the new node from the input body
            String inputData = input.getBody();
            JsonNode newNode = JsonUtil.nodeFromString(inputData);

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
            return new ResponseEntity<>(" {\"message\": \"Invalid Data\" }", HttpStatus.BAD_REQUEST);
        }

        logger.info("patchValue(String object : " + object + "String objectId : " + key + " input : " + input.getBody() + " - End");

        return ResponseEntity.ok().body(" {\"message\": \"Updated data with key: " + internalID + "\" }");
    }

    @RequestMapping(value = "/{object}/{objectId}", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteValue(@PathVariable("object") String object,
                                              @PathVariable("objectId") String objectId,
                                              @RequestHeader HttpHeaders requestHeaders) {

        logger.info("deleteValue(String object : " + object + " objectId : " + objectId + " - Start");

        if(!authorize(requestHeaders)) {
			return new ResponseEntity<String>("Token authorization failed", HttpStatus.NOT_ACCEPTABLE);
		}
        
        String internalID = CommonConstants.ID + object + "_" + objectId;
        String masterObject = redisService.getValue(internalID);
        Set<String> childIdSet = new HashSet<String>();
        childIdSet.add(internalID);
        redisService.populateNestedData(JsonUtil.nodeFromString(masterObject), childIdSet);
        boolean deleteSuccess = false;

        for (String id : childIdSet) {
            deleteSuccess = redisService.deleteValue(id);
        }
        logger.info("deleteValue(String object : " + object + " objectId : " + objectId + " - End");
        if (deleteSuccess)
            return new ResponseEntity<>(" {\"message\": \"Deleted\" }", HttpStatus.OK);

        return new ResponseEntity<>(" {\"message\": \"There is nothing to delete\" }", HttpStatus.NOT_FOUND);
    }
    
    @GetMapping("/Token")
	public ResponseEntity<String> createToken() {
		
		JSONObject jsonToken = new JSONObject ();
		jsonToken.put("organization", "Northeastern");
		jsonToken.put("issuer", "Kunal");
		
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
