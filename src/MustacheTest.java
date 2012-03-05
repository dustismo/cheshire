/**
 * 
 */

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sampullara.mustache.MustacheBuilder;
import com.sampullara.mustache.MustacheException;
import com.sampullara.mustache.Scope;


/**
 * @author Dustin Norlander
 * @created Feb 29, 2012
 * 
 */
public class MustacheTest {

	protected Log log = LogFactory.getLog(MustacheTest.class);
	
	String hello = "Hello";
	String getWorld() {return "Mustache!";}
	  
	public static void main(String ...strings) throws MustacheException, IOException {
		Writer writer = new OutputStreamWriter(System.out);
	    new MustacheBuilder().build(new StringReader("{{hello}}, {{world}}!"), "helloworld").execute(writer, new Scope(new MustacheTest()));
	    writer.flush();
	}
}
