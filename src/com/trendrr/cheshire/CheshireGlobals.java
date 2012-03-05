package com.trendrr.cheshire;
/**
 * 
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * 
 * I know, I know, bad form.  fuck dependancy injection :)
 * @author Dustin Norlander
 * @created Mar 5, 2012
 * 
 */
public class CheshireGlobals {

	protected Log log = LogFactory.getLog(CheshireGlobals.class);
	
	public static String baseDir = "";
}
