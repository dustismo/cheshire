/**
 * 
 */
package com.trendrr.cheshire.caching;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.cache.TrendrrCache;


/**
 * Implemented via the google guava cache.  
 * 
 * This is an imperfect implementation, expires is ignored on a per key basis and is only configurable per cache.
 * 
 * 
 * 
 * @author Dustin Norlander
 * @created Jan 3, 2012
 * 
 */
public class InMemoryTrendrrCache extends TrendrrCache {
	
	Cache<String, Object> cache = null;
	/**
	 * @param config
	 */
	public InMemoryTrendrrCache(DynMap config) {
		super(config);
	}

	protected static Log log = LogFactory.getLog(InMemoryTrendrrCache.class);

	/* (non-Javadoc)
	 * @see com.trendrr.oss.cache.TrendrrCache#_init(com.trendrr.oss.DynMap)
	 */
	@Override
	protected void _init(DynMap config) {
		log.warn("Initing in memory cache");
		cache = CacheBuilder.newBuilder()
			       .maximumSize(config.getInteger("max_size", 1000))
			       .expireAfterWrite(config.getInteger("expire_seconds", 60), TimeUnit.SECONDS)
			       .build();
	}

	/* (non-Javadoc)
	 * @see com.trendrr.oss.cache.TrendrrCache#_set(java.lang.String, java.lang.Object, java.util.Date)
	 */
	@Override
	protected synchronized void _set(String key, Object obj, Date expires) {
		this.cache.put(key, obj);
		
		log.warn("saving key: " +key + " -- uhh, just kidding :)");
	}

	/* (non-Javadoc)
	 * @see com.trendrr.oss.cache.TrendrrCache#_get(java.lang.String)
	 */
	@Override
	protected synchronized Object _get(String key) {
		return this.cache.getIfPresent(key);
	}

	/* (non-Javadoc)
	 * @see com.trendrr.oss.cache.TrendrrCache#_inc(java.lang.String, java.lang.Number)
	 */
	@Override
	protected synchronized long _inc(String key, int value, Date expire) {
		log.warn("inc key: " +key + " -- uhh, just kidding :)");
		return 0l;
	}

	/* (non-Javadoc)
	 * @see com.trendrr.oss.cache.TrendrrCache#_addToSet(java.util.Collection)
	 */
	@Override
	protected synchronized Set<String> _addToSet(String key, Collection<String> str, Date expire) {
		log.warn("saving set: " + key + " -- uhh, just kidding :)");
		return null;
	}

	/* (non-Javadoc)
	 * @see com.trendrr.oss.cache.TrendrrCache#_removeFromSet(java.util.Collection)
	 */
	@Override
	protected synchronized Set<String> _removeFromSet(String key, Collection<String> str) {
		log.warn("remove from set: " + key + " -- uhh, just kidding :)");
		return null;
	}

	/* (non-Javadoc)
	 * @see com.trendrr.oss.cache.TrendrrCache#_setIfAbsent(java.lang.String, java.lang.Object, java.util.Date)
	 */
	@Override
	protected synchronized boolean _setIfAbsent(String key, Object value, Date expires) {
		if (this.cache.getIfPresent(key) == null) {
			this._set(key, value, expires);
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.trendrr.oss.cache.TrendrrCache#_del(java.lang.String)
	 */
	@Override
	protected synchronized void _del(String key) {
		this.cache.invalidate(key);
	}

	/* (non-Javadoc)
	 * @see com.trendrr.oss.cache.TrendrrCache#_getMulti(java.util.Collection)
	 */
	@Override
	protected Map<String, Object> _getMulti(Set<String> keys) {
		Map<String,Object> vals = new HashMap<String, Object>();
		for (String key : keys) {
			Object v = this._get(key);
			if (v != null) {
				vals.put(key, v);
			}
		}
		return vals;
	}

	/* (non-Javadoc)
	 * @see com.trendrr.oss.cache.TrendrrCache#_getSet(java.lang.String)
	 */
	@Override
	protected Set<String> _getSet(String key) {
		// TODO Auto-generated method stub
		return null;
	}
}
