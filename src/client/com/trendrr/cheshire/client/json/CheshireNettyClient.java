/**
 * 
 */
package com.trendrr.cheshire.client.json;

import static org.jboss.netty.channel.Channels.pipeline;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.trendrr.cheshire.client.CheshireListenableFuture;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.concurrent.LazyInit;
import com.trendrr.oss.concurrent.LazyInitObject;
import com.trendrr.oss.concurrent.TrendrrLock;
import com.trendrr.oss.exceptions.TrendrrDisconnectedException;
import com.trendrr.oss.exceptions.TrendrrException;
import com.trendrr.oss.exceptions.TrendrrTimeoutException;
import com.trendrr.oss.strest.StrestRequestCallback;
import com.trendrr.oss.strest.cheshire.*;
import com.trendrr.oss.strest.models.*;
import com.trendrr.oss.strest.models.StrestHeader.Method;
import com.trendrr.oss.strest.models.StrestHeader.TxnAccept;
import com.trendrr.oss.strest.models.StrestHeader.TxnStatus;
import com.trendrr.oss.strest.models.json.StrestJsonRequest;



/**
 * @author Dustin Norlander
 * @created Oct 17, 2012
 * 
 */
public class CheshireNettyClient extends com.trendrr.cheshire.client.CheshireClient{

	protected static Log log = LogFactory.getLog(CheshireNettyClient.class);
	
	protected ConcurrentHashMap<String, CheshireListenableFuture> futures = new ConcurrentHashMap<String, CheshireListenableFuture>();
	protected Channel channel;
	
	protected boolean keepalive = false;
	
	protected Timer timer = null; //timer for keepalive pings
	
	protected AtomicBoolean isClosed = new AtomicBoolean(true);
	
	protected AtomicLong lastConnectAttempt = new AtomicLong(0l);
	
	public synchronized boolean isKeepalive() {
		return keepalive;
	}
	
	
	
