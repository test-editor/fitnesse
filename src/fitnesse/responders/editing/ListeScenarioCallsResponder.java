package fitnesse.responders.editing;

import org.apache.velocity.VelocityContext;

import fitnesse.FitNesseContext;
import fitnesse.Responder;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.http.Response.Format;
import fitnesse.http.SimpleResponse;
import fitnesse.tm.services.TestCaseDataProvider;
import fitnesse.tm.services.TestCaseDataProviderInstance;

public class ListeScenarioCallsResponder implements Responder {

	@Override
	public Response makeResponse(FitNesseContext context, Request request) throws Exception {
		
		TestCaseDataProvider testCaseDataProvider = TestCaseDataProviderInstance.getInstance("NeuelebenTests");

	    VelocityContext velocityContext = new VelocityContext();
	    velocityContext.put("scenarioCalls",  testCaseDataProvider.getAllScenarioCalls());

	    SimpleResponse response = new SimpleResponse();
	    response.setContentType(Format.XML);
	    response.setContent(context.pageFactory.render(velocityContext, "scenarioCalls.vm"));
		return response;

	}


}
	