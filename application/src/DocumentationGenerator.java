/**
 * 
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.trendrr.cheshire.CheshireGlobals;
import com.trendrr.oss.DynMap;
import com.trendrr.strest.doc.StrestDocParser;


/**
 * 
 * Simple main class to generate the models for the documentation.
 * 
 * 
 * @author Dustin Norlander
 * @created Mar 5, 2012
 * 
 */
public class DocumentationGenerator extends StrestDocParser {

	protected static Logger log = LoggerFactory.getLogger(DocumentationGenerator.class);
	
	public static void main(String ...args) {
		CheshireGlobals.baseDir = "application/";
		DocumentationGenerator generator = new DocumentationGenerator();
		generator.addAnnotationName("CheshireApi");
		generator.parseAndSave(CheshireGlobals.baseDir + "src", CheshireGlobals.baseDir + "strestdoc");
	}
	
	
	@Override
	protected DynMap cleanUpRoute(DynMap route) {
		DynMap map = super.cleanUpRoute(route);
		if (map.getBoolean("authenticate", true) || !map.getListOrEmpty(String.class, "access").isEmpty()) {
			map.put("authenticate", true);
		}
		return map;
	}

	/**
	 * Dont add index entries for any api methods that have no documentation.
	 */
	@Override
	public DynMap createIndexEntry(DynMap route) {
		DynMap map = super.createIndexEntry(route);
		String abst = map.getString("abstract");
		if (abst == null || abst.isEmpty())
			return null;
		map.put("authenticate", route.getBoolean("authenticate"));
		if (route.containsKey("access")) {
			map.put("access", route.get("access"));
		}
		return map;	
	}
}