	/**
	 * setting this to true will keep the connection open.  
	 * 
	 * @param keepalive
	 */
	public synchronized void setKeepalive(boolean keepalive) {
		if (this.keepalive == keepalive) {
			return;
		}
		this.keepalive = keepalive;
		if (this.keepalive) {
			//start the timer.
			final CheshireNettyClient self = this;
			this.timer = new Timer(true);
			this.timer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					try {
						self.ping();
					} catch (TrendrrDisconnectedException x) {
						//make one reconnect attempt. 
						try {
							self.connect();
						} catch (Exception e) {
							
						}
					} catch (TrendrrException x) {
						log.error("Caught", x);
					}
				}
			}, 1000*1, 1000*30);
		} else {
			timer.cancel();
		}
	}
	
	
	
	public CheshireNettyClient(String host, int port) {
		super(host, port);
	}
	
	public boolean isClosed() {
		return this.isClosed.get();
	}
	
	void incoming(StrestResponse response) {
		//do something.. 
		
		String txnId = response.getTxnId();
		CheshireListenableFuture cb = futures.get(txnId);
		if (cb == null) {
			log.error("SERVER SENT Response to Transaction: " + txnId + " Which is either closed or doesn't exist!");
			return;
		}
		cb.set(DynMap.instance(response));
		TxnStatus txnStat = response.getTxnStatus();
		if (txnStat != TxnStatus.CONTINUE) {
			this.futures.remove(txnId);
			if (cb.getCallback() != null) {
				cb.getCallback().txnComplete(txnId);
			}
		}
	}
	
	void disconnected() {
		//TODO: do something...
		log.info("Announcing broken connection to callbacks: " + this.futures);
		for (CheshireListenableFuture fut : this.futures.values()) {
			log.info("CONNECTION BROKEN! : " + fut);
			fut.setException(new TrendrrDisconnectedException("Connection Broken"));
		}
		this.futures.clear();
	}
	
	void error(StrestRequest req, Throwable t) {
		req.getTxnId();
	}
	protected static ClientBootstrap bootstrap = null;
	protected static LazyInit bootstrapLock = new LazyInit();
	protected static ExecutorService ioThreadpool = null;
	protected static ExecutorService workerThreadpool = null;
	protected static Object threadpoolLock = new Object();
	
	/**
	 * sets the io threadpool.  this will throw an exception if at least one client has already
	 * been initialized (all clients share the same threadpools).
	 * 
	 * @param service
	 * @throws TrendrrException
	 */
	public static void setExecutors(ExecutorService io, ExecutorService workers) throws TrendrrException {
		if (bootstrapLock.isInited()) {
			throw new TrendrrException("Cant set ExecutorService because at least one client has been initialized");
		}
		synchronized(threadpoolLock) {
			ioThreadpool = io;
			workerThreadpool = workers;
		}
	}
	
	public synchronized void connect() throws TrendrrException {
		if (bootstrapLock.start()) {
			try {
				synchronized(threadpoolLock) {
					if (ioThreadpool == null) {
						ioThreadpool = Executors.newCachedThreadPool();
					}
					if (workerThreadpool == null) {
						
						workerThreadpool = new ThreadPoolExecutor(
								1, // core size
							    50, // max size
							    60, // idle timeout
							    TimeUnit.SECONDS,
							    new SynchronousQueue<Runnable>(), // queue with a size
							    new ThreadPoolExecutor.CallerRunsPolicy() //if queue is full run in current thread.
						);
					}
					
					
					
					ChannelFactory factory = new NioClientSocketChannelFactory(ioThreadpool, workerThreadpool);
				    final CheshireClientIncomingHandler handler = new CheshireClientIncomingHandler();
					
					ChannelPipelineFactory pipeline = new ChannelPipelineFactory() {
			            public ChannelPipeline getPipeline() throws Exception {
			            	 // Create a default pipeline implementation.
			                ChannelPipeline pipeline = pipeline();
			                
			        		pipeline.addLast("decoder", new CheshireJsonDecoder());
			                pipeline.addLast("encoder", new CheshireJsonEncoder());
			                pipeline.addLast("handler", handler);
			                return pipeline;
			            }
			        };
			
				    bootstrap = new ClientBootstrap(factory);
				    
				    // At client side option is tcpNoDelay and at server child.tcpNoDelay
				    bootstrap.setOption("tcpNoDelay", true);
				    bootstrap.setOption("keepAlive", true);
				    bootstrap.setOption("connectTimeoutMillis", 30000);
				    bootstrap.setPipelineFactory(pipeline);
				}
			} finally {
				bootstrapLock.end();
			}
		}
	    
		if (this.lastConnectAttempt.get() > new Date().getTime()-2000) {
			log.warn("Already tried connecting within past 2 seconds");
			return;
		}
			
		//TODO: handle connection pooling
	    ChannelFuture future = bootstrap.connect(new InetSocketAddress(host,
                port));
	    future.awaitUninterruptibly(40000);
	    
	    if (!future.isSuccess()) {
	    	throw toTrendrrException(future.getCause());
	    }
	    if (this.channel != null) {
	    	try {
	    		this.close();
	    	} catch (Exception x) {
	    		log.warn("Caught", x);
	    	}
	    }
	    this.channel = future.getChannel();
	    this.channel.setAttachment(this);
	    this.isClosed.set(false);
	}
	
	protected TrendrrLock lock = new TrendrrLock();
	
	@Override	
	public CheshireListenableFuture apiCall(StrestRequest req) {
		String txnId = this.txnId();
		req.setTxnId(txnId);
		
		if (channel == null || !channel.isConnected()) {
			lock.lockOrWait();
			try {
				if (channel == null || !channel.isConnected()) {
					//attempt reconnect
					try {
						this.connect();
					} catch (TrendrrException e) {
						log.error("Caught", e);
					}
				}
			} finally {
				lock.unlock();
			}
		}
		final CheshireListenableFuture sf = new CheshireListenableFuture(workerThreadpool);
		this.futures.put(txnId, sf);
		if (this.channel == null) {
			sf.setException(new TrendrrDisconnectedException("Not Connected"));
			return sf;
		}
		ChannelFuture channelFuture = this.channel.write(req);

		channelFuture.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (!future.isSuccess()) {
					//set the exception..
					sf.setException(toTrendrrException(future.getCause()));
				}
			}
		}); 
		
		return sf;
	}
	/**
	 * closes this connection
	 */
	public void close() {
		if (this.isClosed())
			return;
		this.isClosed.set(true);
		try {
			if (this.channel != null) {
				this.channel.close().awaitUninterruptibly(10, TimeUnit.SECONDS);
				this.channel = null;
			}
		} catch (Exception x) {
			log.error("Close Exception", x);
		}
		
	}
	

	
	static AtomicLong l = new AtomicLong(0l);
	protected String txnId() {
		return Long.toString(l.incrementAndGet());
	}
	
	
	
	
}
