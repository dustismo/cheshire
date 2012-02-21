/**
 * 
 */
package com.trendrr.cheshire.authentication;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * 
 * Providers can call this in cases where the authentication params are invalid.
 * 
 * Is a standard auth token that doesn't allow access to anything.  This way you can 
 * take advantage of token caching when getting slammed by bogus auth.
 * 
 * 
 * @author Dustin Norlander
 * @created Jan 31, 2012
 * 
 */
public class InvalidAuthToken extends AuthToken {

	protected Log log = LogFactory.getLog(InvalidAuthToken.class);
	
	@Override
	public boolean hasAccessToRoute(String route) {
		return false;
	}
}
