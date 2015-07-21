package fitnesse.responders.run;


public class StopTestServerUtil {

	public static final String CANCEL_METHOD = "tearDown";
	public static final String SUSPEND_METHOD = "suspendTest";
	public static final String RESUME_METHOD = "resumeTest";
	public static final String STEPWISE_METHOD = "stepwiseTest";
	
	public static final String CANCEL_FIXTURE = "stoppableFixture";

	public static int getStopTestServerPort() {

		String stopTestServerPort = System.getProperty("STOPTEST_SERVER_PORT");

		if (stopTestServerPort == null) {
			return 18090;
		}

		return Integer.parseInt(stopTestServerPort);

	}
}
