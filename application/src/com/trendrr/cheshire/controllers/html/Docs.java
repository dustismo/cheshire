/**
 * 
 */
package com.trendrr.cheshire.controllers.html;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.cheshire.CheshireHTML;
import com.trendrr.cheshire.CheshireHTMLController;
import com.trendrr.cheshire.CheshireGlobals;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.FileCache;
import com.trendrr.oss.FileHelper;
import com.trendrr.oss.concurrent.ReinitObject;
import com.trendrr.strest.ContentTypes;
import com.trendrr.strest.StrestHttpException;
import com.trendrr.strest.annotations.Strest;
import com.trendrr.strest.server.controllers.StaticFileController;

/**
 * @author Dustin Norlander
 * @created Apr 5, 2011
 * 
 */
@CheshireHTML(
		route = "/docs/*filename",
		authenticate = true,
		enableSessions = true
)
public class Docs extends CheshireHTMLController {

	protected static Log log = LogFactory.getLog(Docs.class);
	
	
	public static String baseDir = CheshireGlobals.baseDir + "strestdoc/";
	//cache times out after 20 seconds.  long enough to help in case of 
	//a torrent of traffic, short enough to develop.  
	public static long cacheTimeout = 10*1000l;
	
	protected static FileCache cache = new FileCache();
	
	@Override
	public void handleGET(DynMap params) throws Exception {
		String filename = baseDir + params.getString("filename");
		if (filename.contains("/.")) {
			throw StrestHttpException.BAD_REQUEST("Bad bad bad");
		}
		
		if (!this.getAuthToken().hasAccessToRoute("/" + filename.replace(baseDir, ""))) {
			throw StrestHttpException.UNAUTHORIZED();
		}
		String json = cache.getFileString(FileHelper.toWindowsFilename(filename + ".json"), cacheTimeout);
		
		if (json == null) {
			throw StrestHttpException.NOT_FOUND();
		}
		DynMap val = DynMap.instance(json);
		for (String ac : val.getListOrEmpty(String.class, "access")) {
			if (!this.getAuthToken().getUserAccessRoles().contains(ac)) {
				throw StrestHttpException.UNAUTHORIZED();
			}
		}
		this.render("/documentation/routePage.html", val);
	}

}
