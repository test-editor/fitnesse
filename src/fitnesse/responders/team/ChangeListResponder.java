package fitnesse.responders.team;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import fitnesse.FitNesseContext;
import fitnesse.Responder;
import fitnesse.html.template.HtmlPage;
import fitnesse.html.template.PageTitle;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.http.SimpleResponse;

public class ChangeListResponder implements Responder {

	public static String PATH_DOES_NOT_EXIST = "PATH_DOES_NOT_EXIST";
	public static String PATH_IS_NOT_A_DIR = "PATH_IS_NOT_A_DIR";
	public static String COMMIT_ERROR = "COMMIT_ERROR";
	public static String NO_CONTENT_TXT = "NO_CONTENT_TXT";
	public static String NO_PROPERTIES_XML = "NO_PROPERTIES_XML";
	public static String NOT_A_TESTCASE = "NOT_A_TESTCASE";
	public static String GENERAL_EXCEPTION = "GENERAL_EXCEPTION";

	public ChangeListResponder() {
	}

	private static SVNClientManager clientManager;

	@Override
	public Response makeResponse(FitNesseContext context, Request request) throws Exception {
		return makeValidResponse(request, context);
	}

	private Response makeValidResponse(Request request, FitNesseContext context) {

		HtmlPage page = context.pageFactory.newPage();
		page.setTitle("Changes ");
		page.setPageTitle(new PageTitle("Changes"));
		page.put("localPath", request.getResource());

		page.setMainTemplate("changes");

		File testCaseDir = new File(context.getRootPagePath());
		page.put("rootPath", testCaseDir.getAbsolutePath());
		System.out.println("testCaseDir.getAbsolutePath() " + testCaseDir.getAbsolutePath());
		if (!testCaseDir.exists()) {
			return reportError(page, "Verzeichnis " + testCaseDir.getAbsolutePath() + " exisitiert nicht",
					PATH_DOES_NOT_EXIST);
		}
		if (!testCaseDir.isDirectory()) {
			return reportError(page, "Pfad " + testCaseDir.getAbsolutePath() + " ist kein Verzeichnis",
					PATH_IS_NOT_A_DIR);
		}
		File contentTxt = new File(testCaseDir.getAbsolutePath() + File.separatorChar + "content.txt");
		if (!contentTxt.exists()) {
			return reportError(page, "Das Verzeichnis " + testCaseDir.getAbsolutePath()
					+ " ist kein gültiges FitnesseVerzeichnis. Die content.txt fehlt", NO_CONTENT_TXT);
		}
		File propertiesXML = new File(testCaseDir.getAbsolutePath() + File.separatorChar + "properties.xml");
		if (!propertiesXML.exists()) {
			return reportError(page, "Das Verzeichnis " + testCaseDir.getAbsolutePath()
					+ " ist kein gültiges FitnesseVerzeichnis. Die properties.xml fehlt", NO_PROPERTIES_XML);
		}
		try {

			final String rootFilelPath = testCaseDir.getAbsolutePath();
			File rootFile = new File(rootFilelPath);
			final List<String> changedFiles = new ArrayList<String>();
			getClientManager().getStatusClient().doStatus(rootFile, SVNRevision.HEAD, SVNDepth.INFINITY, false, false,
					false, false, new ISVNStatusHandler() {
						@Override
						public void handleStatus(SVNStatus status) throws SVNException {
							SVNStatusType statusType = status.getContentsStatus();
							if (statusType != SVNStatusType.STATUS_IGNORED) {
								String filePath = getTestCase(status.getFile());
								if (filePath != null) {
									changedFiles.add(filePath.replace(File.separatorChar, '.'));
								}
							}
						}

						private String getTestCase(File file) {
							if (!file.getAbsolutePath().endsWith("content.txt")) {
								return null;
							}
							for (File sibling : file.getParentFile().listFiles()) {
								if (sibling.isDirectory()) {
									return null;
								}
							}
							return file.getAbsolutePath().substring((rootFilelPath + "\\FitNesseRoot\\").length(),
									file.getAbsolutePath().length() - "/content.txt".length());
						}
					}, null);
			System.out.println(changedFiles);
			page.put("changes", changedFiles);
		} catch (SVNException svnException) {
			svnException.printStackTrace(System.err);
			return reportError(page,
					"SVNException beim commit: '" + svnException.getMessage() + "' '"
							+ svnException.getErrorMessage().getErrorCode() + "'",
					svnException.getErrorMessage().getErrorCode().getDescription());
		} catch (Throwable e) {
			e.printStackTrace(System.err);
			return reportError(page,
					"Exception vom type " + e.getClass().getName() + " mit Nachricht '" + e.getMessage() + "'",
					GENERAL_EXCEPTION);
		}
		return getResponse(page);
	}

	private SVNClientManager getClientManager() {
		if (clientManager == null) {
			clientManager = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(true),
					SVNWCUtil.createDefaultAuthenticationManager());
		}
		return clientManager;
	}

	private Response reportError(HtmlPage page, String error, String errorId) {
		page.put("error", error);
		page.put("errorID", errorId);
		return getResponse(page);
	}

	private Response getResponse(HtmlPage page) {
		SimpleResponse response = new SimpleResponse();
		response.setContent(page.html());
		return response;
	}

}