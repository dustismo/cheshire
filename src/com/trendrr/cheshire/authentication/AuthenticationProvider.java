/**
 * 
 */
package com.trendrr.cheshire.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.trendrr.strest.StrestException;
import com.trendrr.strest.server.StrestController;


/**
 * 
 * Implements a single authentication scheme.  a single instance will be shared across multiple invokations so this should not hold any state.  
 * 
 * @author Dustin Norlander
 * @created Jan 27, 2012
 * 
 */
public interface AuthenticationProvider {

	public AuthToken authenticate(StrestController controller) throws StrestException;
}
