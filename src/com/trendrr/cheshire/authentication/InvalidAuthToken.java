/**
 * 
 */
package com.trendrr.cheshire.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * Providers can call this in cases where the authentication params are invalid.
 * 
 * Is a standard auth token that doesn't allow access to anything.  This way you can 
 * take advantage of token caching when getting slammed by bogus auth.  Using this is in no way 
 * required, but useful when authentication is a heavyweight process. 
 * 
 * 
 * @author Dustin Norlander
 * @created Jan 31, 2012
 * 
 */
public class InvalidAuthToken extends AuthToken {

	protected static Logger log = LoggerFactory.getLogger(InvalidAuthToken.class);
	
	@Override
	public boolean hasAccessToRoute(String route) {
		return false;
	}
}
