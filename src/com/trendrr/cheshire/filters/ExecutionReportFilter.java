/**
 * 
 */
package com.trendrr.cheshire.filters;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.trendrr.cheshire.CheshireController;
import com.trendrr.oss.StringHelper;
import com.trendrr.oss.concurrent.LazyInit;
import com.trendrr.oss.concurrent.LazyInitObject;
import com.trendrr.oss.executionreport.ExecutionReport;
import com.trendrr.strest.StrestException;
import com.trendrr.strest.server.v2.models.StrestResponse;

/**
 * @author Dustin Norlander
 * @created Nov 8, 2011
 * 
 */
public class ExecutionReportFilter extends CheshireFilter {

	protected Log log = LogFactory.getLog(ExecutionReportFilter.class);
	
	static ExecutionReport report = null;
	static LazyInit lock = new LazyInit();
	
	/* (non-Javadoc)
	 * @see com.trendrr.v4.filters.TrendrrApiFilter#before(com.trendrr.v4.TrendrrApiController)
	 */
	@Override
	public void before(CheshireController controller) throws StrestException {
		if (lock.start()) {
			try {
				report = ExecutionReport.instance(controller.getServerConfig().getString("execution_report.name"));
			} finally {
				lock.end();
			}
		}
		if (controller != null && controller.getTxnStorage() != null) {
		controller.getTxnStorage().put("_ex_report_start", new Date());
		}
	}

	/* (non-Javadoc)
	 * @see com.trendrr.v4.filters.TrendrrApiFilter#after(com.trendrr.v4.TrendrrApiController)
	 */
	@Override
	public void after(CheshireController controller) throws StrestException {
		
//		StringBuilder str = new StringBuilder();

		String route = controller.routes()[0];
		route = StringHelper.trim(route, "/");
		if (route.isEmpty())
			route = "root";
		
		Date start = (Date)controller.getTxnStorage().get("_ex_report_start");
		
		route = escape(route);
		report.inc(route, start);
		report.inc("", start);
//		for (String path: route.split("\\/")) {
//			str.append(path);
//			report.inc(str.toString(), start);
//			str.append(".");
//		}
	}

	public static String escape(String key) {
		key = key.replaceAll("\\/", "_sl_");
		key = key.replaceAll("\\*", "_ax_");
		key = key.replaceAll("\\:", "_cl_");
		return key;
	}
	public static String unescape(String key) {
		key = key.replaceAll("\\_sl\\_", "/");
		key = key.replaceAll("\\_ax\\_", "*");
		key = key.replaceAll("\\_cl\\_", ":");
		return key;
	}
	
	/* (non-Javadoc)
	 * @see com.trendrr.v4.filters.TrendrrApiFilter#error(com.trendrr.v4.TrendrrApiController, org.jboss.netty.handler.codec.http.HttpResponse, java.lang.Exception)
	 */
	@Override
	public void error(CheshireController controller, StrestResponse response,
			Exception exception) {
		Date start = null;
		if (controller != null) {
			start = (Date)controller.getTxnStorage().get("_ex_report_start");
		}
		
		report.inc("controller-errors", start);
	}
}
