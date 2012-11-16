/**
 * 
 */

import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.trendrr.cheshire.client.http.CheshireHttpClient;
import com.trendrr.cheshire.client.json.CheshireNettyClient;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.concurrent.Sleep;
import com.trendrr.oss.exceptions.TrendrrDisconnectedException;
import com.trendrr.oss.exceptions.TrendrrException;
import com.trendrr.oss.exceptions.TrendrrTimeoutException;
import com.trendrr.oss.strest.cheshire.CheshireClient;
import com.trendrr.oss.strest.cheshire.Verb;


/**
 * @author Dustin Norlander
 * @created Oct 17, 2012
 * 
 */
public class CheshireClientTest {

	protected static Log log = LogFactory.getLog(CheshireClientTest.class);
	
	@Test
	public void httptest() {
		try {
			
			
			System.out.println("HERE");
			for (int i=0; i < 50; i++) {
				CheshireHttpClient client1 = new CheshireHttpClient("localhost", 8010);
				
					client1.ping();
					System.out.println("Successful ping!");
				
//				Sleep.seconds(5);
			}
			System.out.println("Thread count: " + Thread.activeCount());

		} catch (TrendrrTimeoutException e) {
			e.printStackTrace();
			log.error("Caught Timeout", e);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Caught Exception", e);
		}
		
	}
	
//	@Test
	public void jsontest() {
		

		try {
			
			CheshireNettyClient client1 = new CheshireNettyClient("dev.trendrr.com", 8009);
			
			for (int i=0; i < 50; i++) {
				
				try {
					client1.ping();
					System.out.println("Successful ping!");
				} catch (TrendrrDisconnectedException e) {
					System.out.println("DISCONNECTED:  REconnect attempt");
					try {
						client1.connect();
					} catch (Exception x) {
						
					}
				}
//				Sleep.seconds(5);
			}
			System.out.println("Thread count: " + Thread.activeCount());

		} catch (TrendrrTimeoutException e) {
			log.error("Caught Timeout", e);
		} catch (Exception e) {
			log.error("Caught Exception", e);
		}
		
	}
}
