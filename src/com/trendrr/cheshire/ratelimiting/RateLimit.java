/**
 * 
 */
package com.trendrr.cheshire.ratelimiting;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.json.simple.JSONAware;


/**
 * 
 * Simple object to hold rate limit info for a specific user + type
 * 
 * @author Dustin Norlander
 * @created Mar 8, 2012
 * 
 */
public class RateLimit implements JSONAware {

	protected static Log log = LogFactory.getLog(RateLimit.class);
	protected String id; //the user id.
	protected String type = "ALL_GETS"; //all get requests
	protected Integer limit = 1000;
	
	public RateLimit(String userId, String type, Integer limit) {
		this.id = userId;
		this.type = type;
		this.limit = limit;
	}
	
	public String getId() {
		return id;
	}
	public String getType() {
		return type;
	}
	public Integer getLimit() {
		return limit;
	}
	/* (non-Javadoc)
	 * @see com.trendrr.json.simple.JSONAware#toJSONString()
	 */
	@Override
	public String toJSONString() {
		// TODO Auto-generated method stub
		return null;
	}
}
