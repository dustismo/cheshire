/**
 * 
 */
package com.trendrr.cheshire.controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.cheshire.CheshireApi;
import com.trendrr.cheshire.CheshireApiController;
import com.trendrr.cheshire.caching.TrendrrCaches;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.DynMapFactory;
import com.trendrr.oss.Timeframe;
import com.trendrr.oss.TypeCast;
import com.trendrr.oss.cache.TrendrrCache;


/**
 * @author Dustin Norlander
 * @created Feb 1, 2012
 * 
 */
@CheshireApi(
	route = "/admin/rate_limit/stats",
	access = { "administrator" }
//	requiredParams = {"user_id"}
)
public class RateLimitStats extends CheshireApiController {

	protected Log log = LogFactory.getLog(RateLimitStats.class);
	
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
