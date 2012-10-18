/**
 * 
 */
package com.trendrr.cheshire.client;

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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import com.trendrr.oss.DynMap;
import com.trendrr.oss.concurrent.LazyInit;
import com.trendrr.oss.concurrent.LazyInitObject;
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
public class CheshireNettyClient implements CheshireApiCaller{

	protected static Log log = LogFactory.getLog(CheshireNettyClient.class);
	
	protected String host;
	protected int port;
	protected int connectionPoolSize = 1;
	protected ConcurrentHashMap<String, CheshireListenableFuture> futures = new ConcurrentHashMap<String, CheshireListenableFuture>();
	protected Channel channel;
	
	protected boolean keepalive = false;
	
	protected Date lastSuccessfulPing = new Date();
	
	protected Timer timer = null; //timer for keepalive pings
	
	public synchronized boolean isKeepalive() {
		return keepalive;
	}
	
	/**
	 * the date of the last successful ping.  could be null
	 * @return
	 */
	public Date getLastSuccessfulPing() {
		return lastSuccessfulPing;
	}
	
	public synchronized void setLastSuccessfullPing(Date d) {
		this.lastSuccessfulPing = d;
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
		this.host = host;
		this.port = port;
	}
	
	
	/**
	 * Does a synchronous ping.  will throw an exception.  This method will *NOT* trigger a reconnect attempt. 
	 * @throws Exception
	 */
	public void ping() throws TrendrrException {
		this.apiCall("/ping", Verb.GET, null, 5*1000);
		this.setLastSuccessfullPing(new Date());
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
	
	public void connect() {
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
							    new ArrayBlockingQueue<Runnable>(60), // queue with a size
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
	    //TODO: handle connection pooling
	    ChannelFuture future = bootstrap.connect(new InetSocketAddress(host,
                port));
	    future.awaitUninterruptibly(30000);
	    
	    if (!future.isSuccess()) {
	    	future.getCause().printStackTrace();
	    	log.error("Caught", future.getCause());
	    	return;
	    }
	    this.channel = future.getChannel();
	    this.channel.setAttachment(this);
	}
	
	
	
	
	/**
	 * Does an asynchronous api call.  This method returns immediately. the Response or error is sent to the callback.
	 * 
	 * 
	 * @param endPoint
	 * @param method
	 * @param params
	 * @param callback
	 */
	public void apiCall(String endPoint, Verb method, Map params, CheshireApiCallback callback) {
		StrestRequest req = this.createRequest(endPoint, method, params);
		req.setTxnAccept(TxnAccept.MULTI);
		CheshireListenableFuture fut = this.apiCall(req);
		fut.setCallback(callback);
		return;
	}
	
	/**
	 * A synchronous call.  blocks until response is available.  Please note that this does *NOT* block concurrent api calls, so you can continue to 
	 * make calls in other threads.
	 * 
	 * If the maxReconnectAttempts is non-zero (-1 is infinit reconnect attempts), then this will attempt to reconnect and send on any io problems. 
	 * 
	 * @param endPoint
	 * @param method
	 * @param params
	 * @param timeoutMillis throw an exception if this # of millis passes,  < 1 should be infinite.
	 * @return
	 * @throws Exception
	 */
	public DynMap apiCall(String endPoint, Verb method, Map params, long timeoutMillis) throws TrendrrTimeoutException, TrendrrException {
		StrestRequest req = this.createRequest(endPoint, method, params);
		req.setTxnAccept(TxnAccept.SINGLE);
		CheshireListenableFuture fut = this.apiCall(req);
		try {
			return fut.get(timeoutMillis, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new TrendrrTimeoutException(e);
		} catch (TimeoutException e) {
			throw new TrendrrTimeoutException(e);
		} catch (ExecutionException e) {
			throw new TrendrrException(e);
		}
	}
	
	
	public CheshireListenableFuture apiCall(StrestRequest req) {
		String txnId = this.txnId();
		req.setTxnId(txnId);
		
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
					sf.setException(future.getCause());
				}
			}
		}); 
		
		return sf;
	}
	/**
	 * closes this connection
	 */
	public void close() {
		try {
			this.channel.close().awaitUninterruptibly(10, TimeUnit.SECONDS);
		} catch (Exception x) {
			log.error("Close Exception", x);
		}
	}
	

	
	static AtomicLong l = new AtomicLong(0l);
	protected String txnId() {
		return Long.toString(l.incrementAndGet());
	}
	
	protected StrestRequest createRequest(String endPoint, Verb method, Map params) {
		StrestJsonRequest request = new StrestJsonRequest();
		request.setUri(endPoint);
		request.setMethod(Method.instance(method.toString())); //TODO:this is stuuupid
		if (params != null) {
			DynMap pms = null;
			if (params instanceof DynMap){
				pms = (DynMap)params;
			} else {
				pms = DynMap.instance(params);
			}
			request.setParams(pms);
		}
		return request;
	}
	
	
	/**
	 * gets the host address
	 * @return
	 */
	public String getHost() {
		return this.host;
	}
	
	/**
	 * gets the cheshire port
	 * @return
	 */
	public int getPort() {
		return this.port;
	}
}
