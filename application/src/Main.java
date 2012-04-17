/**
 * 
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

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

	protected static Logger log = LoggerFactory.getLogger(Main.class);
	
	public static void main(String ...strings) throws Exception {
		CheshireGlobals.baseDir = "application/";
		LogbackConfigure.configLogback(CheshireGlobals.baseDir + "config/logback.xml");
		StrestServerBuilder builder = new StrestServerBuilder();
		builder.addConfigFile(CheshireGlobals.baseDir + "config/config.yaml");
		builder.build().start();
	}
	
}
