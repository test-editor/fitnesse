package fitnesse.responders.editing;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import fitnesse.FitNesseContext;
import fitnesse.Responder;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.http.Response.Format;
import fitnesse.http.SimpleResponse;

public abstract class XmlFileResponder implements Responder {

	@Override
	public Response makeResponse(FitNesseContext context, Request request) throws Exception {

		File testCaseDir = new File(context.getRootPagePath());
		String path = testCaseDir.getAbsolutePath() + File.separatorChar + ".." + File.separatorChar + getXmlFileName();
		byte[] encoded = Files.readAllBytes(Paths.get(path));
	    SimpleResponse response = new SimpleResponse();
	    response.setContentType(Format.XML);
	    response.setContent(new String(encoded, "UTF-8"));
		return response;
	}

	abstract protected String getXmlFileName();

}
