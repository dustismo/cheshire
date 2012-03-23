/**
 * 
 */
package com.trendrr.cheshire.caching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.trendrr.oss.DynMap;
import com.trendrr.oss.Reflection;
import com.trendrr.oss.cache.TrendrrCache;
import com.trendrr.oss.cache.TrendrrCacheStore;
import com.trendrr.oss.concurrent.Initializer;
import com.trendrr.strest.server.StrestController;


/**
 * @author Dustin Norlander
 * @created Jan 3, 2012
 * 
 */
public class TrendrrCaches extends TrendrrCacheStore {

	protected static Logger log = LoggerFactory.getLogger(TrendrrCaches.class);
	
	public static TrendrrCache getDefaultCache(final StrestController controller) {
		return getCache("default", controller);
	}
	
	public static TrendrrCache getCacheOrDefault(final String configname, final StrestController controller) {
		TrendrrCache cache = getCache(configname, controller);
		if (cache == null)
			cache = getDefaultCache(controller);
		return cache;
	}
	
	/**
	 * gets a trendrr cache implementation. 
	 * 
	 * @param configname the name of the configuration details in server configuration 
	 * @param controller
	 * @return
	 */
	public static TrendrrCache getCache(final String configname, final StrestController controller) {
		//get the default
		TrendrrCache cache = TrendrrCacheStore.instance().getCache(configname);
		//need to check that the controller isn't null otherwise we can initialize a bogus cache,
		//which would reak havok until restart.
		if (cache == null && controller != null) {
			cache = TrendrrCacheStore.instance().getCache(configname, new Initializer<TrendrrCache>() {
				@Override
				public TrendrrCache init() {
					DynMap config = controller.getServerConfig().getMap("caches." + configname, new DynMap());
					String cls = config.getString("classname");
					log.warn("Got cls: " +cls);
					log.warn(config.toJSONString());
					log.warn("serverconfig: " + controller.getServerConfig().toJSONString());
					log.warn("key: " + configname);
					if (cls== null) {
						return null;
					}
					
					try {
						return (TrendrrCache)Reflection.instance(Class.forName(cls), config);
					} catch (Exception e) {
						log.warn("Unable to load TrendrrCache class: " + cls);
					}
					return null;
				}
			});
		}
		return cache;
	}
}
