/**
 * 
 */
package com.trendrr.cheshire.client.netty4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.util.CharsetUtil;

import java.nio.charset.Charset;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.json.stream.JSONStreamParser;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.FileHelper;
import com.trendrr.oss.strest.models.json.StrestJsonResponse;


/**
 * @author Dustin Norlander
 * @created May 4, 2012
 * 
 */
public class CheshireJsonDecoder extends DelimiterBasedFrameDecoder {

	protected static Log log = LogFactory.getLog(CheshireJsonDecoder.class);

	JSONStreamParser parser = new JSONStreamParser();

	public CheshireJsonDecoder() {
		super((int)FileHelper.megsToBytes(2),  
				false,
				Unpooled.wrappedBuffer(new byte[] { '}' }));
	}
	
	/* (non-Javadoc)
	 * @see org.jboss.netty.handler.codec.frame.FrameDecoder#decode(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, org.jboss.netty.buffer.ChannelBuffer)
	 */
	@Override
	protected Object decode(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
		
		Object result = super.decode(ctx, buf);
		System.out.println(result);
		
		while(result != null) {
			
			
			StringCharacterIterator iter = new StringCharacterIterator(((ByteBuf)result).toString(CharsetUtil.UTF_8));
			for(char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
		         DynMap val = parser.addChar(c);
		         if (val != null) {
		        	 return new StrestJsonResponse(val);
		         }
		    }
			result = super.decode(ctx, buf);
		}
		return null;
	}
}
