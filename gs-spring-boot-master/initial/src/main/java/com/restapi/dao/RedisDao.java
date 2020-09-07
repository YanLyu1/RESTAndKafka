package com.restapi.dao;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface RedisDao<T> {

	public void putMap(String redisKey, Object key, T data);

	public T getMapAsSingleEntry(String redisKey, Object key);

	public Map<Object, T> getMapAsAll(String redisKey);

	public void putValue(String key, T value);

	public void putValueWithExpireTime(String key, T value, long timeout, TimeUnit unit);

	public T getValue(String key);

	public boolean deleteValue(String key);

	public void setExpire(String key, long timeout, TimeUnit unit);

	public T getHash(String input);

}
