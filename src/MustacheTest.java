/**
 * 
 */

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sampullara.mustache.MustacheBuilder;
import com.sampullara.mustache.MustacheException;
import com.sampullara.mustache.Scope;


/**
 * @author Dustin Norlander
 * @created Feb 29, 2012
 * 
 */
public class MustacheTest {

	protected static Logger log = LoggerFactory.getLogger(MustacheTest.class);
	
	String hello = "Hello";
	String getWorld() {return "Mustache!";}
	  
	public static void main(String ...strings) throws MustacheException, IOException {
		int j = 1123;
		System.out.println(j%1);
		System.out.println((j) - (j%5));
		
		
		
		Writer writer = new OutputStreamWriter(System.out);
	    new MustacheBuilder().build(new StringReader("{{hello}}, {{world}}!"), "helloworld").execute(writer, new Scope(new MustacheTest()));
	    writer.flush();
	}
}
