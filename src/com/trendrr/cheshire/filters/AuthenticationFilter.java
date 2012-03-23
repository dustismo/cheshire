/**
 * 
 */
package com.trendrr.cheshire.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.trendrr.cheshire.CheshireController;
import com.trendrr.cheshire.authentication.AuthToken;
import com.trendrr.cheshire.authentication.AuthToken;
import com.trendrr.cheshire.authentication.AuthenticationProvider;
import com.trendrr.cheshire.authentication.InvalidAuthToken;
import com.trendrr.json.simple.JSONFormatter;
import com.trendrr.json.simple.JSONValue;
import com.trendrr.oss.DynMap;
import com.trendrr.oss.Reflection;
import com.trendrr.oss.concurrent.LazyInitObject;
import com.trendrr.oss.networking.strest.StrestRequest;
import com.trendrr.strest.StrestException;
import com.trendrr.strest.StrestHttpException;
import com.trendrr.strest.server.StrestController;
import com.trendrr.strest.server.StrestControllerFilter;


/**
 * @author Dustin Norlander
 * @created Jan 27, 2012
 * 
 */
public class AuthenticationFilter extends CheshireFilter  {

	protected static Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);
	
	public static void main(String ...strings) {
		DynMap test = new DynMap();
		test.put("test", 1);
//		AbstractAuthToken token = new AbstractAuthToken() {
//			
//		};
//		System.out.println(token);
//		test.put("auth_token", token);
//		System.out.println(test.toJSONString());
	}
	
	
	static {
		/*
		 * register a formatter for the authtoken so it serializes to json properly.
		 */
		JSONValue.registerFormatter(AuthToken.class, new JSONFormatter() {
			
			@Override
			public String toJSONString(Object value) {
				return ((AuthToken)value).toDynMap().toJSONString();
			}
		});
	}
	
	private static ConcurrentHashMap<String, List<AuthenticationProvider>> providers = new ConcurrentHashMap<String, List<AuthenticationProvider>>();
	
	private List<AuthenticationProvider> getAuthProv(String namespace, StrestController controller) {
		List<AuthenticationProvider> auth = providers.get(namespace);
		if (auth != null) {
			return auth;
		}
		auth = new ArrayList<AuthenticationProvider>();
		List<String> authProviderClasses = controller.getServerConfig().getListOrEmpty(String.class, namespace + ".authentication");
		for (String cls : authProviderClasses) {
			AuthenticationProvider ap;
			try {
				ap = Reflection.defaultInstance(AuthenticationProvider.class, cls);
				auth.add(ap);
			} catch (Exception e) {
				log.error("Unable to instantiate auth provider: " + cls, e);
			}
		}
		providers.putIfAbsent(namespace, auth);
		return providers.get(namespace);
		
	}
	
	/* (non-Javadoc)
	 * @see com.trendrr.strest.server.StrestControllerFilter#before(com.trendrr.strest.server.StrestController)
	 */
	@Override
	public void before(CheshireController controller) throws StrestException {
		
		try {
			AuthToken token = this.findAuthToken(controller);
			
			if (token == null) {
				//we dont have a token in the connection or txn.
				//now do the real checking.
				//run through all the authentication providers until we find a match.
				
				for (AuthenticationProvider ap: this.getAuthProv(controller.getControllerNamespace(), controller)) {
					if (token != null) {
						continue;
					}
					token = ap.authenticate(controller);
				}
			}
			if (token instanceof InvalidAuthToken) {
				throw StrestHttpException.UNAUTHORIZED("Invalid authentication");
			}
			
			if (token != null) {
				controller.setAuthToken(token);
				log.info("Authenticated!" + token.toDynMap().toJSONString());
			}
			
			
		} catch (StrestException e) {
			throw e;
		} catch (Exception e) {
			log.error("Caught", e);
			throw StrestHttpException.INTERNAL_SERVER_ERROR("Unable to authenticate. (" + e.getMessage() + ")");
		}
	}
	
	/**
	 * looks in the session and connection for the auth token.
	 * @return
	 */
	public static AuthToken findAuthToken(StrestController controller) throws Exception {
		AuthToken token = null;
		
		//first check txn storage.
		Object at = controller.getTxnStorage().get("auth_token");
		if (at == null) {
			//new check connection storage.
			at = controller.getConnectionStorage().get("auth_token");
		}
		if (at != null) {
			if (at instanceof AuthToken) {
				token = (AuthToken)at;
			} 
		}
		return token;
	}
	/* (non-Javadoc)
	 * @see com.trendrr.strest.server.StrestControllerFilter#after(com.trendrr.strest.server.StrestController)
	 */
	@Override
	public void after(CheshireController controller) throws StrestException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.trendrr.strest.server.StrestControllerFilter#error(com.trendrr.strest.server.StrestController, org.jboss.netty.handler.codec.http.HttpResponse, java.lang.Exception)
	 */
	@Override
	public void error(CheshireController controller, HttpResponse response,
			Exception exception) {
		// TODO Auto-generated method stub
		
	}
}
