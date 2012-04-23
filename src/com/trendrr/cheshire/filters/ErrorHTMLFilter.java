/**
 * 
 */
package com.trendrr.cheshire.filters;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.trendrr.cheshire.CheshireController;
import com.trendrr.cheshire.CheshireHTMLController;
import com.trendrr.strest.StrestException;
import com.trendrr.strest.StrestHttpException;


/**
 * Filter to handle any exceptions.  redirects to appropriate page, and adds flash messages.
 * 
 * 
 * @author Dustin Norlander
 * @created Apr 20, 2012
 * 
 */
public class ErrorHTMLFilter extends CheshireFilter {

	protected static Log log = LogFactory.getLog(ErrorHTMLFilter.class);

	/* (non-Javadoc)
	 * @see com.trendrr.cheshire.filters.CheshireFilter#before(com.trendrr.cheshire.CheshireController)
	 */
	@Override
	public void before(CheshireController controller) throws StrestException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see com.trendrr.cheshire.filters.CheshireFilter#after(com.trendrr.cheshire.CheshireController)
	 */
	@Override
	public void after(CheshireController controller) throws StrestException {
		if (!(controller instanceof CheshireHTMLController)) {
			return;
		}

	}

	/* (non-Javadoc)
	 * @see com.trendrr.cheshire.filters.CheshireFilter#error(com.trendrr.cheshire.CheshireController, org.jboss.netty.handler.codec.http.HttpResponse, java.lang.Exception)
	 */
	@Override
	public void error(CheshireController controller, HttpResponse response,
			Exception exception) {
		if (controller == null || !(controller instanceof CheshireHTMLController)) {
			return;
		}
		CheshireHTMLController c = (CheshireHTMLController)controller;
		if (exception instanceof StrestHttpException) {
			int code = ((StrestHttpException)exception).getCode();
			if (code == 401) {
				//unauthorized
				try {
//					log.warn("Redirecting to login:");
					c.redirect(controller.getServerConfig().getString("html.pages.login", "/login") + "?forward=" + 
							URLEncoder.encode(controller.getRequest().getUri(), "utf-8"));
					return;
				} catch (UnsupportedEncodingException e) {
					log.warn("Caught",e);
				}
			}							

//			log.warn("Redirecting to " + code);
			c.redirect(controller.getServerConfig().getString("html.pages.error_dir", "/errors") + "/" + code);
			return;
		}
//		log.warn("Redirecting to Roor 501");
		c.redirect(controller.getServerConfig().getString("html.pages.error_dir", "/errors") + "/" + 501);
	}
}
