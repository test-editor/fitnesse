package fitnesse.responders.editing;

import org.apache.velocity.VelocityContext;

import fitnesse.FitNesseContext;
import fitnesse.Responder;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.http.Response.Format;
import fitnesse.http.SimpleResponse;
import fitnesse.tm.services.TestCaseDataProviderInstance;

public class ListeTestsResponder implements Responder {

	@Override
	public Response makeResponse(FitNesseContext context, Request request) throws Exception {
		
	    VelocityContext velocityContext = new VelocityContext();
	    velocityContext.put("tests",  TestCaseDataProviderInstance.getAllTests());

	    SimpleResponse response = new SimpleResponse();
	    response.setContentType(Format.JSON);
	    response.setContent(context.pageFactory.render(velocityContext, "tests.vm"));
		return response;

	}
}
	