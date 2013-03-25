/**
 * 
 */
package com.trendrr.cheshire.client.json;

import static org.jboss.netty.channel.Channels.pipeline;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.NoSuchElementException;
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
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
import com.trendrr.oss.concurrent.Sleep;
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
 * 
 * A fast netty based client for cheshire.  This does not handle reconnects automatically.  It is suggested that you use the CheshirePooledClient
 * 
 * 
 * @author Dustin Norlander
 * @created Oct 17, 2012
 * 
 */
public class CheshireNettyClient extends com.trendrr.cheshire.client.CheshireClient{

	protected static Log log = LogFactory.getLog(CheshireNettyClient.class);
	
	protected ConcurrentHashMap<String, CheshireListenableFuture> futures = new ConcurrentHashMap<String, CheshireListenableFuture>();
	protected Channel channel = null;
	
	protected boolean keepalive = false;
	
	protected Timer timer = null; //timer for keepalive pings
	
	protected AtomicBoolean isClosed = new AtomicBoolean(true);
	
	protected AtomicLong lastConnectAttempt = new AtomicLong(0l);
	 
	//The max number of requests allowed to be waiting on return values.
	protected int MAX_INFLIGHT = 500;
	
	protected ReentrantReadWriteLock connectionLock = new ReentrantReadWriteLock();
	
	
	public synchronized boolean isKeepalive() {
		return keepalive;
	}
	
	/**
	 * setting this to true will keep the connection open.  and send ping messages every 10-20 seconds.
	 * 
	 * is true by default.
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
//						try {
//							self.connect();
//						} catch (Exception e) {
//							
//						}
						self.close();
					} catch (com.trendrr.oss.exceptions.TrendrrTimeoutException x) {
						//timed out.  reconnect.
						//make one reconnect attempt. 
//						try {
//							self.connect();
//						} catch (Exception e) {
//							
//						}
						self.close();
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
		this.close();
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
		this.connectionLock.writeLock().lock();
		try {
			
			if (this.channel != null) {
		    	try {
		    		this.doClose();
		    	} catch (Exception x) {
		    		log.warn("Caught", x);
		    	}
		    }
			
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
		    ChannelFuture future = bootstrap.connect(new InetSocketAddress(host,
	                port));
		    future.awaitUninterruptibly(40000);
		    
		    if (!future.isSuccess()) {
		    	throw toTrendrrException(future.getCause());
		    }
		    
		    this.channel = future.getChannel();
		    this.channel.setAttachment(this);
		    this.isClosed.set(false);
		    this.setKeepalive(true);
		} finally {
			this.connectionLock.writeLock().unlock();
		}
	}
	
//	protected TrendrrLock lock = new TrendrrLock();
	
	
	@Override	
	public CheshireListenableFuture apiCall(StrestRequest req) {
		this.connectionLock.readLock().lock();
		try {
			String txnId = this.txnId();
			req.setTxnId(txnId);
			final CheshireListenableFuture sf = new CheshireListenableFuture(workerThreadpool);
			
			int i=0;
			while(i < 30 && this.futures.size() >= this.MAX_INFLIGHT) {
				if (i % 10 == 0 && i != 0) {
					log.warn("Max INFLIGHT reached (" + this.MAX_INFLIGHT +") waiting one second");
				}
				Sleep.seconds(1);
				i++;
			}
			if (i == 30) {
				log.error("Waited 30 seconds for inflight to go down. no luck.  WTF?");
				sf.setException(new TrendrrDisconnectedException("Waited 30 seconds for inflight to go down. no luck.  WTF?"));
				return sf;
			}
			
			//make sure this connection is not closed
			if (this.isClosed()) {
				sf.setException(new TrendrrDisconnectedException("Connection is closed"));
				return sf;
			}
			
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
		} finally {
			this.connectionLock.readLock().unlock();
		}
	}
	
	private void doClose() {
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
		this.setKeepalive(false);
		
		//drain the futures in a threadsafe way.
		while(!this.futures.isEmpty()) {
			try {
				String key = this.futures.keys().nextElement();
				CheshireListenableFuture fut = this.futures.remove(key);
				if (fut != null) {
					fut.setException(new TrendrrDisconnectedException("Connection Broken"));
				}	
			} catch (NoSuchElementException x) {
				
			}
		}
	}
	/**
	 * closes this connection
	 */
	public void close() {
		try {
			this.connectionLock.writeLock().lock();
			this.doClose();
		} finally {
			this.connectionLock.writeLock().unlock();
		}
	}
	

	
	static AtomicLong l = new AtomicLong(0l);
	protected String txnId() {
		return Long.toString(l.incrementAndGet());
	}
	
	
}
