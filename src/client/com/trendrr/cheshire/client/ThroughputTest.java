/**
 * 
 */
package com.trendrr.cheshire.client;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.cheshire.client.netty4.CheshireNetty4Client;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.concurrent.Sleep;
import com.trendrr.oss.strest.cheshire.CheshireApiCallback;
import com.trendrr.oss.strest.cheshire.Verb;


/**
 * @author Dustin Norlander
 * @created May 17, 2013
 * 
 */
public class ThroughputTest {

	protected static Log log = LogFactory.getLog(ThroughputTest.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final int POOLSIZE = 10;
		final int ITERATIONS = 1000000;
		
		
		try {
			log.error("test");
			// 100k in about 5 seconds

//			CheshirePool client1 = new CheshirePool("localhost", 8009, POOLSIZE, CheshireNetty4Client.PROTOCOL.JSON);
			
			CheshirePool client1 = new CheshirePool("localhost", 8011, POOLSIZE, CheshireNetty4Client.PROTOCOL.BINARY);
			
//			CheshireNetty4Client client1 = new CheshireNetty4Client("localhost", 8009, Executors.newCachedThreadPool());
//			client1.connect();
			
			//warm it up
			client1.ping();
			
			//the count of responses
			final AtomicInteger count = new AtomicInteger(0);
			final Date start = new Date();
			for (int i=0; i < ITERATIONS; i++) {
				try {
					if (i % 1000 == 0 ) {
						System.out.println("Sending ping " + i);
					}
					
					client1.apiCall("/ping", Verb.GET, null, new CheshireApiCallback() {

						@Override
						public void error(Throwable arg0) {
							count.incrementAndGet();
							arg0.printStackTrace();
						}

						@Override
						public void response(DynMap res) {
							int c = count.incrementAndGet();
							if (c % 1000 == 0) {
								System.out.println("Recieved response " + c);
							}
							if (c == ITERATIONS) {
								System.out.println("FINISHED " + ITERATIONS + " IN " + 
										(new Date().getTime() - start.getTime()) +
										" MILLIS");
								
								System.exit(0);
							}
						}

						@Override
						public void txnComplete(String arg0) {
						}
						
					});
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}
			}
		} catch (Exception e) {
			log.error("Caught Exception", e);
		}
		//sleep
		Sleep.minutes(10);
	}
}
