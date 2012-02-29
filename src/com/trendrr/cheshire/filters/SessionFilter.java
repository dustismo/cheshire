/**
 * 
 */
package com.trendrr.cheshire.filters;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultCookie;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.util.internal.ConcurrentHashMap;

import com.trendrr.cheshire.caching.TrendrrCaches;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.IsoDateUtil;
import com.trendrr.oss.Reflection;
import com.trendrr.oss.TypeCast;
import com.trendrr.oss.cache.TrendrrCache;
import com.trendrr.oss.cache.TrendrrCacheStore;
import com.trendrr.oss.concurrent.Initializer;
import com.trendrr.oss.concurrent.LazyInit;
import com.trendrr.strest.StrestException;
import com.trendrr.strest.server.StrestController;
import com.trendrr.strest.server.StrestControllerFilter;
import com.trendrr.strest.server.StrestRouter;


/**
 * 
 * 
 * @author Dustin Norlander
 * @created Jun 14, 2011
 * 
 */
public class SessionFilter implements StrestControllerFilter {

	protected Log log = LogFactory.getLog(SessionFilter.class);
	protected static String SESSION = "sessionId";
	protected int maxAge = 60*30; //30 minutes
	
//	protected static ConcurrentHashMap<StrestRouter, LazyInit> persistenceInit = new ConcurrentHashMap<StrestRouter, LazyInit>();
//	protected static ConcurrentHashMap<StrestRouter, TrendrrCache> persistence = new ConcurrentHashMap<StrestRouter,TrendrrCache>();
	
	/**
	 * should this filter run or be skipped?
	 * @param controller
	 * @return
	 */
	protected boolean shouldRun(StrestController controller) {
		if (controller == null) {
			return false; //wtf?
		}
		
		if (controller.isStrest()) {
			return false;
		}
		
		if (controller.routes()[0].startsWith("/static")) {
			return false;
		}
		
		if (controller.getConnectionStorage() == null) {
			return false; //wtf?
		}
		return true;
	}
	/* (non-Javadoc)
	 * @see com.trendrr.strest.server.StrestControllerFilter#before(com.trendrr.strest.server.StrestController)
	 */
	@Override
	public void before(StrestController controller) throws StrestException {
		if (!this.shouldRun(controller)) {
			return;
		}
        
		String sessionId = null;
		 // get the session cookie.
        String cookieString = controller.getRequest().getHeader(HttpHeaders.Names.COOKIE);
        if (cookieString != null) {
            CookieDecoder cookieDecoder = new CookieDecoder();
            Set<Cookie> cookies = cookieDecoder.decode(cookieString);
            for (Cookie cookie : cookies) {
            	if (cookie.getName().equals(SESSION)) {
            		sessionId = cookie.getValue();
            	}
            }
            if(!cookies.isEmpty()) {
                // Reset the cookies if necessary.
                CookieEncoder cookieEncoder = new CookieEncoder(true);
                for (Cookie cookie : cookies) {
                    cookieEncoder.addCookie(cookie);
                }
            }
        }
        if ("deleted".equals(sessionId)) {
        	sessionId = null;
        }
        
        if (sessionId != null) {
        	//load the session.
        	Map<String,Object> vals = DynMap.instance(this.getSessionPersistence(controller).get(sessionId));
        	
        	Date expires = TypeCast.cast(Date.class, "expires");
        	if (expires != null && expires.before(new Date())) {
        		log.info("Session expired!");
        		return;
        	}
        	if (vals != null) {
        		controller.getSessionStorage().putAll(vals);
        	}
        	controller.getConnectionStorage().put(SESSION, sessionId);
        }

	}

	/**
	 * returns the persistence provider, or defaultsessionpersistence if none is specified
	 * @param controller
	 * @return
	 */
	protected TrendrrCache getSessionPersistence(final StrestController controller) {
		return TrendrrCaches.getCacheOrDefault("sessions", controller);
		
//		persistenceInit.putIfAbsent(controller.getRouter(), new LazyInit());
//		LazyInit init = persistenceInit.get(controller.getRouter());
//		if (init.start()) {
//			//lazily initialize
//			try {
//				
//				DynMap sessionConfig = controller.getServerConfig().getMap("sessions", new DynMap());
//				String cls = sessionConfig.getString("persistence", "com.trendrr.strest.contrib.sessions.DefaultSessionPersistence");
//				
//				TrendrrCache per = persistence.get(cls);
//				if (per == null) {
//					try {
//						per = (TrendrrCache)Reflection.instance(Class.forName(cls), sessionConfig);
//					} catch (Exception e) {
//						log.warn("Unable to load TrendrrCache class: " + cls);
//					}
//				}
//				if (per == null) {
//					log.warn("No sessions.persistence class available, using dummy provider");
//					per = new InMemoryTrendrrCache(sessionConfig);
//				}
//				persistence.put(controller.getRouter(), per);
//			} finally {
//				init.end();
//			}
//		}
//		return persistence.get(controller.getRouter());
	}
	
	/* (non-Javadoc)
	 * @see com.trendrr.strest.server.StrestControllerFilter#after(com.trendrr.strest.server.StrestController)
	 */
	@Override
	public void after(StrestController controller) throws StrestException {
		if (!this.shouldRun(controller)) {
			return;
		}
		String sessionId = (String)controller.getConnectionStorage().get(SESSION);
		if (TypeCast.cast(Boolean.class, controller.getConnectionStorage().get("session_destroy"), false)) {
			log.info("Destroying session!");
			//destroy the session.
			if (sessionId == null) {
				return;
			}
			CookieEncoder cookieEncoder = new CookieEncoder(true);
			Cookie cookie = new DefaultCookie(SESSION, "deleted");
			cookie.setMaxAge(0);
			cookieEncoder.addCookie(cookie);
	        controller.getResponse().setHeader(HttpHeaders.Names.SET_COOKIE, cookieEncoder.encode());
	        this.getSessionPersistence(controller).delete(sessionId);
			return;
		}
		
		
		if (sessionId == null && !controller.getSessionStorage().isEmpty()) {
			CookieEncoder cookieEncoder = new CookieEncoder(true);
			sessionId = UUID.randomUUID().toString();
			Cookie cookie = new DefaultCookie(SESSION, sessionId);
			cookie.setMaxAge(this.maxAge);
			cookieEncoder.addCookie(cookie);
	        controller.getResponse().setHeader(HttpHeaders.Names.SET_COOKIE, cookieEncoder.encode());
		}
		//save the session.
		Date expires = new Date(new Date().getTime()+(1000*this.maxAge));
		controller.getSessionStorage().put("expires", IsoDateUtil.getIsoDate(expires));
		if (sessionId != null) {
			this.getSessionPersistence(controller).set(sessionId, DynMap.instance(controller.getSessionStorage()).toJSONString(), expires);
		}
	}

	/* (non-Javadoc)
	 * @see com.trendrr.strest.server.StrestControllerFilter#error(com.trendrr.strest.server.StrestController, org.jboss.netty.handler.codec.http.HttpResponse, java.lang.Exception)
	 */
	@Override
	public void error(StrestController controller, HttpResponse response,
			Exception exception) {
	}
}