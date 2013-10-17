/**
 * 
 */
package com.trendrr.cheshire.client.sharded;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * @author Dustin Norlander
 * @created Aug 23, 2013
 * 
 */
public class Test {

	protected static Log log = LogFactory.getLog(Test.class);
	
	public static void main(String ...strings) throws Exception {
		CheshireShardClient client = new CheshireShardClient("http://localhost:8020/");
		client.connect();
		
	}
}
