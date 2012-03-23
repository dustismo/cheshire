/**
 * 
 */
package com.trendrr.cheshire.controllers.html;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.trendrr.cheshire.CheshireHTML;
import com.trendrr.cheshire.CheshireHTMLController;
import com.trendrr.oss.DynMap;


@CheshireHTML(
		route = "/error/:code",
		authenticate = false
)
public class ErrorPagesController extends CheshireHTMLController {

	protected static Logger log = LoggerFactory.getLogger(ErrorPagesController.class);
	
	@Override
	public void handleGET(DynMap params) throws Exception {
		this.render("errors/" + params.getString("code"), params);
	}
}
