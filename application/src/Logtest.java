/**
 * 
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Dustin Norlander
 * @created Mar 9, 2012
 * 
 */
public class Logtest {

	final static Logger logger = LoggerFactory.getLogger(Logtest.class);
	
	public static void main(String[] args) {
	    logger.info("Entering application.", new Exception());
	    logger.info("Exiting application.");
	  }
}
