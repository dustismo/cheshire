/**
 * 
 */
package com.trendrr.cheshire.authentication;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.oss.DynMap;
import com.trendrr.oss.Reflection;
import com.trendrr.oss.cache.TrendrrCacheItem;


/**
 * 
 * A suitable implementation for most people.
 * 
 * Implementors are free to override this class to change anything, just be sure to 
 * have a default constructor and handle fromDynMap and toDynMap methods as these are used for 
 * serializing in the cache and session.
 * 
 * @author Dustin Norlander
 * @created Jan 27, 2012
 * 
 */
public class AuthToken{

	protected static Log log = LogFactory.getLog(AuthToken.class);

	protected Set<String> routesAllowed = new HashSet<String>();
	protected Set<String> routesDisallowed = new HashSet<String>();
	protected String userId = null;
	protected Set<String> userAccessRoles = new HashSet<String>();
	protected Integer rateLimit = null;
	protected boolean saveInConnection = true;
	
	
	/**
	 * loads a new auth token based on the classname.
	 * @param cls
	 * @param content
	 * @return
	 */
	public static AuthToken instance(String cls, DynMap content) throws Exception{
		AuthToken tok = Reflection.defaultInstance(AuthToken.class, cls);
		tok.fromDynMap(content);
		return tok;
	}
	
	/**
	 * Should we save the auth token in the connection?  This is only applicable to STREST connections, and
	 * lets us know if the token should persist for the length of the connection
	 */
	public boolean isSaveInConnection() {
		return saveInConnection;
	}

	public void setSaveInConnection(boolean saveInConnection) {
		this.saveInConnection = saveInConnection;
	}

	/**
	 * Manditory constructor.
	 */
	public AuthToken() {
	}
	
	public AuthToken(
			Collection<String> routesAllowed,
			Collection<String> routesDisallowed,
			String userId,
			Collection<String> userAccessRoles,
			Integer rateLimit,
			boolean saveInConnection
			) {
		this.routesAllowed.addAll(routesAllowed);
		this.routesDisallowed.addAll(routesDisallowed);
		this.setRateLimit(rateLimit);
		this.userAccessRoles.addAll(userAccessRoles);
		this.setUserId(userId);
		this.setSaveInConnection(saveInConnection);
	}
	
	/**
	 * convert to a DynMap, used for serialization.
	 * 
	 * All objects in the dynmap should be json serializable
	 * 
	 * @return
	 */
	public DynMap toDynMap() {
		DynMap mp = new DynMap();
		mp.put("user_id", this.getUserId());
		mp.put("routes_allowed", this.getRoutesAllowed());
		mp.put("routes_disallowed", this.getRoutesDisallowed());
		mp.put("user_access_roles", this.getUserAccessRoles());
		mp.put("rate_limit", this.getRateLimit());
		mp.put("save_in_connection", this.isSaveInConnection());
		return mp;
	}
	
	/**
	 * Deserializes from toDynMap().
	 * @param mp
	 */
	public void fromDynMap(DynMap mp) {
		this.routesAllowed.addAll(mp.getListOrEmpty(String.class, "routes_allowed"));
		this.routesDisallowed.addAll(mp.getListOrEmpty(String.class, "routes_disallowed"));
		this.userAccessRoles.addAll(mp.getListOrEmpty(String.class, "user_access_roles"));
		this.setUserId(mp.getString("user_id"));
		this.setSaveInConnection(mp.getBoolean("save_in_connection", true));
		this.setRateLimit(mp.getInteger("rate_limit"));
	}

	
	public Set<String> getUserAccessRoles() {
		return userAccessRoles;
	}

	public void setUserAccessRoles(Set<String> userAccessRoles) {
		this.userAccessRoles = userAccessRoles;
	}

	public Integer getRateLimit() {
		return rateLimit;
	}

	public void setRateLimit(Integer rateLimit) {
		this.rateLimit = rateLimit;
	}

	public void addRoutesAllowed(Collection<String> routesAllowed) {
		if (routesAllowed == null)
			return;
		this.getRoutesAllowed().addAll(routesAllowed);
	}
	public void setRoutesAllowed(Set<String> routesAllowed) {
		this.routesAllowed = routesAllowed;
	}

	public void addRoutesDisallowed(Collection<String> routesDisllowed) {
		if (routesDisallowed == null)
			return;
		this.getRoutesDisallowed().addAll(routesDisallowed);
	}
	public void setRoutesDisallowed(Set<String> routesDisallowed) {
		this.routesDisallowed = routesDisallowed;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}


	
	public String getUserId() {
		return this.userId;
	}
	
	/**
	 * A collection of allowed routes for this user.
	 * if this set is empty/null then everything is allowed.  if there is at least
	 * one route, then these are the <b>only</b> allowed routes
	 * 
	 * For now, routes can be starting fragments. i.e. allowed=['/v4/'] would give access to
	 * any route starting with /v4/.  TODO: support wildcards
	 * 
	 * @return
	 */
	public Set<String> getRoutesAllowed() {
		return this.routesAllowed;
	}
	
	
	/**
	 * set individual routes the user does not have access to.
	 * 
	 * For now, routes can be starting fragments. i.e. allowed=['/v4/'] would deny access to
	 * any route starting with /v4/.  TODO: support wildcards
	 * @return
	 */
	public Set<String> getRoutesDisallowed() {
		return this.routesDisallowed;
	}
	
	public boolean hasAccessToRoute(String[] routes) {
		if (routes == null)
			return false;
		for (String r : routes) {
			if (this.hasAccessToRoute(r))
				return true;
		}
		return false;
	}

	/**
	 * Return true if this user has access to the requested route
	 * 
	 * The default implementation looks at the routesDisallowed, routesAllowed and userAccess settings.
	 */
	public boolean hasAccessToRoute(String route) {
		Set<String> disallowed = this.getRoutesDisallowed();
		
		if (disallowed != null) {
			for (String dis : this.getRoutesDisallowed()) {
				if (route.startsWith(dis)) {
					log.warn(route + " disallowed: " + dis);
					return false;
				}
			}
		}
		
		Set<String> allowed = this.getRoutesAllowed();
		if (allowed != null && !allowed.isEmpty()) {
			for (String al : allowed) {
				if (route.startsWith(al)) {
					return true;
				}
			}
			
			if (route.startsWith("/docs")) {
				//everyone has access to docs
				return true;
			}
			log.warn(route + " not in the allowed list: " + allowed);
			return false;
		}
		return true;
	}
}
