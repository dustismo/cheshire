/**
 * 
 */
package com.trendrr.cheshire.controllers.html;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.cheshire.CheshireHTML;
import com.trendrr.cheshire.CheshireHTMLController;
import com.trendrr.oss.DynMap;


@CheshireHTML(
		route = "/logout",
		authenticate = false,
		enableSessions = true
)
public class LogoutController extends CheshireHTMLController {

	protected static Log log = LogFactory.getLog(LogoutController.class);
	
	public void handleGET(DynMap params) throws Exception {
		this.getConnectionStorage().put("session_destroy", true);	
		this.render("/", params);
	}
}
