/**
 * 
 */
package com.trendrr.cheshire.client;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.cheshire.client.json.CheshireNettyClient;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.concurrent.LazyInit;
import com.trendrr.oss.exceptions.TrendrrDisconnectedException;
import com.trendrr.oss.exceptions.TrendrrException;
import com.trendrr.oss.exceptions.TrendrrTimeoutException;
import com.trendrr.oss.strest.cheshire.CheshireApiCallback;
import com.trendrr.oss.strest.cheshire.CheshireApiCaller;
import com.trendrr.oss.strest.cheshire.Verb;
import com.trendrr.oss.strest.models.StrestRequest;


/**
 * A client implementation that maintains a pool of connections.
 * 
 * Use this for highest throughput.  The pool is used in a roundrobin, since the client does not need to 
 * wait for a response.
 * 
 * Disconnections will propogate to the end user, but the underlying connection will automatically be reconnected.
 * 
 * 
 * @author Dustin Norlander
 * @created Mar 19, 2013
 * 
 */
public class CheshirePooledClient extends CheshireClient {

	protected static Log log = LogFactory.getLog(CheshirePooledClient.class);

	private int poolsize;
	private AtomicLong checkout = new AtomicLong(0l);
	
	private Client[] clients;
	
	private AtomicBoolean closed = new AtomicBoolean(false);
	
	class Client {
		LazyInit lock = new LazyInit();
		CheshireNettyClient client;
		/**
		 * refresh this client due to disconnect or something
		 */
		void refresh() {
			lock.reset();
		}
	}
	
	class Callback implements CheshireApiCallback {
		CheshireApiCallback callback;
		Client client;
		
		@Override
		public void error(Throwable ex) {
			if (ex instanceof TrendrrDisconnectedException) {
				client.refresh();
			}
			callback.error(ex);
		}
		
		@Override
		public void response(DynMap arg0) {
			callback.response(arg0);
		}
		
		@Override
		public void txnComplete(String arg0) {
			callback.txnComplete(arg0);
		}
	}
	
	
	
	public CheshirePooledClient(String host, int port, int poolsize) {
		super(host, port);
		this.poolsize = poolsize;
		clients = new Client[poolsize];
		for (int i=0; i < poolsize; i++) {
			clients[i] = new Client();
		}
	}
	
	protected Client nextClient(boolean tryNext) throws TrendrrException {
		if (this.poolsize < 1) {
			throw new TrendrrException("Bad poolsize");
		}
		if (this.closed.get()) {
			throw new TrendrrException("Client is closed");
		}
		
		int index = (int)(this.checkout.incrementAndGet() % this.poolsize);
		
//		System.out.println("GETTIGN CLIENT: " + index);
		Client c = clients[index];
		if (c.lock.start()) {			
			//need to initialize
			try {
//				System.out.println("initializing client : " + index);
				if (c.client != null) {
//					System.out.println("REINIT!");
					c.client.close();
				}
				if (this.closed.get()) {
					throw new TrendrrException("Client is closed");
				}
				CheshireNettyClient client = new CheshireNettyClient(this.host, this.port);
				client.connect();
				c.client = client;
			} finally {
				c.lock.end();
			}
		}
		
		if (c.client == null) {
			c.refresh();
			if (tryNext) {
				return this.nextClient(false);
			}
		}
		
		return c;
	}
	
	/**
	 * Does an asynch api call.  This will NOT retry the request on 
	 * disconnect.
	 */
	@Override
	public void apiCall(String uri, Verb verb, Map params,
			CheshireApiCallback callback) {
		try {
			Client c = this.nextClient(true);
			Callback cb = new Callback();
			cb.callback = callback;
			cb.client = c;
			c.client.apiCall(uri, verb, params, callback);
		} catch (Exception x) {
			callback.error(x);
		}
		
	}

	/**
	 * Does a synchronous api call.  This will retry the call exactly once on disconnect.
	 * 
	 */
	@Override
	public DynMap apiCall(String uri, Verb verb, Map params, long timeoutMillis)
			throws TrendrrTimeoutException, TrendrrException {
		Client c = this.nextClient(true);
		try {
			return c.client.apiCall(uri, verb, params, timeoutMillis);
		} catch (TrendrrDisconnectedException x) {
			c.refresh();
			//retry it..
			c = this.nextClient(false);
			return c.client.apiCall(uri, verb, params, timeoutMillis);
		}
	}

	/* (non-Javadoc)
	 * @see com.trendrr.oss.strest.cheshire.CheshireApiCaller#close()
	 */
	@Override
	public void close() {
		this.closed.set(true);
		for (Client c : this.clients) {
			if (c.client != null) {
				c.client.close();
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.trendrr.cheshire.client.CheshireClient#apiCall(com.trendrr.oss.strest.models.StrestRequest)
	 */
	@Override
	public CheshireListenableFuture apiCall(StrestRequest req) {
		try {
			Client c = this.nextClient(true);
			return c.client.apiCall(req);
		} catch (Exception x) {
			CheshireListenableFuture sf = new CheshireListenableFuture(null);
			sf.setException(x);
			return sf;
		}
	}
}
