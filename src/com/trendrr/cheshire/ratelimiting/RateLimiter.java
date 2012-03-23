/**
 * 
 */
package com.trendrr.cheshire.ratelimiting;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.Timeframe;
import com.trendrr.oss.TypeCast;
import com.trendrr.oss.cache.TrendrrCache;


/**
 * @author Dustin Norlander
 * @created Mar 8, 2012
 * 
 */
public class RateLimiter implements RemovalListener<String, AtomicInteger>{

	protected static Logger log = LoggerFactory.getLogger(RateLimiter.class);
	
	Timeframe timeframe = Timeframe.MINUTES;
	Integer timeframeAmount = 1;
	TrendrrCache longTermCache; //long term cache
	LoadingCache<String, AtomicInteger> localCache = null; //localcach
	public RateLimiter(DynMap config) {
		this.timeframe = Timeframe.instance(config.getString("time_unit", "minutes"));
		this.timeframeAmount = config.getInteger("time_unit_amount", 1);
		localCache = CacheBuilder.newBuilder()
			       .maximumSize(config.getInteger("local_max_size", 1000))
			       .expireAfterWrite(config.getInteger("local_flush_seconds", 10), TimeUnit.SECONDS)
			       .removalListener(this)
			       .build(new CacheLoader<String, AtomicInteger>() {
						@Override
						public AtomicInteger load(String str) throws Exception {
							return new AtomicInteger(TypeCast.cast(Integer.class,longTermCache.get("rate_limits", str), 0));
						}
			       });
	}
	
	
	public boolean shouldThrottle(RateLimit limit) {
		String key = this.epoch(new Date()) + ":" + limit.getId() + ":" + limit.getType();
		try {
			int val = this.localCache.get(key).incrementAndGet();
			if (limit.getLimit() <= val) {
				return true;
			}
		} catch (ExecutionException e) {
			log.error("SNAP! Problem with rate limiter!", e);
		}
		return false;
	}
	
	protected int epoch(Date date) {
		int e = timeframe.toTrendrrEpoch(date).intValue();
		return e - (e % timeframeAmount);
	}

	/* (non-Javadoc)
	 * @see com.google.common.cache.RemovalListener#onRemoval(com.google.common.cache.RemovalNotification)
	 */
	@Override
	public void onRemoval(RemovalNotification<String, AtomicInteger> removal) {
		/* we keep in long term for 60 X*/
		this.longTermCache.inc("rate_limits", removal.getKey(), removal.getValue().get(), this.timeframe.add(new Date(), 60*this.timeframeAmount));
	}
}
