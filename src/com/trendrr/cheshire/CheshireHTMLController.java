/**
 * 
 */
package com.trendrr.cheshire;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sampullara.mustache.MustacheBuilder;
import com.sampullara.mustache.MustacheException;
import com.sampullara.mustache.Scope;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.FileHelper;
import com.trendrr.strest.ContentTypes;
import com.trendrr.strest.server.StrestController;


/**
 * 
 * Base class to serve html content.
 * 
 * @author Dustin Norlander
 * @created Feb 29, 2012
 * 
 */
public class CheshireHTMLController extends CheshireController {

	protected static Log log = LogFactory.getLog(CheshireHTMLController.class);
	
	public static void main(String ...strings) {
		{
			DynMap params = new DynMap();
			params.put("hello", "you");
			params.put("you", "me");
			CheshireHTMLController controller = new CheshireHTMLController();
			controller.render("test.html", params);
		}
//		Date start = new Date();
//		for (int i=0; i < 10000; i++) {
//			DynMap params = new DynMap();
//			params.put("hello", "you");
//			params.put("you", "me");
//			CheshireHTMLController controller = new CheshireHTMLController();
//			controller.render("test.html", params);
//		}
//		System.out.println("Millis: " + (new Date().getTime()-start.getTime()));
		
	}
	
	/**
	 * renders a mustache template, the current controller is in scope, as well as any additional params.
	 * 
	 * @param template path to the mustache template.  defaults to looking in the 'views' folder.
	 * @param templateParams
	 */
	public void render(String template, DynMap templateParams) {
//		String root = this.getServerConfig().getString("view_path", "views");
		String root = "views";
		
		if (!template.endsWith(".html")) {
			template = template + ".html";
		}
		/*
		 * TODO: this isn't exactly optimal.  this reloads on every pageload, need to cache the results somewhere/somehow.
		 */
//		String view = "views/" + template;
		StringWriter str = new StringWriter();
		try {
			MustacheBuilder builder = new MustacheBuilder(new File(root));
			builder.parseFile(template).execute(str, new Scope(templateParams, new Scope(this)));
//			String t = FileHelper.loadString(view);
//			System.out.println(t);
//			
//			
//			new MustacheBuilder().parse(t, template).execute(str, new Scope(templateParams, new Scope(this)));
			
			System.out.println(str);
//			this.setResponseUTF8(ContentTypes.HTML, str.toString());
		} catch (MustacheException e) {
			log.error("Caught", e);
		} catch (Exception e) {
			log.error("Caught", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.trendrr.cheshire.CheshireController#getAnnotationClass()
	 */
	@Override
	protected Class getAnnotationClass() {
		return CheshireHTML.class;
	}
	
	
}
