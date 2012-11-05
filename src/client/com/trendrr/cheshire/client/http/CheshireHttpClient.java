/**
 * 
 */
package com.trendrr.cheshire.client.http;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

import com.google.common.util.concurrent.MoreExecutors;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Request;
import com.ning.http.client.Response;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.trendrr.cheshire.client.CheshireClient;
import com.trendrr.cheshire.client.CheshireListenableFuture;
import com.trendrr.cheshire.client.json.CheshireNettyClient;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.concurrent.LazyInitObject;
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
 * A cheshire client based on https://github.com/AsyncHttpClient/async-http-client
 * 
 * @author Dustin Norlander
 * @created Nov 5, 2012
 * 
 */
public class CheshireHttpClient extends CheshireClient {

	protected static Log log = LogFactory.getLog(CheshireHttpClient.class);

	protected static LazyInitObject<AsyncHttpClient> client = new LazyInitObject<AsyncHttpClient>() {
		@Override
		public AsyncHttpClient init() {
			AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder()
				.setRequestTimeoutInMs(30*1000)
				.setIdleConnectionTimeoutInMs(30*1000)
				.setConnectionTimeoutInMs(30*1000)
				.setCompressionEnabled(false)
				.setUserAgent("cheshire-http-client")
				.build();
			return new AsyncHttpClient(config);
		}
	}; //probably should be static?
	ExecutorService asynchThreadPool = MoreExecutors.sameThreadExecutor();
	
	protected AsyncHttpClient httpClient = null; //in case user wants to use a local one.
	
	public CheshireHttpClient(String host, int port, ExecutorService callbackExecutor, AsyncHttpClient httpClient) {
		super(host, port);
		this.httpClient = httpClient;
		if (callbackExecutor != null) {
			this.asynchThreadPool = callbackExecutor;
		}
	}
	
	public CheshireHttpClient(String host, int port, ExecutorService callbackExecutor) {
		this(host,port,callbackExecutor, null);
	}
	
	public CheshireHttpClient(String host, int port, AsyncHttpClient httpClient) {
		this(host, port, null, httpClient);
	}
	
	public CheshireHttpClient(String host, int port) {
		this(host, port, null, null);
	}
	public CheshireListenableFuture apiCall(StrestRequest req) {
		String url = "http://" + this.host + ":" + this.port + req.getUri();
		
		BoundRequestBuilder builder = null;
		if (Verb.instance(req.getMethod().toString()) == Verb.GET) {
			builder = this.getHttpClient().prepareGet(url + "?" + DynMap.instance(req.getParams(), new DynMap()).toURLString());
		} else {
			builder = this.getHttpClient().preparePost(url).setBody(DynMap.instance(req.getParams(), new DynMap()).toURLString())
				.setHeader("Content-Type", "application/x-www-form-urlencoded");
		}
		
		final CheshireListenableFuture result = new CheshireListenableFuture(this.asynchThreadPool);
		try {
			Future<Integer> f = builder.execute(
				       new AsyncCompletionHandler<Integer>(){
	
				        @Override
				        public Integer onCompleted(Response response) throws Exception{
				        	result.set(DynMap.instance(response.getResponseBody()));
				            return response.getStatusCode();
				        }
	
				        @Override
				        public void onThrowable(Throwable t){
				            result.setException(t);
				        }
				    });
		} catch (Exception x) {
			result.setException(x);
		}
		return result;
	}
	
	
	/* (non-Javadoc)
	 * @see com.trendrr.oss.strest.cheshire.CheshireApiCaller#close()
	 */
	@Override
	public void close() {
		if (this.httpClient != null) {
			this.httpClient.close();
		}
	}
	

	/**
	 * gets the shared static AsyncHttpClient 
	 * @return
	 */
	public static AsyncHttpClient getAsyncHttpClient() {
		return client.get();
	}
	
	
	public AsyncHttpClient getHttpClient() {
		if (this.httpClient != null)
			return this.httpClient;
		return client.get();
	}
}
