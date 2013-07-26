/**
 * 
 */
package com.trendrr.cheshire.client.netty4;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.oss.DynMap;
import com.trendrr.oss.strest.models.json.StrestJsonRequest;


/**
 * @author Dustin Norlander
 * @created May 4, 2012
 * 
 */
public class CheshireJsonEncoder extends MessageToByteEncoder<StrestJsonRequest> {

	protected static Log log = LogFactory.getLog(CheshireJsonEncoder.class);

	@Override 
	public void encode(ChannelHandlerContext ctx, StrestJsonRequest request, ByteBuf out) throws Exception {
		
		System.out.println("Encoding: " + request);
		out.writeBytes(request.toByteArray());
		
	}
}
