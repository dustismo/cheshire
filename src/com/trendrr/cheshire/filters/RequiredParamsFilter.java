/**
 * 
 */
package com.trendrr.cheshire.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.trendrr.strest.StrestException;
import com.trendrr.strest.StrestHttpException;
import com.trendrr.strest.server.StrestController;
import com.trendrr.strest.server.StrestControllerFilter;
import com.trendrr.strest.server.v2.models.StrestResponse;


/**
 * @author Dustin Norlander
 * @created Mar 29, 2011
 * 
 */
public class RequiredParamsFilter implements StrestControllerFilter {

	protected static Logger log = LoggerFactory.getLogger(RequiredParamsFilter.class);

	/* (non-Javadoc)
	 * @see com.trendrr.strest.server.StrestControllerFilter#before(com.trendrr.strest.server.StrestController)
	 */
	@Override
	public void before(StrestController controller) throws StrestException {
		String req[] = controller.requiredParams();
		if (req == null) 
			return;
		for (String p : req) {
			if (p.isEmpty())
				continue;
			if (controller.getParams().get(p) == null) {
				throw StrestHttpException.NOT_ACCEPTABLE("Param : " + p + " is required for this action!");
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.trendrr.strest.server.StrestControllerFilter#after(com.trendrr.strest.server.StrestController)
	 */
	@Override
	public void after(StrestController controller) throws StrestException {
		// Do Nothing

	}

	/* (non-Javadoc)
	 * @see com.trendrr.strest.server.StrestControllerFilter#error(com.trendrr.strest.server.StrestController, org.jboss.netty.handler.codec.http.HttpResponse, java.lang.Exception)
	 */
	@Override
	public void error(StrestController controller, StrestResponse response,
			Exception exception) {
		//Do nothing
	}
}
