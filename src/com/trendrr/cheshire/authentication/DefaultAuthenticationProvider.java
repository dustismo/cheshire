/**
 * 
 */
package com.trendrr.cheshire.authentication;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.cheshire.caching.TrendrrCaches;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.Reflection;
import com.trendrr.oss.Timeframe;
import com.trendrr.oss.cache.TrendrrCache;
import com.trendrr.oss.cache.TrendrrCacheItem;
import com.trendrr.strest.server.StrestController;


/**
 * @author Dustin Norlander
 * @created Jan 30, 2012
 * 
 */
public abstract class DefaultAuthenticationProvider implements AuthenticationProvider {

	protected Log log = LogFactory.getLog(DefaultAuthenticationProvider.class);

	
	public TrendrrCache getCache(StrestController controller) {
		return TrendrrCaches.getCacheOrDefault("authentication.cache", controller);
	}
	
	/**
	 * Gets from the cache. Key should be something unique to this login (the auth token, or sha1(username+password), ect
	 * @param cache
	 * @param key
	 * @return
	 */
	public AuthToken getFromCache(StrestController controller, String key) {
		Object obj = this.getCache(controller).get("auth_tokens", key);
		if (obj == null)
			return null;
		
		try {
			TrendrrCacheItem item = TrendrrCacheItem.deserialize((byte[])obj);
			String cls = item.getMetadata().getString("auth_token_class");
			DynMap content = DynMap.instance(new String(item.getContentBytes(), "utf8"));
			AuthToken tok = Reflection.defaultInstance(AuthToken.class, cls);
			tok.fromDynMap(content);
			log.info("got auth from cache: " + content.toJSONString());
			return tok;
		} catch (Exception e) {
			log.error("caught", e);
		}
		return null;
	}
	
	public void saveToCache(StrestController controller, String key, AuthToken token, int timeoutSeconds) {
		TrendrrCacheItem item = new TrendrrCacheItem();
		item.getMetadata().put("auth_token_class", token.getClass().getName());
		try {
			item.setContentBytes(token.toDynMap().toJSONString().getBytes("utf8"));
			this.getCache(controller).set("auth_tokens", key, item.serialize(), Timeframe.SECONDS.add(new Date(), timeoutSeconds));
		} catch (Exception e) {
			log.error("caught", e);
		}	
	}
}
