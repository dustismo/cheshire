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
import com.trendrr.strest.annotations.Strest;
import com.trendrr.strest.server.StrestController;



/**
 * @author Dustin Norlander
 * @created Jan 30, 2012
 * 
 */
public class CheshireApiController extends CheshireController {

	protected static Log log = LogFactory.getLog(CheshireApiController.class);
	
	protected DynMap returnResult = new DynMap();
	protected String returnType = null;
	protected List<String> warnings = new ArrayList<String>();
	
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
	
	public String[] returnTypes() { 
		if (this.isAnnotationPresent()) {
			return this.getAnnotationVal(String[].class, "returnTypes");
		}
		return new String[] {"json","xml"};
	}
	
	@Override
	protected Class getAnnotationClass() {
		return CheshireApi.class;
	}
	
	@Override
	public String getControllerNamespace() {
		return "api";
	}
}
