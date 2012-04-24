/**
 * 
 */
package com.trendrr.cheshire.controllers.html;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.trendrr.cheshire.CheshireHTML;
import com.trendrr.cheshire.CheshireHTMLController;
import com.trendrr.oss.DynMap;
import com.trendrr.strest.StrestException;


@CheshireHTML(
		route = "/errors/:code",
		authenticate = false
)
public class ErrorPagesController extends CheshireHTMLController {

	protected static Logger log = LoggerFactory.getLogger(ErrorPagesController.class);
	
	@Override
	public void handleGET(DynMap params) throws Exception {
		try {
			this.render("errors/" + params.getString("code"), params);
		} catch (StrestException x) {
			//need to avoid infinite loops 
			log.error("Caught", x);
		}
	}
}
