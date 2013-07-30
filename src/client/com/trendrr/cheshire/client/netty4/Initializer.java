/**
 * 
 */
package com.trendrr.cheshire.client.netty4;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * @author Dustin Norlander
 * @created Jul 25, 2013
 * 
 */
class Initializer extends ChannelInitializer<SocketChannel> {

	protected static Log log = LogFactory.getLog(Initializer.class);

	CheshireNetty4Client.PROTOCOL protocol;
	
    public Initializer(CheshireNetty4Client.PROTOCOL protocol) {
    	this.protocol = protocol;
    }
    
	/* (non-Javadoc)
	 * @see io.netty.channel.ChannelInitializer#initChannel(io.netty.channel.Channel)
	 */
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
        // Add the text line codec combination first,
		
		if (this.protocol == CheshireNetty4Client.PROTOCOL.JSON) {
	        pipeline.addLast("decoder", new CheshireJsonDecoder());
	        pipeline.addLast("encoder", new CheshireJsonEncoder());
		} else if (this.protocol == CheshireNetty4Client.PROTOCOL.BINARY) {
			pipeline.addLast("decoder", new CheshireBinDecoder());
			pipeline.addLast("helloencoder", new CheshireBinHelloEncoder());
			pipeline.addLast("encoder", new CheshireBinEncoder());
		}
        // and then business logic.
        pipeline.addLast("handler", new CheshireIncomingHandler());
	}
}
