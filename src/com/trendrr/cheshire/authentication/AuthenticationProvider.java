/**
 * 
 */
package com.trendrr.cheshire.authentication;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.strest.StrestException;
import com.trendrr.strest.server.StrestController;


/**
 * @author Dustin Norlander
 * @created Jan 27, 2012
 * 
 */
public interface AuthenticationProvider {

	public AuthToken authenticate(StrestController controller) throws StrestException;
}
