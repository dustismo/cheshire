/**
 * 
 */
package com.trendrr.cheshire.client;

import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.trendrr.cheshire.client.json.CheshireNettyClient;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.strest.cheshire.CheshireApiCallback;
import com.trendrr.oss.strest.models.StrestResponse;

/**
 * @author Dustin Norlander
 * @created Oct 17, 2012
 * 
 */
public class CheshireListenableFuture extends AbstractFuture<StrestResponse> {

	protected static Log log = LogFactory.getLog(CheshireListenableFuture.class);

	ExecutorService executor;
	CheshireApiCallback callback = null;
	
	public CheshireListenableFuture(ExecutorService pool) {
		this.executor = pool;
	}
	
	public void setCallback(CheshireApiCallback cb) {
		this.callback = cb;
		Futures.addCallback(this, new FutureCallback<StrestResponse>(){
			@Override
			public void onFailure(Throwable error) {
				callback.error(CheshireNettyClient.toTrendrrException(error));
			}

			@Override
			public void onSuccess(StrestResponse result) {
				callback.response(DynMap.instance(result));
			}
		}, this.executor);
	}

	/**
	 * returns the callback
	 * @return
	 */
	public CheshireApiCallback getCallback() {
		return callback;
	}
	
	
	@Override
	public boolean set(StrestResponse response) {
	    return super.set(response);
	}
	
	@Override
	public boolean setException(Throwable t) {
		return super.setException(t);
	}
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return super.cancel(mayInterruptIfRunning);
	}
}
