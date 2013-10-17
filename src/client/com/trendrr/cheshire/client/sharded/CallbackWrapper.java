/**
 * 
 */
package com.trendrr.cheshire.client.sharded;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.cheshire.client.CheshireClient;
import com.trendrr.oss.strest.StrestRequestCallback;
import com.trendrr.oss.strest.models.StrestRequest;
import com.trendrr.oss.strest.models.StrestResponse;


/**
 * @author Dustin Norlander
 * @created Aug 23, 2013
 * 
 */
class CallbackWrapper implements StrestRequestCallback {

	protected static Log log = LogFactory.getLog(CallbackWrapper.class);

	StrestRequestCallback cb;
	CheshireShardClient client;
	CheshireClient connection;
	StrestRequest request;
	int retries = 0;
	
	/* (non-Javadoc)
	 * @see com.trendrr.oss.strest.StrestRequestCallback#response(com.trendrr.oss.strest.models.StrestResponse)
	 */
	@Override
	public void response(StrestResponse response) {
		this.retries++;
		if (this.retries > CheshireShardClient.maxRetries) {
			cb.response(response);
			return;
		}
		
		int code = response.getStatusCode();
		
		
		if (response.getStatusCode() > 630 && response.getStatusCode() < 640) {
			//router table issue
			if (code == CheshireShardClient.E_ROUTER_TABLE_OLD ||
					 code == CheshireShardClient.E_NOT_MY_PARTITION) {
				try {
                    log.warn("Got a not old router table problem: " + code + " " + request.getShardRequest().getRevision() + " " + request.getUri() + " client: " + this.connection.getHost() + " partition: " + this.request.getShardRequest().getPartition());
					this.client.requestRouterTable(/*this.connection*/);
					client.retryApiCall(this, 0);
				} catch (Exception x) {
                    log.warn("Caught", x);
					this.error(x);
				}
				return;
			}
			if (code == CheshireShardClient.E_SEND_ROUTER_TABLE) {
				try {
                    log.warn("Client wants a router table");
					this.client.sendRouterTable(this.connection);
					client.retryApiCall(this, 0);
				} catch (Exception x) {
                    log.warn("Caught", x);
					this.error(x);
				}
				return;
			}
			if (code == CheshireShardClient.E_PARTITION_LOCKED) {
				log.warn("Partition locked, try again in 1 second");
				client.retryApiCall(this, 1000);
				return;
			}
			log.warn("Unknown response code: " + response.getStatusCode() + " " + response.getStatusMessage());	
		}
		cb.response(response);
	}

	/* (non-Javadoc)
	 * @see com.trendrr.oss.strest.StrestRequestCallback#txnComplete(java.lang.String)
	 */
	@Override
	public void txnComplete(String txnId) {
		cb.txnComplete(txnId);
	}

	/* (non-Javadoc)
	 * @see com.trendrr.oss.strest.StrestRequestCallback#error(java.lang.Throwable)
	 */
	@Override
	public void error(Throwable x) {
		cb.error(x);
	}
}
