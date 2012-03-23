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
		route = "/",
		authenticate = false
)
public class IndexController extends CheshireHTMLController {

	protected static Logger log = LoggerFactory.getLogger(IndexController.class);
	
	@Override
	public void handleGET(DynMap params) throws Exception {
		this.render("index", params);
	}
}
