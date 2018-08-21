package fitnesse.responders.files;

import java.nio.file.Files;
import java.nio.file.Paths;

import fitnesse.FitNesseContext;
import fitnesse.Responder;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.http.Response.Format;
import fitnesse.http.SimpleResponse;

public class LogFileResponder implements Responder {

	@Override
	public Response makeResponse(FitNesseContext context, Request request) throws Exception {

		String path = "C:\\Testautomatisierung\\FitNessePages\\.metadata\\logs\\interaction.log";
		byte[] encoded = Files.readAllBytes(Paths.get(path));
	    SimpleResponse response = new SimpleResponse();
	    response.setContentType(Format.TEXT_UTF8);
	    response.setContent(encoded);
		response.addHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
		return response;
	}

}
