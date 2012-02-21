/**
 * 
 */
package com.trendrr.cheshire;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * @author Dustin Norlander
 * @created Feb 21, 2012
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CheshireApi {
		//copy and paste from Strest annotation since
		//you cant extend an annotation for some reason.
		String[] route() default "";
		Class[] filters() default {};
		
		/**
		 * these params are required.
		 * @return
		 */
		String[] requiredParams() default {};
		
		boolean authenticate() default true;
		String[] access() default {};
		String[] returnTypes() default {"json","xml"};
		
		/**
		 * timeout of cache in seconds.
		 * @return
		 */
		int cacheTimeoutSeconds() default 0;
		boolean enableSessions() default false;
}
