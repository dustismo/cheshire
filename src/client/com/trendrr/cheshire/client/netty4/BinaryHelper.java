/**
 * 
 */
package com.trendrr.cheshire.client.netty4;

import java.io.UnsupportedEncodingException;

import io.netty.buffer.ByteBuf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * @author Dustin Norlander
 * @created Jul 29, 2013
 * 
 */
public class BinaryHelper {

	protected static Log log = LogFactory.getLog(BinaryHelper.class);
	
	public static void writeString(ByteBuf out, String str) {
		if (str == null) {
			out.writeShort((short)0);
			return;
		}
		
		try {
			byte[] bytes = str.getBytes("utf8");
			out.writeShort((short)bytes.length);
			out.writeBytes(bytes);
		} catch (UnsupportedEncodingException e) {
			out.writeShort((short)0);
			log.error("Caught", e);
		}
	}
}
