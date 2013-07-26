/**
 * 
 */
package com.trendrr.cheshire.client.netty4;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.cheshire.client.CheshireClient;
import com.trendrr.cheshire.client.CheshireListenableFuture;
import com.trendrr.cheshire.client.netty4.Pool.Creator;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.exceptions.TrendrrClosedException;
import com.trendrr.oss.exceptions.TrendrrDisconnectedException;
import com.trendrr.oss.exceptions.TrendrrException;
import com.trendrr.oss.exceptions.TrendrrInitializationException;
import com.trendrr.oss.exceptions.TrendrrTimeoutException;
import com.trendrr.oss.strest.cheshire.CheshireApiCallback;
import com.trendrr.oss.strest.cheshire.CheshireApiCaller;
import com.trendrr.oss.strest.cheshire.Verb;
import com.trendrr.oss.strest.models.StrestRequest;


/**
 * @author Dustin Norlander
 * @created Jul 26, 2013
 * 
 */
public class CheshirePool extends CheshireClient {

	public static enum TYPE {
		JSON,
		BINARY
	}
	
	
	

	protected static Log log = LogFactory.getLog(CheshirePool.class);
	
	private Pool<CheshireClient> pool;
	protected CheshirePool.TYPE type;
	protected ExecutorService callbackExecutor;
	protected EventLoopGroup evloop;
	
	
	/**
	 * Internal client creator
	 */
	private static class ClientCreator implements Creator<CheshireClient> {
		CheshirePool parent;
		public ClientCreator(CheshirePool p) {
			this.parent = p;
		}
		@Override
		public CheshireClient create() throws Exception {
			if (this.parent.type == TYPE.JSON) {
				CheshireNetty4Client c = new CheshireNetty4Client(this.parent.host, this.parent.port, this.parent.callbackExecutor);
				c.connect(this.parent.evloop);
				return c;
			} 
			//TODO: Binary and http
			throw new TrendrrException("Unsupported type: " + this.parent.type);
		}
		@Override
		public void cleanup(CheshireClient obj) {
			obj.close();
		}		
	}
	
	/**
	 * @param host
	 * @param port
	 */
	public CheshirePool(String host, int port, int poolsize, CheshirePool.TYPE connectionType) {
		super(host, port);
		this.type = connectionType;	
		this.callbackExecutor = new ThreadPoolExecutor(
				1, // core size
			    50, // max size
			    60, // idle timeout
			    TimeUnit.SECONDS,
			    new SynchronousQueue<Runnable>(), // queue with a size
			    new ThreadPoolExecutor.CallerRunsPolicy() //if queue is full run in current thread.
		);
		this.evloop = new NioEventLoopGroup();
		
		this.pool = new Pool<CheshireClient>(new ClientCreator(this), poolsize);
	}
	
	/* (non-Javadoc)
	 * @see com.trendrr.oss.strest.cheshire.CheshireApiCaller#close()
	 */
	@Override
	public void close() {
		pool.close();
	}

	/* (non-Javadoc)
	 * @see com.trendrr.cheshire.client.CheshireClient#apiCall(com.trendrr.oss.strest.models.StrestRequest)
	 */
	@Override
	public CheshireListenableFuture apiCall(StrestRequest req)
			throws TrendrrDisconnectedException {
		for (int i=0; i < this.pool.getSize(); i++ ) {
			CheshireClient c = null;
			try {
				c = this.pool.borrow(20 * 1000);
			} catch (Exception e) {
				throw new TrendrrDisconnectedException(e);
			} 
		
			try {
				CheshireListenableFuture fut = c.apiCall(req);
				//no exception, return the client and done.
				this.pool.returnGood(c);
				return fut;
			} catch (TrendrrDisconnectedException x) {
				this.pool.returnBroken(c);
			}
		}
		throw new TrendrrDisconnectedException("Tried " + this.pool.getSize() + " times, and couldnt get a single usable connection!");

	}
}
