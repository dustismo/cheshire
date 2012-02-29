/**
 * 
 */
package com.trendrr.cheshire;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.strest.server.StrestController;


/**
 * 
 * Base class to serve html content.
 * 
 * @author Dustin Norlander
 * @created Feb 29, 2012
 * 
 */
public class CheshireHTMLController extends StrestController {

	protected Log log = LogFactory.getLog(CheshireHTMLController.class);
	
	public boolean enableSessions() {
		if (this.isAnnotationPresent()) {
			return this.getAnnotationVal(Boolean.class, "enableSessions");
		}
		return true;
	}
}
