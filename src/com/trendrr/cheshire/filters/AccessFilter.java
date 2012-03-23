/**
 * 
 */
package com.trendrr.cheshire.filters;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.trendrr.cheshire.CheshireController;
import com.trendrr.cheshire.CheshireHTMLController;
import com.trendrr.cheshire.authentication.AuthToken;
import com.trendrr.strest.StrestException;
import com.trendrr.strest.StrestHttpException;


/**
 * @author Dustin Norlander
 * @created Mar 28, 2011
 * 
 */
public class AccessFilter extends CheshireFilter {

	protected static Logger log = LoggerFactory.getLogger(AccessFilter.class);


	/* (non-Javadoc)
	 * @see com.trendrr.cheshire.filters.CheshireFilter#before(com.trendrr.cheshire.CheshireApiController)
	 */
	@Override
	public void before(CheshireController controller) throws StrestException {
		if (!controller.requireAuthentication()) {
			return;
		}
		AuthToken auth = controller.getAuthToken();
		if (auth == null) {
			if (controller instanceof CheshireHTMLController) {
				try {
					((CheshireHTMLController)controller).redirect(controller.getServerConfig().getString("html.pages.login", "/login") + "?forward=" + 
							URLEncoder.encode(controller.getRequest().getUri(), "utf-8"));
				} catch (UnsupportedEncodingException e) {
					log.error("Exception while trying to redirect to login page:", e);
					throw StrestHttpException.FORBIDDEN("Authentication is manditory (" + e.getMessage() + ")");
				}
				return;
			}
			throw StrestHttpException.FORBIDDEN("Authentication is manditory");
		}
		
		if (auth.getUserAccessRoles().contains("administrator")) {
			return;
		}

		if (!auth.hasAccessToRoute(controller.routes())) {
			throw StrestHttpException.FORBIDDEN("You do not have access to this api call");
		}
		
		String access[] = controller.requiredAccess();
		if (access != null && access.length > 0) {
			Set<String> userAc = auth.getUserAccessRoles();
			boolean hasAccess = false;
			for (String ac : access) {
				if (userAc.contains(ac)) {
					hasAccess = true;
				}
			}
			if (!hasAccess) {
				throw StrestHttpException.FORBIDDEN("You do not have access to this api call");
			}
		}
		
	}

	/* (non-Javadoc)
	 * @see com.trendrr.cheshire.filters.CheshireFilter#after(com.trendrr.cheshire.CheshireApiController)
	 */
	@Override
	public void after(CheshireController controller) throws StrestException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.trendrr.cheshire.filters.CheshireFilter#error(com.trendrr.cheshire.CheshireApiController, org.jboss.netty.handler.codec.http.HttpResponse, java.lang.Exception)
	 */
	@Override
	public void error(CheshireController controller, HttpResponse response,
			Exception exception) {
		// TODO Auto-generated method stub
		
	}

	
}
