/**
 * 
 */
package com.trendrr.cheshire.controllers.html;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.trendrr.cheshire.CheshireHTML;
import com.trendrr.cheshire.CheshireHTMLController;
import com.trendrr.cheshire.CheshireGlobals;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.FileCache;
import com.trendrr.strest.ContentTypes;
import com.trendrr.strest.StrestHttpException;

/*
 * simple controller to server static files from the static directory.
 * 
 */
@CheshireHTML(
		route = "/static/*filename",
		authenticate = false,
		enableSessions = false
)
public class StaticFilesController extends CheshireHTMLController {

	protected Log log = LogFactory.getLog(StaticFilesController.class);
	public static String baseDir = CheshireGlobals.baseDir + "static/";
	public static long cacheTimeout = 10*1000l;
	protected static FileCache cache = new FileCache();
	
	@Override
	public void handleGET(DynMap params) throws Exception {
		String filename = baseDir + params.getString("filename");
		if (filename.contains("/.")) {
			throw StrestHttpException.BAD_REQUEST("Bad bad bad");
		}
		String returnType = params.getString("return_type");
		if (returnType != null) {
			filename += "." + returnType;
		}
		byte[] bytes = cache.getFileBytes(filename, cacheTimeout);
		if (bytes == null) {
			throw StrestHttpException.NOT_FOUND();
		}
		this.setResponseBytes(ContentTypes.fromFileExtension(returnType), bytes);
	}
}
