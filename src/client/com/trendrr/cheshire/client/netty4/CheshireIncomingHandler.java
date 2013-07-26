/**
 * 
 */
package com.trendrr.cheshire.client.netty4;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.cheshire.client.json.CheshireNettyClient;
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
		CheshireNetty4Client client = ctx.channel().attr(CheshireNetty4Client.CLIENT_KEY).get();
		if (client != null) {
			client.incoming(response);
		} else {
			log.warn("No channel set for : " + ctx.channel());
			//TODO: should we kill the channel?
		}
	}

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn("Unexpected exception from downstream.", cause);
        ctx.close();
    }
}
