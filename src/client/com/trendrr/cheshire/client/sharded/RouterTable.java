/**
 * 
 */
package com.trendrr.cheshire.client.sharded;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.exceptions.TrendrrParseException;


/**
 * @author Dustin Norlander
 * @created Aug 20, 2013
 * 
 */
public class RouterTable {

	protected static Log log = LogFactory.getLog(RouterTable.class);
	
	protected String service;
	protected long revision;
	protected int totalPartitions;
	
	protected String json;
	
	protected Multimap<Integer, RouterTableEntry> entriesByPartition = ArrayListMultimap.create();
	
	protected Collection<RouterTableEntry> entries = new ArrayList<RouterTableEntry>();

	
// {
//  "service" : "trendrrdb",
//  "revision" : 898775762309309,
//  "total_partitions" : 256,
//  "entries" : [
//      {/*router entry 1*/},
//      {/*router entry 2*/}
//  ]
//}	
	public static RouterTable parse(DynMap mp) throws TrendrrParseException {
		RouterTable rt = new RouterTable();
		rt.service = mp.getString("service");
		if (rt.service == null) {
			throw new TrendrrParseException("No service in router table");
		}
		
		rt.revision = mp.getLong("revision", 0l);
		rt.totalPartitions = mp.getInteger("total_partitions", 0);
		
		//parse the entries
		for (DynMap en : mp.getListOrEmpty(DynMap.class, "entries")){
			RouterTableEntry entry = RouterTableEntry.parse(en);
			rt.entries.add(entry);
			for (Integer p : entry.getPartitions()) {
				rt.entriesByPartition.put(p, entry);
			}
		}
		rt.json = mp.toJSONString();
		return rt;
	}
	
	public String getService() {
		return service;
	}

	public long getRevision() {
		return revision;
	}

	public int getTotalPartitions() {
		return totalPartitions;
	}

	public String getJson() {
		return json;
	}

	public Collection<RouterTableEntry> getEntries() {
		return entries;
	}

	/**
	 * returns the entries responsible for the partition.  
	 * 
	 * Currently this will be a collection of length 1, in the future it may be multiple entries once replication is implemented
	 * 
	 * @param partition
	 * @return
	 */
	public Collection<RouterTableEntry> getEntries(int partition) {
		return this.entriesByPartition.get(partition);
	}
	
	
}
