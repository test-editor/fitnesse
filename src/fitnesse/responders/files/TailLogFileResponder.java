package fitnesse.responders.files;

import java.nio.file.Files;
import java.nio.file.Paths;

import fitnesse.FitNesseContext;
import fitnesse.Responder;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.http.Response.Format;
import fitnesse.http.SimpleResponse;

public class TailLogFileResponder implements Responder {

	@Override
	public Response makeResponse(FitNesseContext context, Request request) throws Exception {

		String path = "C:\\Testautomatisierung\\FitNessePages\\.metadata\\logs\\interaction.log";
		byte[] encoded = Files.readAllBytes(Paths.get(path));
	    SimpleResponse response = new SimpleResponse();
	    response.setContentType(Format.TEXT_UTF8);
	    response.setContent("");
		if(encoded != null) {
			String logFile = new String(encoded, "UTF-8");
			String[] lines = logFile.split("\n");
			StringBuffer tailLogFile = new StringBuffer();
			if(lines.length < 10) {
				for(String line : lines){
					tailLogFile.append(line);
				}
			}else {
				for(int lineIndex = lines.length - 10;lineIndex < lines.length; lineIndex++ ){
					tailLogFile.append(lines[lineIndex]+ "\n");
				}
			}
		    response.setContent(tailLogFile.toString());
		}
		response.addHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
		return response;
	}

}
