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
		route = "/error/:code",
		authenticate = false
)
public class ErrorPagesController extends CheshireHTMLController {

	protected Log log = LogFactory.getLog(ErrorPagesController.class);
	
	@Override
	public void handleGET(DynMap params) throws Exception {
		this.render("errors/" + params.getString("code"), params);
	}
}
