/**
 * 
 */

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;


/**
 * @author Dustin Norlander
 * @created Mar 12, 2012
 * 
 */
public class LogbackConfigure {
	
	public static void configLogback(String configFilename) {
	    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
	            try {
	           JoranConfigurator configurator = new JoranConfigurator();
	           configurator.setContext(lc);
	           lc.reset();
	           configurator.doConfigure(configFilename);
	       } catch (JoranException je) {
	           je.printStackTrace();
	       }
	}
	
}
