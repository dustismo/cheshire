/**
 * 
 */
package com.trendrr.cheshire.client.json;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import com.trendrr.oss.DynMap;
import com.trendrr.oss.strest.models.StrestResponse;


/**
 * @author Dustin Norlander
 * @created Oct 17, 2012
 * 
 */
@Deprecated
public class CheshireClientIncomingHandler extends SimpleChannelUpstreamHandler {

	protected static Log log = LogFactory.getLog(CheshireClientIncomingHandler.class);
	
	CheshireClientIncomingHandler() {
		
	}
	
	@Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		StrestResponse response = (StrestResponse)e.getMessage();
		
		CheshireNettyClient client = (CheshireNettyClient)e.getChannel().getAttachment();
		if (client != null) {
			client.incoming(response);
		} else {
			log.warn("No channel set for : " + e.getChannel());
			//TODO: should we kill the channel?
		}
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
    	CheshireNettyClient client = (CheshireNettyClient)e.getChannel().getAttachment();
		if (client != null) {
			client.disconnected();
		} else {
			log.warn("No channel set for : " + e.getChannel());
			//TODO: should we kill the channel?
		}
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
    	log.warn("Caught", e.getCause());
        e.getChannel().close();
        
        CheshireNettyClient client = (CheshireNettyClient)e.getChannel().getAttachment();
		if (client != null) {
			client.disconnected();
		} else {
			log.warn("No channel set for : " + e.getChannel());
			//TODO: should we kill the channel?
		}
    }
}
