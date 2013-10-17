package com.trendrr.cheshire.client.sharded;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;


public class Partitioner {
	
	private int numPartitions;
	HashFunction fun;
	public Partitioner(int num){
		numPartitions = num;
		this.fun = Hashing.md5();
	}

	
	/**
	 * writes the 4 least significant bytes of the md5 hash of the input string into a long 
	 * which is used to assign a partition for that string
	 * @param str
	 * @return 
	 */
	public int partition(String str) throws Exception {
		byte[] md5bytes = fun.hashBytes(str.getBytes("utf8")).asBytes();
		long hashValue = 0l;
		for(int i=0; i <= 3; i++){
			byte b = md5bytes[(md5bytes.length-1) - (3-i)];
			hashValue += (b & 0xFFl) << ((3-i)*8);
		}
		return (int)(hashValue % (long)numPartitions);
	}
}
