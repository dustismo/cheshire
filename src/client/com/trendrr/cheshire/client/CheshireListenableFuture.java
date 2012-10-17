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
import com.trendrr.oss.DynMap;
import com.trendrr.oss.strest.cheshire.CheshireApiCallback;

/**
 * @author Dustin Norlander
 * @created Oct 17, 2012
 * 
 */
public class CheshireListenableFuture extends AbstractFuture<DynMap> {

	protected static Log log = LogFactory.getLog(CheshireListenableFuture.class);

	ExecutorService executor;
	CheshireApiCallback callback = null;
	
	CheshireListenableFuture(ExecutorService pool) {
		this.executor = pool;
	}
	
	public void setCallback(CheshireApiCallback cb) {
		this.callback = cb;
		Futures.addCallback(this, new FutureCallback<DynMap>(){
			@Override
			public void onFailure(Throwable error) {
				callback.error(error);
			}

			@Override
			public void onSuccess(DynMap result) {
				callback.response(result);
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
	public boolean set(DynMap newValue) {
	    return super.set(newValue);
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
