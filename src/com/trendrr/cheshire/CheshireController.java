/**
 * 
 */
package com.trendrr.cheshire;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.cheshire.authentication.AuthToken;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.DynMapFactory;
import com.trendrr.oss.Reflection;
import com.trendrr.oss.StringHelper;

import com.trendrr.strest.StrestException;
import com.trendrr.strest.StrestHttpException;
import com.trendrr.strest.server.StrestController;



/**
 * @author Dustin Norlander
 * @created Jan 30, 2012
 * 
 */
public class CheshireController extends StrestController {

	protected Log log = LogFactory.getLog(CheshireController.class);
	
	protected DynMap returnResult = new DynMap();
	protected String returnType = null;
	protected List<String> warnings = new ArrayList<String>();
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
	 * warning will show up in the warning section of the status portion of the packet.
	 * @param warning
	 */
	public void addWarning(String warning) {
		this.warnings.add(warning);
	}

	public List<String> getWarnings() {
		return this.warnings;
	}
	
	/**
	 * format of return message
	 * @return
	 */
	public String getReturnType() {
		if (this.returnType == null) {
			return "json";
		}
		return returnType;
	}

	public void setReturnType(String returnType) throws StrestException{
		if (returnType == null)
			return;
		if (returnType.equalsIgnoreCase("json"))
			this.returnType = "json";
		if (returnType.equalsIgnoreCase("xml"))
			this.returnType = "xml";
		else 
			this.returnType = returnType;
		
		for (String type : this.returnTypes()) {
			if (type.equals(this.returnType)) {
				return;
			}
		}
		throw StrestHttpException.NOT_ACCEPTABLE("Invalid return type");
	}
	
	public DynMap getReturnResult() {
		return returnResult;
	}
	public void setReturnResult(DynMap returnResult) {
		this.returnResult = returnResult;
	}
	
	/**
	 * sets the data field with whatever is passed in. Will not convert anything.
	 * use addData to automatically convert to dynmaps
	 * @param data
	 */
	public void setData(Object data) {
		
		this.getReturnResult().put("data", data);
	}
	
	/**
	 * will add to the current "data" field.  if field is not currently an array, then it is converted.
	 * 
	 * Will convert to dynmap if it is not an array or collection.
	 * @param data
	 */
	public void addData(Object data) {
		if (data == null)
			return;
		
		Object cur = this.getReturnResult().get("data");
		List vals = new ArrayList();
		
		if (data instanceof Collection) {	
			for (Object obj : (Collection)data) {
				if (obj instanceof DynMap) {
					vals.add((DynMap)obj);
				} else {
					vals.add(DynMapFactory.instance(obj));
				}
			}
		} else {
			
			if (data instanceof DynMap) {
				vals.add((DynMap)data);
			} else {
				DynMap tmp = DynMapFactory.instance(data);
				if (tmp != null) {
					vals.add(tmp);
				} else {
					vals.add(data);
				}
			}
		}
		if (cur == null && vals.size() == 1) {
			this.setData(vals.get(0));
		} else if (cur == null && vals.size() > 1) {
			this.setData(vals);
		} else {
			if (cur instanceof Collection) {
				((Collection)cur).addAll(vals);
			} else {
				List array = new ArrayList();
				array.add(cur);
				array.addAll(vals);
				this.setData(array);
			}	
		}
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
	
	public boolean enableSessions() {
		if (this.isAnnotationPresent()) {
			return (Boolean)Reflection.execute(this.getClass().getAnnotation(this.getAnnotationClass()), "enableSessions");
		}
		return false;
	}
	
	public int cacheTimeoutSeconds() {
		if (this.cacheTimeoutSeconds != null) {
			return this.cacheTimeoutSeconds;
		}
		
		if (this.isAnnotationPresent()) {
			return (Integer)Reflection.execute(this.getClass().getAnnotation(this.getAnnotationClass()), "cacheTimeoutSeconds");
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
	 * filters, would override the regular filters.
	 * 
	 * Not sure I like this, maybe 'addFilters' and 'removeFilters' would be better
	 * @return
	 */
	@Override
	public Class[] filters() {
		Class[] tmp = super.filters();
		if (tmp != null)
			return tmp;
		
		if (this.isAnnotationPresent()) {
			return (Class[])Reflection.execute(this.getClass().getAnnotation(this.getAnnotationClass()), "filters");
		}
		return null;
	}
	
	public String[] requiredParams() {	
		String [] req = super.requiredParams();
		if (req != null) {
			return req;
		}
		if (this.isAnnotationPresent()) {	
			return  (String[])Reflection.execute(this.getClass().getAnnotation(this.getAnnotationClass()), "requiredParams");
		}
		return null;
	}
	
	/**
	 * override 
	 * @return
	 */
	public boolean requireAuthentication() {
		if (this.isAnnotationPresent()) {
			if ((Boolean)Reflection.execute(this.getClass().getAnnotation(this.getAnnotationClass()), "authenticate")) {
				System.out.println("Authenticate is true");
				return true;
			}
			//true if there is any required access.
			String[] requiredAccess = this.requiredAccess();
			if (requiredAccess != null && requiredAccess.length > 0) {
				System.out.println(requiredAccess);
				return true;
			}
		}
		return false;
	}
	
	public String[] requiredAccess() {
		if (this.isAnnotationPresent()) {
			return (String[])Reflection.execute(this.getClass().getAnnotation(this.getAnnotationClass()), "access");
		}
		return null;
	}
	
	public String[] returnTypes() {
		if (this.isAnnotationPresent()) {
			return (String[])Reflection.execute(this.getClass().getAnnotation(this.getAnnotationClass()), "returnTypes");
		}
		return new String[] {"json","xml"};
	}
	//we cache the routes.
	private String[] routes = null;
	@Override
	public String[] routes() {
		if (this.routes != null) {
			return routes;
		}
		
		this.routes = super.routes();
		if (this.routes != null) {
			return this.routes;
		}
		if (this.isAnnotationPresent()) {
			this.routes = (String[])Reflection.execute(this.getClass().getAnnotation(this.getAnnotationClass()), "route");
		}
		return this.routes;
	}
	
	protected Class getAnnotationClass() {
		return CheshireApi.class;
	}
	
	private boolean isAnnotationPresent() {
		return this.getClass().isAnnotationPresent(this.getAnnotationClass());
	}
}
