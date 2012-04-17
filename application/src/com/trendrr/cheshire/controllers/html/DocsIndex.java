/**
 * 
 */
package com.trendrr.cheshire.controllers.html;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.trendrr.cheshire.CheshireHTML;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.FileHelper;


/**
 * @author Dustin Norlander
 * @created May 3, 2011
 * 
 */
@CheshireHTML(
		route = "/docs",
		authenticate = true,
		enableSessions = true
)
public class DocsIndex extends Docs {

	protected static Logger log = LoggerFactory.getLogger(DocsIndex.class);
	
	public void handleGET(DynMap params) throws Exception {
		String filename = baseDir + "strestdoc_index.json";
		String json = cache.getFileString(FileHelper.toWindowsFilename(filename), cacheTimeout);
		DynMap val = DynMap.instance(json);
		
		List<DynMap> categories = new ArrayList<DynMap> ();
		for (DynMap cat : val.getList(DynMap.class, "categories")) {
			DynMap c = this.processCategory(cat);
			if (c != null)
				categories.add(c);
		}
		val.put("categories", categories);
		log.warn(val.toJSONString());
		this.render("/documentation/index", val);
	}
	
	/**
	 * processes a specific category.  Returns the processed category or null.
	 * @param category
	 * @return
	 */
	protected DynMap processCategory(DynMap category) {
		List<DynMap> routes = new ArrayList<DynMap>();
		for (DynMap r : category.getList(DynMap.class, "routes")) {
			if (this.hasRouteAcess(r)) {
				routes.add(r);
			}
		}
		if (routes.isEmpty())
			return null;
		category.put("routes", routes);
		return category;
		
	}
	
	protected boolean hasRouteAcess(DynMap route) {
		
		String r = route.getString("route");
		List<String> access = route.getList(String.class, "access");

		if (!this.getAuthToken().hasAccessToRoute(r)) {
			return false;
		}
		
		if (access == null || access.isEmpty()) {
			return true;
		}
		if (this.getAuthToken().getUserAccessRoles().contains("administrator")) {
			return true;
		}
		
		for (String ac : access) {
			if (this.getAuthToken().getUserAccessRoles().contains(ac)) {
				return true;
			}
		}
		return false;
	}
}
