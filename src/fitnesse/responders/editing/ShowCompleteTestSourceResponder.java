package fitnesse.responders.editing;

import fitnesse.FitNesseContext;
import fitnesse.Responder;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.http.Response.Format;
import fitnesse.http.SimpleResponse;
import fitnesse.tm.services.TestCaseDataProviderInstance;

public class ShowCompleteTestSourceResponder implements Responder {

	@Override
	public Response makeResponse(FitNesseContext context, Request request) throws Exception {
		
	    StringBuffer source = new StringBuffer();
	    for(String line: TestCaseDataProviderInstance.getTestSource(request.getInput("path"))){
	    	source.append(line + "\r\n");
	    }

	    SimpleResponse response = new SimpleResponse();
	    response.setContentType(Format.TEXT_UTF8);
	    response.setContent(source.toString());
		return response;

	}
}
	