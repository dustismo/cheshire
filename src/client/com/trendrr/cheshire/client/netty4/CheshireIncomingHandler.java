/**
 * 
 */
package com.trendrr.cheshire.client.netty4;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.oss.strest.models.StrestResponse;


/**
 * @author Dustin Norlander
 * @created Jul 25, 2013
 * 
 */
class CheshireIncomingHandler extends SimpleChannelInboundHandler<StrestResponse> {

	protected static Log log = LogFactory.getLog(CheshireIncomingHandler.class);
	
	 @Override
    protected void channelRead0(ChannelHandlerContext ctx, StrestResponse response) throws Exception {
		 //TODO: handle the response
		 
		 System.err.println(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn("Unexpected exception from downstream.", cause);
        ctx.close();
    }
}
