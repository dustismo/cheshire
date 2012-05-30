/**
 * 
 */
package com.trendrr.cheshire.filters;

import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.trendrr.cheshire.CheshireApiController;
import com.trendrr.cheshire.CheshireController;
import com.trendrr.oss.DynMap;
import com.trendrr.strest.StrestException;
import com.trendrr.strest.server.ResponseBuilder;
import com.trendrr.strest.server.v2.models.StrestResponse;


/**
 * Handles everything related to the return type. 
 * 
 * An exception is thrown BEFORE the controller is executed if the return type is invalid.
 * 
 * makes sure requested return type is valid, and sets it on the way out, whenever possible.
 * 
 * 
 * @author Dustin Norlander
 * @created Mar 15, 2011
 * 
 */
public class ReturnTypeFilter extends CheshireFilter {

	protected static Logger log = LoggerFactory.getLogger(ReturnTypeFilter.class);

	/* (non-Javadoc)
	 * @see com.trendrr.strest.server.StrestControllerFilter#before(com.trendrr.strest.server.StrestController)
	 */
	@Override
	public void before(CheshireController cont) throws StrestException {
		((CheshireApiController)cont).setReturnType(cont.getParams().getString("return_type"));
	}

	/* (non-Javadoc)
	 * @see com.trendrr.strest.server.StrestControllerFilter#after(com.trendrr.strest.server.StrestController)
	 */
	@Override
	public void after(CheshireController cont) throws StrestException {
		if (!cont.isSendResponse()) {
			return;//do nothing.
		}
		String returnType = ((CheshireApiController)cont).getReturnType();
		this.setBytes(cont, returnType, cont.getResponse(), ((CheshireApiController)cont).getReturnResult());
	}

	/* (non-Javadoc)
	 * @see com.trendrr.strest.server.StrestControllerFilter#error(com.trendrr.strest.server.StrestController, org.jboss.netty.handler.codec.http.HttpResponse, java.lang.Exception)
	 */
	@Override
	public void error(CheshireController controller, StrestResponse response,
			Exception exception) {
		String type = "json";
		if (controller != null) {
			type = ((CheshireApiController)controller).getReturnType();
		}
		this.setBytes(controller, type, response, new DynMap());
	}
	
	private void setBytes(CheshireController controller, String type, StrestResponse response, DynMap val) {
		if (type == null)
			type = "json";
		
		//automatically add the status portion of the return.
		if (val == null)
			val = new DynMap();
		
		DynMap status = val.get(DynMap.class, "status", new DynMap());
		status.putIfAbsent("message", response.getStatusMessage());
		status.put("code", response.getStatusCode());	
		if (controller != null && !((CheshireApiController)controller).getWarnings().isEmpty()) {
			status.put("warnings", ((CheshireApiController)controller).getWarnings());
		}
		
		val.put("status", status);
		try {
			if(type.equalsIgnoreCase("json")) {
				new ResponseBuilder(response).contentJSON(val);
			} else if (type.equalsIgnoreCase("xml")) {
				new ResponseBuilder(response).content("text/xml", val.toXMLString().getBytes("utf8"));	
			} else {
				new ResponseBuilder(response).content("text/plain", ("Unknown return type: " + type).getBytes("utf8"));
			}
		} catch (UnsupportedEncodingException e) {
			log.error("Caught", e);
		}
	}
}
