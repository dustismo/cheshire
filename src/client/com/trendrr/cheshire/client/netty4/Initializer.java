/**
 * 
 */
package com.trendrr.cheshire.client.netty4;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * @author Dustin Norlander
 * @created Jul 25, 2013
 * 
 */
class Initializer extends ChannelInitializer<SocketChannel> {

	protected static Log log = LogFactory.getLog(Initializer.class);

	private CheshireJsonDecoder DECODER = new CheshireJsonDecoder();
    private CheshireJsonEncoder ENCODER = new CheshireJsonEncoder();
    
	/* (non-Javadoc)
	 * @see io.netty.channel.ChannelInitializer#initChannel(io.netty.channel.Channel)
	 */
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
        // Add the text line codec combination first,
        pipeline.addLast("decoder", DECODER);
        pipeline.addLast("encoder", ENCODER);

        // and then business logic.
        pipeline.addLast("handler", new CheshireIncomingHandler());
	}
}
