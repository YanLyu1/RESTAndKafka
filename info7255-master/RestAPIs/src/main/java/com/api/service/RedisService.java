package com.api.service;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

public interface RedisService {

	public String getValue(final String key);

	public void postValue(final String key, final String value);
	
	public void traverseInput(JsonNode inputData);
	
	public void populateNestedData(JsonNode parent, Set<String> childIdSet);

	public boolean deleteValue(final String key);

	public String getHash(String internalID);
	
}
