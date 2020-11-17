package com.api.dao;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
//import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import com.api.constant.CommonConstants;

@Repository
public class RedisDaoImpl<T> implements RedisDao<T> {

	private RedisTemplate<String, T> redisTemplate;
	private HashOperations<String, Object, T> hashOperation;
	// private ListOperations<String,T> listOperation;
	private ValueOperations<String, T> valueOperations;

	@Autowired
	RedisDaoImpl(RedisTemplate<String, T> redisTemplate) {
		this.redisTemplate = redisTemplate;
		this.hashOperation = redisTemplate.opsForHash();
		// this.listOperation = redisTemplate.opsForList();
		this.valueOperations = redisTemplate.opsForValue();
	}

	@SuppressWarnings("unchecked")
	public void putMap(String redisKey, Object key, Object data) {
		hashOperation.put(redisKey, key, (T) data);
	}

	public T getMapAsSingleEntry(String redisKey, Object key) {
		return hashOperation.get(redisKey, key);
	}

	public Map<Object, T> getMapAsAll(String redisKey) {
		return hashOperation.entries(redisKey);
	}

	public T getHash(String input) {
		return valueOperations.get(input + CommonConstants.HASH_MARK);
	}

	@SuppressWarnings("unchecked")
	public void putValue(String key, Object value) {
		valueOperations.set(key, (T) value);
		valueOperations.set(key + CommonConstants.HASH_MARK, (T) String.valueOf(System.currentTimeMillis()));
	}

	@SuppressWarnings("unchecked")
	public void putValueWithExpireTime(String key, Object value, long timeout, TimeUnit unit) {
		valueOperations.set(key, (T) value, timeout, unit);
	}

	public T getValue(String key) {
		return valueOperations.get(key);
	}

	public void setExpire(String key, long timeout, TimeUnit unit) {
		redisTemplate.expire(key, timeout, unit);
	}

	public boolean deleteValue(String key) {
		return redisTemplate.delete(key);
	}

}
