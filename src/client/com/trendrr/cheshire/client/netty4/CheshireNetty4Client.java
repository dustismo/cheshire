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
import com.trendrr.oss.exceptions.TrendrrException;
import com.trendrr.oss.strest.cheshire.CheshireApiCallback;
import com.trendrr.oss.strest.cheshire.Verb;
import com.trendrr.oss.strest.models.DefaultStrestRequest;
import com.trendrr.oss.strest.models.StrestHeader;
import com.trendrr.oss.strest.models.StrestHello;
import com.trendrr.oss.strest.models.StrestRequest;
import com.trendrr.oss.strest.models.StrestResponse;
import com.trendrr.oss.strest.models.StrestHeader.TxnStatus;
import com.trendrr.oss.strest.models.json.StrestJsonRequest;
import com.trendrr.strest.doc.StrestDocParser;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;
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
	
	
	public static enum PROTOCOL {
		JSON,
		BINARY
	}
	
	public static String USER_AGENT = "java-netty4-1.0";
	
	protected Channel channel;
	
	protected ExecutorService executor;
	
	protected ConcurrentHashMap<String, CheshireListenableFuture> futures = new ConcurrentHashMap<String, CheshireListenableFuture>();
	
	protected Timer timer = null; //timer for keepalive pings
	
	//The max number of requests allowed to be waiting on return values.
	protected int MAX_INFLIGHT = 500;
	
	public static final AttributeKey<CheshireNetty4Client> CLIENT_KEY = new AttributeKey<CheshireNetty4Client>("client");
	/**
	 * how long to wait when max inflight reached.
	 */
	protected int MAX_INFLIGHT_WAIT_SECONDS = 30;
	
	protected PROTOCOL protocol;
		
	public static void main(String ...args) throws Exception {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(
				1, // core size
			    50, // max size
			    60, // idle timeout
			    TimeUnit.SECONDS,
			    new SynchronousQueue<Runnable>(), // queue with a size
			    new ThreadPoolExecutor.CallerRunsPolicy() //if queue is full run in current thread.
		);
		
		CheshireNetty4Client c = new CheshireNetty4Client("platform.trendrr.com", 8009, executor, PROTOCOL.JSON);
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
	public CheshireNetty4Client(String host, int port, ExecutorService service, PROTOCOL protocol) {
		super(host, port);
		this.executor = service;
		this.protocol = protocol;
	}
	
	
	protected static Log log = LogFactory.getLog(CheshireNetty4Client.class);
	
	@Override
	protected StrestRequest newRequest() {
		if (this.protocol == PROTOCOL.JSON) {
			return  new StrestJsonRequest();
		} else {
			return new DefaultStrestRequest();
		}
	}
	
	public void connect() throws Exception {
		this.connect(new NioEventLoopGroup()); 
	}
	
	public void connect(EventLoopGroup group) throws Exception {
		Bootstrap b = new Bootstrap()
			.group(group)
            .channel(NioSocketChannel.class)
            .handler(new Initializer(this.protocol));
        
        // Start the connection attempt.
        this.channel = b.connect(host, port).sync().channel();
        
        Attribute<CheshireNetty4Client> attr = this.channel.attr(CLIENT_KEY);
        attr.set(this);
        
        //TODO: send hello
        StrestHello hello = new StrestHello();
        hello.setUserAgent(USER_AGENT);
        hello.setVersion(StrestHeader.STREST_VERSION);
        
        this.channel.writeAndFlush(hello);
        
      //start the ping timer.
		final CheshireNetty4Client self = this;
		this.timer = new Timer(true);
		this.timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					self.ping();
				} catch (TrendrrDisconnectedException x) {
					self.close();
				} catch (com.trendrr.oss.exceptions.TrendrrTimeoutException x) {
					//timeount on a ping is baaad
					self.close();
				} catch (TrendrrException x) {
					log.error("Caught", x);
				}
			}
		}, 1000* (int)(Math.random()*30), 1000*30);
	}
	
	
	/**
	 * handle an incoming response
	 * @param response
	 */
	void incoming(StrestResponse response) {
		
		String txnId = response.getTxnId();
		CheshireListenableFuture cb = futures.get(txnId);
		if (cb == null) {
			log.error("SERVER SENT Response to Transaction: " + txnId + " Which is either closed or doesn't exist!");
			return;
		}
		cb.set(response);
		TxnStatus txnStat = response.getTxnStatus();
		if (txnStat != TxnStatus.CONTINUE) {
			this.futures.remove(txnId);
			if (cb.getCallback() != null) {
				cb.getCallback().txnComplete(txnId);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.trendrr.oss.strest.cheshire.CheshireApiCaller#close()
	 */
	@Override
	public synchronized void close() {
		//cancel the ping timer
		this.timer.cancel();
		
		//close the channel
		this.channel.close();
		
		//alert all the waiting transactions.
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
	public CheshireListenableFuture apiCall(StrestRequest req) throws TrendrrDisconnectedException {
		String txnId = this.txnId();
		req.setTxnId(txnId);
		
		
		//make sure this connection is not closed
		if (!this.channel.isActive()) {
			this.close();
			throw new TrendrrDisconnectedException("Connection is closed");
		}		
		
		//now check max inflight
		//let pings slip through the max inflight
		if (!req.getUri().equals(this.pingEndpoint)) {
			
			int i=0;
			while(i < this.MAX_INFLIGHT_WAIT_SECONDS && this.futures.size() >= this.MAX_INFLIGHT) {
				if (i % 10 == 0 && i != 0) {
					log.warn("Max INFLIGHT reached (" + this.MAX_INFLIGHT +") waiting one second");
				}
				Sleep.seconds(1);
				i++;
			}
			if (i >= this.MAX_INFLIGHT_WAIT_SECONDS) {
				log.error("Waited " + this.MAX_INFLIGHT_WAIT_SECONDS + " seconds for inflight to go down. no luck.  WTF?");
				this.close();
				throw new TrendrrDisconnectedException("Waited " + this.MAX_INFLIGHT_WAIT_SECONDS + " seconds for inflight to go down. no luck.  WTF?");
			}
		}
		
		
		final CheshireListenableFuture sf = new CheshireListenableFuture(this.executor);
		this.futures.put(txnId, sf);
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
