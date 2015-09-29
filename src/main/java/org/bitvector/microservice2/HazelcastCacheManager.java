package org.bitvector.microservice2;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.apache.shiro.ShiroException;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.cache.MapCache;
import org.apache.shiro.util.Destroyable;
import org.apache.shiro.util.Initializable;

import java.util.concurrent.ConcurrentMap;

public class HazelcastCacheManager implements CacheManager, Initializable, Destroyable {
    private boolean implicitlyCreated = false;
    private HazelcastInstance hazelcastInstance;
    private Config config;

    public <K, V> Cache<K, V> getCache(String name) throws CacheException {
        ConcurrentMap<K, V> map = ensureHazelcastInstance().getMap(name);
        return new MapCache<>(name, map);
    }

    protected HazelcastInstance ensureHazelcastInstance() {
        init();
        return this.hazelcastInstance;
    }

    public void init() throws ShiroException {
        if (this.hazelcastInstance == null) {
            this.hazelcastInstance = Hazelcast.newHazelcastInstance(this.config);
            this.implicitlyCreated = true;
        }
    }

    public void destroy() throws Exception {
        if (this.implicitlyCreated) {
            hazelcastInstance.getLifecycleService().shutdown();
        }
    }

    public HazelcastInstance getHazelcastInstance() {
        return hazelcastInstance;
    }

    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }
}