/**
 * 
 */
package com.trendrr.cheshire.filters;

import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.trendrr.cheshire.CheshireApiController;
import com.trendrr.oss.DynMap;
import com.trendrr.strest.StrestException;
import com.trendrr.strest.server.ResponseBuilder;


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

	protected Log log = LogFactory.getLog(ReturnTypeFilter.class);

	/* (non-Javadoc)
	 * @see com.trendrr.strest.server.StrestControllerFilter#before(com.trendrr.strest.server.StrestController)
	 */
	@Override
	public void before(CheshireApiController cont) throws StrestException {
		cont.setReturnType(cont.getParams().getString("return_type"));
	}

	/* (non-Javadoc)
	 * @see com.trendrr.strest.server.StrestControllerFilter#after(com.trendrr.strest.server.StrestController)
	 */
	@Override
	public void after(CheshireApiController cont) throws StrestException {
		if ((cont.getResponse().getContent() == null || cont.getResponse().getContent().readableBytes()==0)
				&& cont.isSendResponse()) {
			String returnType = cont.getReturnType();
			this.setBytes(cont, returnType, cont.getResponse(), cont.getReturnResult());
		}
	}

	/* (non-Javadoc)
	 * @see com.trendrr.strest.server.StrestControllerFilter#error(com.trendrr.strest.server.StrestController, org.jboss.netty.handler.codec.http.HttpResponse, java.lang.Exception)
	 */
	@Override
	public void error(CheshireApiController controller, HttpResponse response,
			Exception exception) {
		String type = "json";
		if (controller != null) {
			type = ((CheshireApiController)controller).getReturnType();
		}
		this.setBytes(controller, type, response, new DynMap());
	}
	
	private void setBytes(CheshireApiController controller, String type, HttpResponse response, DynMap val) {
		if (type == null)
			type = "json";
		
		//automatically add the status portion of the return.
		if (val == null)
			val = new DynMap();
		
		DynMap status = val.get(DynMap.class, "status", new DynMap());
		status.putIfAbsent("message", response.getStatus().getReasonPhrase());
		status.put("code", response.getStatus().getCode());	
		if (controller != null && !controller.getWarnings().isEmpty()) {
			status.put("warnings", controller.getWarnings());
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
