/**
 * 
 */
package com.trendrr.cheshire.filters;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultCookie;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.util.internal.ConcurrentHashMap;

import com.trendrr.cheshire.CheshireController;
import com.trendrr.cheshire.CheshireHTMLController;
import com.trendrr.cheshire.authentication.AuthToken;
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
import com.trendrr.strest.StrestHttpException;
import com.trendrr.strest.server.StrestController;
import com.trendrr.strest.server.StrestControllerFilter;
import com.trendrr.strest.server.StrestRouter;
import com.trendrr.strest.server.v2.models.StrestResponse;


/**
 * 
 * 
 * @author Dustin Norlander
 * @created Jun 14, 2011
 * 
 */
public class SessionFilter extends CheshireFilter {

	protected static Logger log = LoggerFactory.getLogger(SessionFilter.class);
	protected static String SESSION = "sessionId";
	protected int maxAge = 60*30; //30 minutes
	
	/**
	 * should this filter run or be skipped?
	 * @param controller
	 * @return
	 */
	protected boolean shouldRun(CheshireController controller) {
		if (controller == null) {
			return false; //wtf?
		}
		
		if (controller.isStrest()) {
			return false;
		}
		if (!(controller instanceof CheshireHTMLController)) {
			return false;
		}
		if (!((CheshireHTMLController)controller).enableSessions()) {
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
	public void before(CheshireController controller) throws StrestException {
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
        	DynMap vals = DynMap.instance(this.getSessionPersistence(controller).get(sessionId));
        	
        	Date expires = TypeCast.cast(Date.class, "expires");
        	if (expires != null && expires.before(new Date())) {
        		log.info("Session expired!");
        		return;
        	}
        	if (vals != null) {
        		log.info("Got session: " + vals.toJSONString());
        		((CheshireHTMLController)controller).getSessionStorage().putAll(vals);
        		DynMap authtmp = vals.getMap("auth_token");
        		if (authtmp != null) {
        			try {
						AuthToken token = AuthToken.instance(vals.getString("auth_token_class"), authtmp);
						controller.getConnectionStorage().put("auth_token", token);
					} catch (Exception e) {
						log.error("Caught", e);
					}
        		}
        	}
        	controller.getConnectionStorage().put(SESSION, sessionId);
        }

	}

	/**
	 * returns the persistence provider, or defaultsessionpersistence if none is specified
	 * @param controller
	 * @return
	 */
	protected TrendrrCache getSessionPersistence(final CheshireController controller) throws StrestException {
		TrendrrCache cache = TrendrrCaches.getCacheOrDefault("sessions", controller);
		if (cache == null) {
			throw StrestHttpException.INTERNAL_SERVER_ERROR("No cache configured for SessionFilter");
		}
		return cache;
	}
	
	/* (non-Javadoc)
	 * @see com.trendrr.strest.server.StrestControllerFilter#after(com.trendrr.strest.server.StrestController)
	 */
	@Override
	public void after(CheshireController controller) throws StrestException {
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
		
		if (controller.getAuthToken() != null) {
			//save the auth token.
			((CheshireHTMLController)controller).getSessionStorage().put("auth_token", controller.getAuthToken().toDynMap());
			((CheshireHTMLController)controller).getSessionStorage().put("auth_token_class", controller.getAuthToken().getClass().getCanonicalName());
		}
		log.info("Storing session!!!");
		
		if (sessionId == null && !((CheshireHTMLController)controller).getSessionStorage().isEmpty()) {
			CookieEncoder cookieEncoder = new CookieEncoder(true);
			sessionId = UUID.randomUUID().toString();
			Cookie cookie = new DefaultCookie(SESSION, sessionId);
			cookie.setMaxAge(this.maxAge);
			cookieEncoder.addCookie(cookie);
	        controller.getResponse().setHeader(HttpHeaders.Names.SET_COOKIE, cookieEncoder.encode());
		}
		//save the session.
		Date expires = new Date(new Date().getTime()+(1000*this.maxAge));
		((CheshireHTMLController)controller).getSessionStorage().put("expires", IsoDateUtil.getIsoDate(expires));
		if (sessionId != null) {
			this.getSessionPersistence(controller).set(sessionId, ((CheshireHTMLController)controller).getSessionStorage().toJSONString(), expires);
		}
	}

	/* (non-Javadoc)
	 * @see com.trendrr.strest.server.StrestControllerFilter#error(com.trendrr.strest.server.StrestController, org.jboss.netty.handler.codec.http.HttpResponse, java.lang.Exception)
	 */
	@Override
	public void error(CheshireController controller, StrestResponse response,
			Exception exception) {
		if (controller == null || !(controller instanceof CheshireHTMLController)) {
			return;
		}
		CheshireHTMLController c = (CheshireHTMLController)controller;
		//add flash message and still save the session.
		c.flashMessage("error", "Error!", exception.getMessage());
		try {
			this.after(controller);
		} catch (Exception x) {
			log.error("Caught", x);
		}
	}
}
