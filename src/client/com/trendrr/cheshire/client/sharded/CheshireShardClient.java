/**
 * 
 */
package com.trendrr.cheshire.client.sharded;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.cheshire.client.CheshireClient;
import com.trendrr.cheshire.client.CheshireListenableFuture;
import com.trendrr.cheshire.client.CheshirePool;
import com.trendrr.cheshire.client.http.CheshireHttpClient;
import com.trendrr.cheshire.client.json.CheshireNettyClient;
import com.trendrr.cheshire.client.netty4.CheshireNetty4Client;
import com.trendrr.cheshire.client.sharded.exceptions.CheshireNoShardParams;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.StringHelper;
import com.trendrr.oss.concurrent.Sleep;
import com.trendrr.oss.exceptions.TrendrrDisconnectedException;
import com.trendrr.oss.exceptions.TrendrrException;
import com.trendrr.oss.exceptions.TrendrrParseException;
import com.trendrr.oss.strest.StrestRequestCallback;
import com.trendrr.oss.strest.models.DefaultStrestRequest;
import com.trendrr.oss.strest.models.ShardRequest;
import com.trendrr.oss.strest.models.StrestHeader.Method;
import com.trendrr.oss.strest.models.StrestRequest;
import com.trendrr.oss.strest.models.StrestResponse;



/**
 * @author Dustin Norlander
 * @created Aug 20, 2013
 * 
 */
public class CheshireShardClient {

	protected static Log log = LogFactory.getLog(CheshireShardClient.class);
	
	// Constants
	// our routertable is old. need to get an update.
	public static final int E_ROUTER_TABLE_OLD = 632;
	// server has requested an update.
	public static final int E_SEND_ROUTER_TABLE = 633;
	// the requested partition is locked.  requester should try back in a bit
	public static final int E_PARTITION_LOCKED = 634;
	// The requested partition does not live on this shard
	public static final int E_NOT_MY_PARTITION = 635;
	
	
	protected HashSet<String> seedUrls = new HashSet<String>();
	protected RouterTable routerTable;
	protected HashMap<String, CheshireClient> clients = new HashMap<String, CheshireClient> ();
	
	
	
	protected Partitioner partitioner;
	
	static int maxRetries = 3;
	
	protected ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
	public CheshireShardClient(String ...seedurls) {
		for (String seed : seedurls) {
			this.seedUrls.add(StringHelper.trim(seed, "/"));
		}
	}
	
	public void close() {
		//TODO
		log.warn("Close not yet implemented");
		
	}
	
	public void connect() throws Exception {
		RouterTable rt = this.routerTable;
		for (String seed : this.seedUrls) {
			if (rt != null) {
				continue;
			}
			try {
				rt = getRouterTable(seed);
			} catch (Exception x) {
				log.warn("Couldn't get router table from : " + seed + " " + x.getMessage());
			}
		}
		if (rt == null) {
			throw new TrendrrException("Couldn't get router table");
		}
		this.setRouterTable(rt);
	}
	
	private long lastRTReq = 0l;
	synchronized void requestRouterTable(CheshireClient client) throws Exception {
		long cur = new Date().getTime();
		//only allow one rt request per second.
		if (cur - lastRTReq < 1000) {
			return;
		}
		try {
			StrestRequest request = new DefaultStrestRequest();
			request.setMethod(Method.GET);
			request.setUri("/__c/rt/get");
			CheshireListenableFuture fut = client.apiCall(request);
			StrestResponse response = fut.get(15, TimeUnit.SECONDS);
			DynMap rt = response.getParams().getMap("router_table");
			this.setRouterTable(RouterTable.parse(rt));
		} finally {
			lastRTReq = new Date().getTime();
		}
	}
	
	void sendRouterTable(CheshireClient client) {
		StrestRequest request = new DefaultStrestRequest();
		request.setMethod(Method.GET);
		request.setUri("/__c/rt/set");
		request.getParams().put("router_table", this.routerTable.getJson());
		try {
			CheshireListenableFuture fut = client.apiCall(request);
		} catch (TrendrrDisconnectedException e) {
			log.error("Caught", e);
		}
	}
	
	
	/**
	 * Sets the router table. if the rt is the same as current, this does nothing.  it throws an exception
	 * if the rt is older.  Else it updates the table and establishes new connections.
	 * 
	 * threadsafe
	 * 
	 * @param rt
	 * @throws Exception
	 */
	public void setRouterTable(RouterTable rt) throws Exception {
		System.out.println("Setting router table!!!");
		System.out.println(rt.getJson());
		this.lock.writeLock().lock();
		HashMap<String, CheshireClient> newClientMap = new HashMap<String, CheshireClient>();
		try {
			if (this.routerTable != null) {
				//check the revision
				if (rt.getRevision() == this.routerTable.getRevision()) {
					return; //do nothing
				}
				
				if (rt.getRevision() < this.routerTable.getRevision()) {
					throw new TrendrrException("Trying to set an old router table");
				}
			}
			this.routerTable = rt;
			
			
			//connect all
			for (RouterTableEntry entry : this.routerTable.getEntries()) {
				CheshireClient client = this.clients.remove(entry.getId());
				if (client == null) {
					client = this.createClient(entry);
				}
				newClientMap.put(entry.getId(), client);
			}
			
			//Close any other clients
			for (CheshireClient c : this.clients.values()) {
				c.close();
			}
			this.clients.clear();
			
			this.partitioner = new Partitioner(this.routerTable.getTotalPartitions());
			
		} finally {	
			this.clients.putAll(newClientMap);
			this.lock.writeLock().unlock();
		}
	}
	
	
	/**
	 * Retries an api call. 
	 * @param cb
	 * @param waitmillis
	 */
	void retryApiCall(CallbackWrapper cb, long waitmillis) {
		if (cb.retries > this.maxRetries) {
			cb.error(new TrendrrException("Tried request " + maxRetries + " times and couldn't get a response."));
			return;
		}
		if (waitmillis == 0) {
			try {
				this.apiCall(cb.request, cb);
			} catch (Exception e) {
				cb.error(e);
			} 
		} else {
			//TODO: add to a wait queue

		}		
	}
	
