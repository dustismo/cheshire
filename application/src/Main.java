/**
 * 
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.cheshire.CheshireGlobals;
import com.trendrr.oss.DynMap;
import com.trendrr.strest.StrestServerBuilder;
import com.trendrr.strest.server.StrestServer;


/**
 * @author Dustin Norlander
 * @created Mar 5, 2012
 * 
 */
public class Main {

	protected Log log = LogFactory.getLog(Main.class);
	
	public static void main(String ...strings) throws Exception {
		CheshireGlobals.baseDir = "application/";
		StrestServerBuilder builder = new StrestServerBuilder();
		builder.addConfigFile(CheshireGlobals.baseDir + "config/config.yaml");
		builder.build().start();
	}
}
