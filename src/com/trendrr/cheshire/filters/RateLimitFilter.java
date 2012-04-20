/**
 * 
 */
package com.trendrr.cheshire.filters;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.trendrr.cheshire.CheshireController;
import com.trendrr.cheshire.authentication.AuthToken;
import com.trendrr.cheshire.caching.TrendrrCaches;
import com.trendrr.cheshire.ratelimiting.RateLimit;
import com.trendrr.cheshire.ratelimiting.RateLimiter;
import com.trendrr.oss.Timeframe;
import com.trendrr.oss.cache.TrendrrCache;
import com.trendrr.oss.concurrent.LazyInit;
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
public class RateLimitFilter extends CheshireFilter {

	protected static Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

	protected static RateLimiter limiter = null;
	protected static LazyInit lock = new LazyInit();
	
	/* (non-Javadoc)
	 * @see com.trendrr.cheshire.filters.CheshireFilter#before(com.trendrr.cheshire.CheshireController)
	 */
	@Override
	public void before(CheshireController controller) throws StrestException {
		AuthToken token = controller.getAuthToken();
		if (token == null) {
			return;
		}
		
		
		if (lock.start()) {
			try {
				limiter = this.initRateLimiter(controller);
			} finally {
				lock.end();
			}
		}
		
		if (limiter == null)
			return;
		RateLimit rl = token.getRateLimit(limiter,controller);
		if (rl == null) {
			return;
		}
		if (limiter.shouldThrottle(rl)) {
			throw StrestHttpException.RATE_LIMITED();
		}
		
	}

	protected RateLimiter initRateLimiter(CheshireController controller) {
		return new RateLimiter(controller);
	}
	/* (non-Javadoc)
	 * @see com.trendrr.cheshire.filters.CheshireFilter#after(com.trendrr.cheshire.CheshireController)
	 */
	@Override
	public void after(CheshireController controller) throws StrestException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.trendrr.cheshire.filters.CheshireFilter#error(com.trendrr.cheshire.CheshireController, org.jboss.netty.handler.codec.http.HttpResponse, java.lang.Exception)
	 */
	@Override
	public void error(CheshireController controller, HttpResponse response,
			Exception exception) {
		// TODO Auto-generated method stub
		
	}
}
