package fitnesse.responders;

import java.io.PrintWriter;
import java.net.Socket;

import fitnesse.FitNesseContext;
import fitnesse.authentication.SecureOperation;
import fitnesse.authentication.SecureResponder;
import fitnesse.authentication.SecureTestOperation;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.http.SimpleResponse;
import fitnesse.responders.run.StopTestServerUtil;

public class TestRunStepwiseResponder implements SecureResponder {

	public Response makeResponse(FitNesseContext context, Request request) {
		SimpleResponse response = new SimpleResponse();


		response.setContent("Run Step by Step");

		try {

			int stopTestServerPort = StopTestServerUtil.getStopTestServerPort();

			Socket socket = new Socket("localhost", stopTestServerPort);
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			out.println(StopTestServerUtil.STEPWISE_METHOD);
			socket.close();
		} catch (Exception e) {
			System.out.println(StopTestServerUtil.STEPWISE_METHOD + " could not be invoked because of " + e.getMessage()
					+ " on Port " + StopTestServerUtil.getStopTestServerPort());
		}

		return response;
	}

	public SecureOperation getSecureOperation() {
		return new SecureTestOperation();
	}

}
