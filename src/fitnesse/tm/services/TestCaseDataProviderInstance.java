package fitnesse.tm.services;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
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

	public static void init(String _rootPath, String _projectName) throws IOException {
		logger.info("init TestCaseDataProviderInstance");
		instance = new TestCaseDataProviderImpl(_rootPath, _projectName);
		rootPath = _rootPath;
		projectName = _projectName;
		revisionsNumber = svnService.getLocalRevisionsNumber(rootPath + File.separator + projectName);
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
		if (revisionsNumber == svnService.getLocalRevisionsNumber(rootPath + File.separator + projectName)) {
			return;
		}
		logger.info("updating to version " + svnService.getLocalRevisionsNumber(rootPath + File.separator + projectName));
		
		new Thread() {
			public void run() {
				try {
					TestCaseDataProviderImpl newInstance = new TestCaseDataProviderImpl(rootPath, projectName);
					instance = newInstance;
					revisionsNumber = svnService.getLocalRevisionsNumber(rootPath + File.separator + projectName);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				logger.info("done updating to version " + revisionsNumber);
			};
		}.start();
	}

}
