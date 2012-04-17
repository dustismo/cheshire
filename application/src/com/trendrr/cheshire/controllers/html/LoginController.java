/**
 * 
 */
package com.trendrr.cheshire.controllers.html;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.trendrr.cheshire.CheshireHTML;
import com.trendrr.cheshire.CheshireHTMLController;
import com.trendrr.cheshire.authentication.AuthToken;
import com.trendrr.oss.DynMap;



@CheshireHTML(
		route = "/login",
		authenticate = false,
		enableSessions = true
)
public class LoginController extends CheshireHTMLController {

	protected static Logger log = LoggerFactory.getLogger(LoginController.class);
	
	public void handleGET(DynMap params) throws Exception {
		this.render("/login", params);
	}
	
	public void handlePOST(DynMap params) throws Exception {
		String username = params.getString("username");
		String password = params.getString("password");
		AuthToken token = null;
		
		if (username != null && password != null) {
			log.warn("Dummy login controller gave you ADMIN ACCESS!");
			//TODO: this is just a dummy token..
			token = new AuthToken();
			token.getUserAccessRoles().add("administrator");
			token.setUserId(username);
		}
		this.setAuthToken(token);
		//logged in!
		this.redirect(params.getString("forward", "/"));
	}
	

}
