/**
 * 
 */
package com.trendrr.cheshire.client.json;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.util.CharsetUtil;

import com.trendrr.oss.DynMap;
import com.trendrr.oss.strest.models.json.StrestJsonRequest;


/**
 * @author Dustin Norlander
 * @created May 4, 2012
 * 
 */
public class CheshireJsonEncoder extends SimpleChannelHandler {

	protected static Log log = LogFactory.getLog(CheshireJsonEncoder.class);

	@Override
	public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		Object msg = e.getMessage();
		if (msg instanceof StrestJsonRequest) {
			StrestJsonRequest req = (StrestJsonRequest)msg;
			Channels.write(ctx, e.getFuture(), 
					ChannelBuffers.wrappedBuffer(req.getMap().toJSONString().getBytes(CharsetUtil.UTF_8))
					);
		} else if (msg instanceof DynMap) {
			Channels.write(ctx, e.getFuture(), 
					ChannelBuffers.wrappedBuffer(((DynMap)msg).toJSONString().getBytes(CharsetUtil.UTF_8))
					);
		}
	}
}
