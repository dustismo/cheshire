/**
 * 
 */
package com.trendrr.cheshire.client.sharded.exceptions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.oss.exceptions.TrendrrException;


/**
 * @author Dustin Norlander
 * @created Aug 22, 2013
 * 
 */
public class CheshireNoShardParams extends TrendrrException {

	protected static Log log = LogFactory.getLog(CheshireNoShardParams.class);
}
