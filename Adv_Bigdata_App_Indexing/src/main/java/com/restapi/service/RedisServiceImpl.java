package com.restapi.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.restapi.configuration.ConstantsConfiguration;
import com.restapi.dao.RedisDao;
import com.restapi.json.JsonUtil;

@Service
public class RedisServiceImpl implements RedisService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	RedisDao<String> redisDao;

	public String search(final String key) {
		logger.info("getValue ( key : " + key + " - Start");
		return redisDao.getValue(key);
	}

	public void postValue(final String key, final String value) {
		logger.info("postValue ( key : " + key + " value : " + value + " - Start");
		redisDao.putValue(key, value);
		logger.info("postValue ( key : " + key + " value : " + value + " - End");
	}

	public boolean deleteValue(final String key) {
		logger.info("deleteValue ( key : " + key + " - Start");
		return redisDao.deleteValue(key);
	}

	@Override
	public String getHash(String internalID) {
		return redisDao.getHash(internalID);
	}

	/**
	 * Added to traverse every object and array in the inputed jsonNode
	 */
	public void traverseInput(JsonNode inputData) {
		inputData.fields().forEachRemaining(entry -> {

			// Check if the field is an array
			if (entry.getValue().isArray()) {
				ArrayList<JsonNode> innerValues = new ArrayList<JsonNode>();
				Iterator<JsonNode> iterator = entry.getValue().iterator();

				// Iterate over each element in the array
				while (iterator.hasNext()) {
					JsonNode en = (JsonNode) iterator.next();

					// Check if the objects present in this array
					if (en.isContainerNode())
						traverseInput(en);

					// Add the array object to the temporary array list
					innerValues.add(replace(en));
					traverseInput(en);
				}

				// Update the array with references created
				if (!innerValues.isEmpty()) {
					// Remove the individual array objects from the array node
					((ArrayNode) entry.getValue()).removeAll();

					// Replace the existing array objects with the references of each array element
					// created on a whole
					innerValues.forEach(s -> {
						if (s != null)
							((ArrayNode) entry.getValue()).add(s);
					});
				}
			}
			// Check if the field is an object
			else if (entry.getValue().isContainerNode()) {
				// Check if there are child objects
				traverseInput(entry.getValue());
				replaceWithId(entry);
			}
		});
	}

	/**
	 * Added to populate the nested json data by using the child node references
	 */
	public void populateNestedData(JsonNode parent, Set<String> childIdSet) {
		if (parent == null)
			return;

		// Iterate over only those elements that contain an object Id
		while (parent.toString().contains(ConstantsConfiguration.ID)) {
			parent.fields().forEachRemaining(s -> {

				// Check if the element is an array
				if (s.getValue().isArray()) {
					ArrayList<JsonNode> innerValues = new ArrayList<>();
					s.getValue().iterator().forEachRemaining(node -> {
						if (node.asText().startsWith((ConstantsConfiguration.ID)))
							innerValues.add(node);
						if (node.isContainerNode())
							populateNestedData(node, childIdSet);

						node.iterator().forEachRemaining(innerNode -> {
							if (innerNode.isContainerNode())
								populateNestedData(node, childIdSet);
						});
					});

					if (!innerValues.isEmpty()) {
						((ArrayNode) s.getValue()).removeAll();

						// Iterate through every inner value in the temp array list
						innerValues.forEach(innerValue -> {

							if (childIdSet != null)
								childIdSet.add(innerValue.asText());

							String value = redisDao.getValue(innerValue.asText());

							if (value != null)
								try {
									((ArrayNode) s.getValue()).add(JsonUtil.getJsonNode(value));
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
						});
					}
				}

				String value = s.getValue().asText();

				if (value.startsWith(ConstantsConfiguration.ID)) {
					if (childIdSet != null)
						childIdSet.add(value);

					String val = redisDao.getValue(value);
					val = val == null ? "" : val;
					JsonNode node = null;
					try {
						node = JsonUtil.getJsonNode(val);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					s.setValue(node);
				}
			});
		}
	}

	/**
	 * Added to replace the node value with reference id generated
	 * 
	 * @param entry
	 */
	private void replaceWithId(Map.Entry<String, JsonNode> entry) {
		JsonNode node = replace(entry.getValue());
		entry.setValue(node);
	}

	/**
	 * Added to persist the child value in redis before replacing the node value
	 * with reference id generated
	 * 
	 * @param entry
	 * @return
	 */
	private JsonNode replace(JsonNode entry) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		String value = entry.toString();
		String id = ConstantsConfiguration.ID + entry.get("objectType").asText() + "_" + entry.get("objectId").asText();
		JsonNode node = mapper.valueToTree(id);
		redisDao.putValue(id, value);
		return node;
	}

}
