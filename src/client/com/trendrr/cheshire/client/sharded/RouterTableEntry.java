/**
 * 
 */
package com.trendrr.cheshire.client.sharded;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.oss.DynMap;
import com.trendrr.oss.exceptions.TrendrrParseException;


/**
 * @author Dustin Norlander
 * @created Aug 20, 2013
 * 
 */
public class RouterTableEntry {

	protected static Log log = LogFactory.getLog(RouterTableEntry.class);
	
	protected String id;
	protected String address;
	protected int jsonPort;
	protected int httpPort;
	protected int binPort;
	protected List<Integer> partitions;
	
	// {
//  "id" : "localhost:8009"
//  "address" : "localhost",
//  "ports" : {
//      "json" : 8009,
//      "http" : 8010,
//	       "bin" : 8011
//  }
//  "partitions" : [1,2,3,4,5,6,7,8,9]
//}
	public static RouterTableEntry parse(DynMap mp) throws TrendrrParseException{
		RouterTableEntry rt = new RouterTableEntry();
		rt.id = mp.getString("id");
		if (rt.id == null) {
			throw new TrendrrParseException("id not valid");
		}
		
		rt.address = mp.getString("address");
		if (rt.address == null) {
			throw new TrendrrParseException("address not valid");
		}
		
		DynMap ports = mp.getMap("ports", new DynMap());
		if (ports.isEmpty()) {
			throw new TrendrrParseException("No ports in entry");
		}
		
		rt.binPort = ports.getInteger("bin", 0);
		rt.httpPort = ports.getInteger("http", 0);
		rt.jsonPort = ports.getInteger("json", 0);
		rt.partitions = mp.getListOrEmpty(Integer.class, "partitions");
		return rt;
	}
	
	public String getId() {
		return this.id;
	}
	
	public String getAddress() {
		return address;
	}
	public int getJsonPort() {
		return jsonPort;
	}
	public int getHttpPort() {
		return httpPort;
	}
	public int getBinPort() {
		return binPort;
	}
	public List<Integer> getPartitions() {
		return partitions;
	}
	
}
