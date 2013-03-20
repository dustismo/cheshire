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
public class CheshirePooledClient implements CheshireApiCaller {

	protected static Log log = LogFactory.getLog(CheshirePooledClient.class);

	String host;
	int port;
	int poolsize;
	AtomicLong checkout = new AtomicLong(0l);
	
	Client[] clients;
	
	AtomicBoolean closed = new AtomicBoolean(false);
	
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
	
	
	
	public CheshirePooledClient (String host, int port, int poolsize) {
		this.host = host;
		this.port = port;
		this.poolsize = poolsize;
		clients = new Client[poolsize];
		for (int i=0; i < poolsize; i++) {
			clients[i] = new Client();
		}
	}
	
	protected Client nextClient() throws TrendrrException {
		if (this.poolsize < 1) {
			throw new TrendrrException("Bad poolsize");
		}
		if (this.closed.get()) {
			throw new TrendrrException("Client is closed");
		}
		
		int index = (int)(this.checkout.incrementAndGet() % this.poolsize);
		Client c = clients[index];
		if (c.lock.start()) {			
			//need to initialize
			try {
				if (c.client != null) {
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
			Client c = this.nextClient();
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
		Client c = this.nextClient();
		try {
			return c.client.apiCall(uri, verb, params, timeoutMillis);
		} catch (TrendrrDisconnectedException x) {
			c.refresh();
			//retry it..
			c = this.nextClient();
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
	 * @see com.trendrr.oss.strest.cheshire.CheshireApiCaller#getHost()
	 */
	@Override
	public String getHost() {
		return this.host;
	}

	/* (non-Javadoc)
	 * @see com.trendrr.oss.strest.cheshire.CheshireApiCaller#getPort()
	 */
	@Override
	public int getPort() {
		return this.port;
	}
}
