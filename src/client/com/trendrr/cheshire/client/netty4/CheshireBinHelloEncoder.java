/**
 * 
 */
package com.trendrr.cheshire.client.netty4;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.oss.strest.models.StrestHeader;
import com.trendrr.oss.strest.models.StrestHello;
import com.trendrr.oss.strest.models.StrestRequest;


/**
 * @author Dustin Norlander
 * @created Jul 29, 2013
 * 
 */
public class CheshireBinHelloEncoder extends MessageToByteEncoder<StrestHello> {

	protected static Log log = LogFactory.getLog(CheshireBinHelloEncoder.class);
	
	@Override 
	public void encode(ChannelHandlerContext ctx, StrestHello hello, ByteBuf out) throws Exception {
		System.out.println("HELLO!");
		out.writeByte(StrestHeader.ParamEncoding.JSON.getBinary());
		BinaryHelper.writeString(out, hello.toJSONString());
	}
}
