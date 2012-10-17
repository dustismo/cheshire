/**
 * 
 */

import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import com.trendrr.cheshire.client.CheshireNettyClient;
import com.trendrr.oss.DynMap;
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
	public void test() {
		

		try {
			CheshireNettyClient client = new CheshireNettyClient(Executors.newCachedThreadPool(),
	                Executors.newCachedThreadPool(), "localhost", 8009);
			client.connect();
			
			CheshireClient client2 = new CheshireClient("localhost", 8009);
			client2.connect();
			
			
			DynMap result1 = client.apiCall("/ping", Verb.GET, null, 10*1000);
			DynMap result2 = client2.apiCall("/ping", Verb.GET, null, 10*1000);
			System.out.println(result1.toJSONString());
			System.out.println(result2.toJSONString());
		} catch (TrendrrTimeoutException e) {
			log.error("Caught", e);
		} catch (Exception e) {
			log.error("Caught", e);
		}
		
	}
}
