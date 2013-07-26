/**
 * 
 */
package com.trendrr.cheshire.client.netty4;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.channel.ChannelFutureListener;

import com.trendrr.cheshire.client.CheshireListenableFuture;
import com.trendrr.cheshire.client.json.CheshireNettyClient;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.concurrent.Sleep;
import com.trendrr.oss.exceptions.TrendrrDisconnectedException;
import com.trendrr.oss.strest.cheshire.CheshireApiCallback;
import com.trendrr.oss.strest.cheshire.Verb;
import com.trendrr.oss.strest.models.StrestRequest;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Dustin Norlander
 * @created Jul 25, 2013
 * 
 */
public class CheshireNetty4Client extends com.trendrr.cheshire.client.CheshireClient {
	
	protected Channel channel;
	
	protected ExecutorService executor;
	
	protected ConcurrentHashMap<String, CheshireListenableFuture> futures = new ConcurrentHashMap<String, CheshireListenableFuture>();
	
	
	public static void main(String ...args) throws Exception {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(
				1, // core size
			    50, // max size
			    60, // idle timeout
			    TimeUnit.SECONDS,
			    new SynchronousQueue<Runnable>(), // queue with a size
			    new ThreadPoolExecutor.CallerRunsPolicy() //if queue is full run in current thread.
		);
		
		CheshireNetty4Client c = new CheshireNetty4Client("localhost", 8009, executor);
		c.connect();
		
		c.apiCall("/ping", Verb.GET, null, new CheshireApiCallback() {
			@Override
			public void error(Throwable arg0) {
				arg0.printStackTrace();
			}

			@Override
			public void response(DynMap res) {
				System.out.println("Recieved response " + res);
			}

			@Override
			public void txnComplete(String arg0) {
			
			}
		});
		
		Sleep.seconds(20);
	}
	
	/**
	 * @param host
	 * @param port
	 */
	public CheshireNetty4Client(String host, int port, ExecutorService service) {
		super(host, port);
		this.executor = service;
	}
	
	
	protected static Log log = LogFactory.getLog(CheshireNetty4Client.class);
	
	public void connect() throws Exception {
		this.connect(new NioEventLoopGroup()); 
	}
	
	public void connect(EventLoopGroup group) throws Exception {
		Bootstrap b = new Bootstrap()
			.group(group)
            .channel(NioSocketChannel.class)
            .handler(new Initializer());
        
        // Start the connection attempt.
        this.channel = b.connect(host, port).sync().channel();
        
        //send hello
        
	}
	
	/* (non-Javadoc)
	 * @see com.trendrr.oss.strest.cheshire.CheshireApiCaller#close()
	 */
	@Override
	public void close() {
		//alert all the waiting transactions.
		
		
		
		this.channel.close();
	}
	
	static AtomicLong l = new AtomicLong(0l);
	protected String txnId() {
		return Long.toString(l.incrementAndGet());
	}
	
	public boolean isConnected() {
		return this.channel.isActive();
	}
	
	/* (non-Javadoc)
	 * @see com.trendrr.cheshire.client.CheshireClient#apiCall(com.trendrr.oss.strest.models.StrestRequest)
	 */
	@Override
	public CheshireListenableFuture apiCall(StrestRequest req) {
		String txnId = this.txnId();
		req.setTxnId(txnId);
		final CheshireListenableFuture sf = new CheshireListenableFuture(this.executor);
		
		this.futures.put(txnId, sf);
		
		//make sure this connection is not closed
		if (!this.channel.isActive()) {
			sf.setException(new TrendrrDisconnectedException("Connection is closed"));
			this.close();
			return sf;
		}		
		
		ChannelFuture channelFuture = this.channel.writeAndFlush(req);
		
		
		channelFuture.addListener(new io.netty.channel.ChannelFutureListener() {
			
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (!future.isSuccess()) {
					Exception ex = toTrendrrException(future.cause());
					if (ex instanceof TrendrrDisconnectedException) {
						close();
					}
					//set the exception..
					sf.setException(ex);
				}
			}
		});
		
		return sf;
	}
}
