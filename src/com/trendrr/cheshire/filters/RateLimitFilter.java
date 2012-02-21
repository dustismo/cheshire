/**
 * 
 */
package com.trendrr.cheshire.filters;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.trendrr.cheshire.authentication.AuthToken;
import com.trendrr.cheshire.caching.TrendrrCaches;
import com.trendrr.oss.Timeframe;
import com.trendrr.oss.cache.TrendrrCache;
import com.trendrr.strest.StrestException;
import com.trendrr.strest.StrestHttpException;
import com.trendrr.strest.server.StrestController;
import com.trendrr.strest.server.StrestControllerFilter;


/**
 * Filter providing rate limiting.
 * 
 * @author Dustin Norlander
 * @created Feb 1, 2012
 * 
 */
public class RateLimitFilter implements StrestControllerFilter {

	protected Log log = LogFactory.getLog(RateLimitFilter.class);

	/* (non-Javadoc)
	 * @see com.trendrr.strest.server.StrestControllerFilter#before(com.trendrr.strest.server.StrestController)
	 */
	@Override
	public void before(StrestController controller) throws StrestException {
		TrendrrCache cache = TrendrrCaches.getCacheOrDefault("rate_limits", controller);
		AuthToken token = (AuthToken)controller.getTxnStorage().get("auth_token");
		if (token == null) {
			token = new AuthToken() {
				{
					this.setUserId("PUBLIC"); //TODO: this should be the users IP address!
				}
			};
		}
		
		String id =  token.getUserId();
		
		long val = this.inc(cache, id);
		this.inc(cache, "ALL");
		log.info("rate limit for: " + id + ": " + val);
		log.info("key: " + id + "_" + Timeframe.MINUTES.toTrendrrEpoch(new Date()));
		if (this.isOverLimit(controller, token, val)) {
			throw StrestHttpException.RATE_LIMITED("You have maxed out your request limit");
		}
	}
	
	protected long inc(TrendrrCache cache, String userId) {
		return cache.inc("rate_limits", userId + "_" + Timeframe.MINUTES.toTrendrrEpoch(new Date()), 1, Timeframe.HOURS.add(new Date(), 2));
	}
	
	protected boolean isOverLimit(StrestController controller, AuthToken token, long curVal) {
		if (token.getRateLimit() == null || token.getRateLimit() < 1) {
			return false;
		}
		return curVal < token.getRateLimit();
	}

	/* (non-Javadoc)
	 * @see com.trendrr.strest.server.StrestControllerFilter#after(com.trendrr.strest.server.StrestController)
	 */
	@Override
	public void after(StrestController controller) throws StrestException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.trendrr.strest.server.StrestControllerFilter#error(com.trendrr.strest.server.StrestController, org.jboss.netty.handler.codec.http.HttpResponse, java.lang.Exception)
	 */
	@Override
	public void error(StrestController controller, HttpResponse response,
			Exception exception) {
		// TODO Auto-generated method stub

	}
}