	public StrestResponse apiCall(StrestRequest request, long timeoutMillis) throws TrendrrDisconnectedException, CheshireNoShardParams, TrendrrException {
		for (int i=0 ; i < maxRetries; i++) {
			
			CheshireListenableFuture fut = this.apiCall(request);
			StrestResponse response = null;
			try {
				response = fut.get(timeoutMillis, TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				throw CheshireNettyClient.toTrendrrException(e);
			}
			
			int code = response.getStatusCode();
			
			if (code > 630 && code < 640) {
				//router table issue
				if (code == CheshireShardClient.E_ROUTER_TABLE_OLD ||
						 code == CheshireShardClient.E_NOT_MY_PARTITION) {
					try {
						this.requestRouterTable(fut.getClient());
						continue;
					} catch (Exception x) {
						throw CheshireNettyClient.toTrendrrException(x);
					}
				}
				if (code == CheshireShardClient.E_SEND_ROUTER_TABLE) {
					try {
						this.sendRouterTable(fut.getClient());
						continue;
					} catch (Exception x) {
						throw CheshireNettyClient.toTrendrrException(x);
					}
				}
				if (code == CheshireShardClient.E_PARTITION_LOCKED) {
					log.warn("Partition locked, try again in 3 second");
					Sleep.seconds(3);
					continue;
				}
				log.warn("Unknown response code: " + response.getStatusCode() + " " + response.getStatusMessage());	
			}
			
			//else good response.
			return response;
		}
		throw new TrendrrException("Tried " + maxRetries + " times to send message, giving up...");
	}
	
	public void apiCall(StrestRequest request, StrestRequestCallback callback) throws TrendrrDisconnectedException, CheshireNoShardParams {
		CheshireListenableFuture fut = this.apiCall(request);
		
		CallbackWrapper cb = null;
		
		if (callback instanceof CallbackWrapper) {
			cb = ((CallbackWrapper) callback);
		} else {
			cb = new CallbackWrapper();
		}
		cb.client = this;
		cb.connection = fut.getClient();
		cb.request = request;
		cb.cb = callback;
		
		fut.setStrestCallback(cb);
	}
	
	
	
	protected CheshireListenableFuture apiCall(StrestRequest req) throws TrendrrDisconnectedException, CheshireNoShardParams {
		
		this.lock.readLock().lock();
		try {
			ShardRequest shard = req.getShardRequest();
			if (shard == null) {
				throw new CheshireNoShardParams("no ShardRequest");
			}
			
			int partition = shard.getPartition();
			if (partition < 0) {
				//try the key
				String key = shard.getKey();
				if (key == null) {
					throw new CheshireNoShardParams("No key");
				}
				
				try {
					partition = this.partitioner.partition(key);
				} catch (Exception e) {
					log.error("Caught", e);
				}
			}
			if (partition < 0) {
				throw new CheshireNoShardParams("No key or partition");
			}
			
			shard.setPartition(partition);
			shard.setRevision(this.routerTable.getRevision());
			
			//ok, we got a partition, woot!
			
			//now find the right client
			for (RouterTableEntry entry : this.routerTable.getEntries(partition)) {
				CheshireClient client = this.clients.get(entry.getId());
				if (client == null) {
					continue;
				}
				CheshireListenableFuture result = client.apiCall(req);
				if (result != null) {
					result.setClient(client);
					return result;
				}
			}
			throw new CheshireNoShardParams("Unknown problem.  ack.");
			
		} finally {
			this.lock.readLock().unlock();
		}
	}
	
	protected CheshireClient createClient(RouterTableEntry entry) throws Exception {
		
		CheshireNetty4Client.PROTOCOL protocol = CheshireNetty4Client.PROTOCOL.JSON;
		int port = entry.getJsonPort();
		if (entry.getBinPort() > 0) {
			protocol = CheshireNetty4Client.PROTOCOL.BINARY;
			port = entry.getBinPort();
		} 
		CheshirePool c = new CheshirePool(entry.getAddress(), port, 10, protocol);
		return c;
	}
	
	/**
	 * Gets the router table from the given url.  Returns a valid router table or throws an exception. 
	 * @param url
	 * @return
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	public static RouterTable getRouterTable(String url) throws Exception {
		System.out.println(url);
		StrestRequest request = new DefaultStrestRequest();
		request.setMethod(Method.GET);
		request.setUri("/__c/rt/get");
		CheshireListenableFuture fut = CheshireHttpClient.api(url, request);
		StrestResponse response = fut.get(15, TimeUnit.SECONDS);
		DynMap rt = response.getParams().getMap("router_table");
try {
	RouterTable.parse(rt);
} catch (Exception x) {
	x.printStackTrace();
}
		return RouterTable.parse(rt);
	}
	
}
