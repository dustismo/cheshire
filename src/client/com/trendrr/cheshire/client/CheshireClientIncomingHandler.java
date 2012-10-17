/**
 * 
 */
package com.trendrr.cheshire.client;

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
public class CheshireClientIncomingHandler extends SimpleChannelUpstreamHandler {

	protected static Log log = LogFactory.getLog(CheshireClientIncomingHandler.class);
	
	CheshireNettyClient client;
	
	CheshireClientIncomingHandler(CheshireNettyClient client) {
		this.client = client;
	}
	
	@Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		StrestResponse response = (StrestResponse)e.getMessage();
		this.client.incoming(response);
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
//	    	log.info("Disconnect! " + ctx);    	
    	client.disconnected();
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
    	log.warn("Caught", e.getCause());
        e.getChannel().close();
        client.disconnected();
    }
}
