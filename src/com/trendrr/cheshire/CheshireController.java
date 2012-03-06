/**
 * 
 */
package com.trendrr.cheshire;

import java.nio.charset.Charset;

import com.trendrr.cheshire.authentication.AuthToken;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.StringHelper;


/**
 * @author Dustin Norlander
 * @created Feb 29, 2012
 * 
 */
public abstract class CheshireController extends com.trendrr.strest.server.StrestController {
	protected DynMap accessLog = new DynMap();
	private Integer cacheTimeoutSeconds = null;
	/**
	 * use to add additional fields to the access log.
	 * @return
	 */
	public DynMap getAccessLog() {
		return accessLog;
	}

	/**
	 * gets the AuthToken or null
	 * @return
	 */
	public AuthToken getAuthToken() {
		if (this.getTxnStorage().containsKey("auth_token")) {
			return (AuthToken)this.getTxnStorage().get("auth_token");
		}
		return (AuthToken)this.getConnectionStorage().get("auth_token");
	}

	/**
	 * if cacheTimeout is greater then 0, then this key is
	 * used to cache the return result.  Override for custom implementation.
	 * 
	 * if null is returned then content will not be cached.
	 * 
	 * @return
	 */
	public String getCacheKey() {
		String uri = this.request.getUri().split("\\?")[0];
		//do this so ordering of uri params is consistent.
		DynMap pms = this.getParams().clone();
		pms.removeAll("callback", "jsonp", "_");
		String key = uri + "?" + pms.toURLString();
		key = StringHelper.sha1Hex(key.getBytes(Charset.forName("utf8")));
		
		//TODO: automatically scale date, end_date, start_date to the nearest 10 seconds?
		
		
//		log.info("Cache KEY: " + Encoding.base64(bytes) + " ORIGINAL: " + key);
//		log.info("ORIGINAL KEY: " + key);
		return key;
	}
	
	public int cacheTimeoutSeconds() {
		if (this.cacheTimeoutSeconds != null) {
			return this.cacheTimeoutSeconds;
		}
		
		if (this.isAnnotationPresent()) {
			return this.getAnnotationVal(Integer.class, "cacheTimeoutSeconds");
		}
		return 0;
	}
	
	/**
	 * sets how long to cache the current key for.  
	 * this will override the annotation. 
	 * 
	 * if 0 then no caching is enabled.
	 * @param seconds
	 */
	public void setCacheTimeoutSeconds(int seconds) {
		this.cacheTimeoutSeconds = seconds;
	}
	
	/**
	 * override 
	 * @return
	 */
	public boolean requireAuthentication() {
		if (this.isAnnotationPresent()) {
			if (this.getAnnotationVal(Boolean.class, "authenticate")) {
				return true;
			}
			//true if there is any required access.
			String[] requiredAccess = this.requiredAccess();
			if (requiredAccess != null && requiredAccess.length > 0) {
				return true;
			}
		}
		return false;
	}
	
	public String[] requiredAccess() {
		if (this.isAnnotationPresent()) {
			return this.getAnnotationVal(String[].class, "access");
		}
		return null;
	}
	
	@Override
	protected abstract Class getAnnotationClass();
}
