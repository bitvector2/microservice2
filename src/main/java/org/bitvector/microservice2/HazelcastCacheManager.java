package org.bitvector.microservice2;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.cache.MapCache;

import java.util.concurrent.ConcurrentMap;

public class HazelcastCacheManager implements CacheManager {
    private HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();

    public <K, V> Cache<K, V> getCache(String name) throws CacheException {
        ConcurrentMap<K, V> map = hazelcastInstance.getMap(name);
        return new MapCache<>(name, map);
    }
}