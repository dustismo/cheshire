/**
 * 
 */
package com.trendrr.cheshire.client;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.cheshire.client.json.CheshireNettyClient;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.exceptions.TrendrrDisconnectedException;
import com.trendrr.oss.exceptions.TrendrrException;
import com.trendrr.oss.exceptions.TrendrrTimeoutException;
import com.trendrr.oss.strest.cheshire.CheshireApiCallback;
import com.trendrr.oss.strest.cheshire.CheshireApiCaller;
import com.trendrr.oss.strest.cheshire.Verb;
import com.trendrr.oss.strest.models.StrestRequest;
import com.trendrr.oss.strest.models.StrestHeader.Method;
import com.trendrr.oss.strest.models.StrestHeader.TxnAccept;
import com.trendrr.oss.strest.models.json.StrestJsonRequest;


/**
 * Base class for the cheshire clients.
 * 
 * 
 * 
 * 
 * @author Dustin Norlander
 * @created Nov 5, 2012
 * 
 */
public abstract class CheshireClient implements CheshireApiCaller {

	protected static Log log = LogFactory.getLog(CheshireClient.class);

	protected String host;
	protected int port;
	
	protected String pingEndpoint = "/ping";
	
	protected Date lastSuccessfulPing = new Date();
	protected int defaultPingTimeoutSeconds = 30;
	
	public CheshireClient(String host, int port) {
		this.host = host;
		this.port = port;
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
	public DynMap apiCall(String endPoint, Verb method, Map params, long timeoutMillis) throws TrendrrTimeoutException, TrendrrDisconnectedException, TrendrrException {
		StrestRequest req = this.createRequest(endPoint, method, params);
		req.setTxnAccept(TxnAccept.SINGLE);
		CheshireListenableFuture fut = this.apiCall(req);
		try {
			return fut.get(timeoutMillis, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			throw CheshireNettyClient.toTrendrrException(e);
		}
	}

	public abstract CheshireListenableFuture apiCall(StrestRequest req);
	
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
	
	void cancelFuture(CheshireListenableFuture fut) {
		//override in subclass if needed
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
	 * Does a synchronous ping.  will throw an exception.  
	 * @throws Exception
	 */
	public void ping() throws TrendrrException {
		this.apiCall(this.pingEndpoint, Verb.GET, null, this.defaultPingTimeoutSeconds*1000);
		this.setLastSuccessfullPing(new Date());
	}
	
	public static TrendrrException toTrendrrException(Throwable t) {
		if (t == null) {
			return new TrendrrException("Unhappy no no");
		}
		if (t instanceof TrendrrException) {
			return (TrendrrException)t;
		}
		if (t instanceof ExecutionException) {
			return toTrendrrException(((ExecutionException)t).getCause());
		}
		
		if (t instanceof java.nio.channels.ClosedChannelException) {
			return new TrendrrDisconnectedException((Exception)t);
		}
		
		if (t instanceof java.net.ConnectException) {
			return new TrendrrDisconnectedException((Exception)t);	
		}
		
		if (t instanceof InterruptedException) {
			return new TrendrrTimeoutException((InterruptedException)t);
		}
		if (t instanceof TimeoutException) {
			return new TrendrrTimeoutException((TimeoutException)t);
		}
		
//		if (t instanceof org.jboss.netty.handler.timeout.TimeoutException) {
//			return new TrendrrTimeoutException((org.jboss.netty.handler.timeout.TimeoutException)t);
//		}
		
		//default
		if (t instanceof Exception) {
			return new TrendrrException((Exception)t);
		}
		return new TrendrrException(new Exception(t));
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
}
