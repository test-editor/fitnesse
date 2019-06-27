package fitnesse.tm.services;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import fitnesse.responders.team.StatusResponder.SvnService;
import fitnesse.tm.services.TestCaseDataProvider.ScenarioCall;
import fitnesse.tm.services.TestCaseDataProvider.Suite;
import fitnesse.tm.services.TestCaseDataProvider.Test;

public class TestCaseDataProviderInstance {
	
	private static final Logger logger = Logger.getLogger(TestCaseDataProviderInstance.class.getName());

	static TestCaseDataProvider instance = null;
	private static SvnService svnService = SvnService.getInstance();
	private static long revisionsNumber;
	private static String rootPath;
	private static String projectName;
	private static boolean updating = false;

	public static void init(String _rootPath, String _projectName) throws IOException {
		logger.info("init TestCaseDataProviderInstance");
		instance = new TestCaseDataProviderImpl(_rootPath, _projectName);
		rootPath = _rootPath;
		projectName = _projectName;
		revisionsNumber = svnService.getLocalRevisionsNumber();
	}

	public static List<String> getTestSource(String key) {
		startUpdate();
		return instance.getSource(key);
	}
	public static Collection<Test> getAllTests() throws IOException {
		startUpdate();
		return instance.getAllTests();
	}

	public static Collection<ScenarioCall> getAllScenarioCalls() throws IOException {
		startUpdate();
		return instance.getAllScenarioCalls();
	}

	public static Collection<Suite> getAllSuites() throws IOException {
		startUpdate();
		return instance.getAllSuites();
	}
	static public long getRevisionNumber() {
		return revisionsNumber;
	}
	public static synchronized void startUpdate() {
		logger.info("Check for " + revisionsNumber);
		if (!updating && revisionsNumber == svnService.getLocalRevisionsNumber()) {
			return;
		}
		updating = true;
		logger.info("updating to version " + svnService.getLocalRevisionsNumber());
		
		new Thread() {
			public void run() {
				try {
					TestCaseDataProviderImpl newInstance = new TestCaseDataProviderImpl(rootPath, projectName);
					instance = newInstance;
				} catch (IOException e) {
					throw new RuntimeException(e);
				} finally {
					updating = false;
					revisionsNumber = svnService.getLocalRevisionsNumber();
				}
				logger.info("done updating to version " + revisionsNumber);
			};
		}.start();
	}

}
