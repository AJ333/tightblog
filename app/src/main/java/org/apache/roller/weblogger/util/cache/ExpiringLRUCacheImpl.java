/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 *
 * Source file modified from the original ASF source; all changes made
 * are also under Apache License.
 */
package org.apache.roller.weblogger.util.cache;

import org.apache.roller.weblogger.WebloggerCommon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An LRU cache where entries expire after a given timeout period.
 *
 * Negative timeout values indicate entries will never time out but can be
 * pushed out of the cache when the cache reaches its max size
 *
 * Zero timeout value means entries are invalid just after being placed
 * in and hence never usable.
 */
public class ExpiringLRUCacheImpl implements Cache {
    private static Logger log = LoggerFactory.getLogger(ExpiringLRUCacheImpl.class);

    private Map<String, Object> cache = null;
    private long timeoutInMS = 0;

    // for metrics
    protected int hits = 0;
    protected int misses = 0;
    protected int puts = 0;
    protected int removes = 0;
    protected Date startTime = new Date();

    public ExpiringLRUCacheImpl(int maxsize, long timeoutInMS) {
        this.cache = Collections.synchronizedMap(new LRULinkedHashMap<>(maxsize));
        this.timeoutInMS = timeoutInMS;
    }

    public synchronized void remove(String key) {
        this.cache.remove(key);
        removes++;
    }

    public synchronized void clear() {
        this.cache.clear();

        // clear metrics
        hits = 0;
        misses = 0;
        puts = 0;
        removes = 0;
        startTime = new Date();
    }

    public CacheStats getStats() {
        CacheStats stats = new CacheStats();
        stats.setStartTime(startTime);
        stats.setHits(hits);
        stats.setMisses(misses);
        stats.setPuts(puts);
        stats.setRemoves(removes);

        // calculate efficiency
        if (misses + hits > 0) {
            double efficiency = hits * WebloggerCommon.PERCENT_100 / (misses + hits);
            stats.setEfficiency(efficiency);
        }

        return stats;
    }

    /**
     * Store an entry in the cache.
     *
     * We wrap the cached object in our ExpiringCacheEntry object so that we
     * can track when the entry has expired.
     */
    @Override
    public synchronized void put(String key, Object value) {
        ExpiringCacheEntry entry = new ExpiringCacheEntry(value, this.timeoutInMS);
        this.cache.put(key, entry);
        puts++;
    }

    /**
     * Retrieve an entry from the cache.
     *
     * This LRU cache supports timeouts, so if the cached object has expired
     * then we return null, just as if the entry wasn't found.
     */
    @Override
    public synchronized Object get(String key) {
        Object value = null;
        ExpiringCacheEntry entry;
        
        synchronized(this) {
            entry = (ExpiringCacheEntry) this.cache.get(key);

            // for metrics
            if(entry == null) {
                misses++;
            } else {
                hits++;
            }
        }
        
        if (entry != null) {
            value = entry.getValue();
            
            // if the value is null then that means this entry expired
            if (value == null) {
                log.debug("EXPIRED {}", key);
                hits--;
                remove(key);
            }
        }
        
        return value;
    }

    // David Flanaghan: http://www.davidflanagan.com/blog/000014.html
    private static class LRULinkedHashMap<String, Object>
            extends LinkedHashMap<String, Object> {
        protected int maxsize;

        public LRULinkedHashMap(int maxsize) {
            super(maxsize * 4 / 3 + 1, 0.75f, true);
            this.maxsize = maxsize;
        }

        protected boolean removeEldestEntry(Map.Entry eldest) {
            return this.size() > this.maxsize;
        }
    }


}
