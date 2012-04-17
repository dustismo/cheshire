/**
 * 
 */
package com.trendrr.cheshire.controllers.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.trendrr.cheshire.CheshireApi;
import com.trendrr.cheshire.CheshireApiController;
import com.trendrr.cheshire.caching.TrendrrCaches;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.DynMapFactory;
import com.trendrr.oss.Timeframe;
import com.trendrr.oss.TypeCast;
import com.trendrr.oss.cache.TrendrrCache;


/*//
 * 
 * @description Gives stats about the rate limiting filter. 
 * 
 * @param limit limit the number of results to show
 * @param user_id @default(PUBLIC) the user_id to show rate limits for 
 * @param end_date the end date to show results for. @default(NOW)
 */
@CheshireApi(
	route = "/admin/rate_limit/stats",
	access = { "administrator" }
)
public class RateLimitStats extends CheshireApiController {

	protected static Logger log = LoggerFactory.getLogger(RateLimitStats.class);
	
	public void handleGET(DynMap params) throws Exception {
		TrendrrCache cache = TrendrrCaches.getCacheOrDefault("rate_limits", this);
		int limit = params.getInteger("limit",30);
		Date endDate = params.get(Date.class, "end_date", new Date());
		String userId = params.getString("user_id", "PUBLIC");
		HashMap<String, DynMap> points = new HashMap<String,DynMap>();
		
		for (int i=0; i < limit; i++) {
			Date d = Timeframe.MINUTES.subtract(endDate, i);
			int epoch =  Timeframe.MINUTES.toTrendrrEpoch(d).intValue();
			String key = userId + "_" + epoch;
			DynMap p = new DynMap();
			p.put("ts", Timeframe.MINUTES.fromTrendrrEpoch(epoch));
			p.put("val", 0l);
			points.put(key, p);
			
		}
		
		Map<String, Object> results = cache.getMulti("rate_limits", points.keySet());
		for (String k : results.keySet()) {
			
			DynMap p = points.get(k);
			p.put("val", TypeCast.cast(Long.class, results.get(k), 0l));
		}
		ArrayList<DynMap> vals = new ArrayList<DynMap>(points.values());
		DynMapFactory.sort(vals, Date.class, "ts");
		this.setData(vals);
	}
}
