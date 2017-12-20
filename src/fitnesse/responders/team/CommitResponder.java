package fitnesse.responders.team;

import java.io.File;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import fitnesse.FitNesseContext;
import fitnesse.Responder;
import fitnesse.html.template.HtmlPage;
import fitnesse.html.template.PageTitle;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.http.SimpleResponse;
import fitnesse.wiki.PathParser;
import fitnesse.wiki.SystemVariableSource;
import fitnesse.wikitext.parser.Maybe;

public class CommitResponder implements Responder {

	public static String PATH_DOES_NOT_EXIST = "PATH_DOES_NOT_EXIST";
	public static String PATH_IS_NOT_A_DIR = "PATH_IS_NOT_A_DIR";
	public static String COMMIT_ERROR = "COMMIT_ERROR";
	public static String NO_CONTENT_TXT = "NO_CONTENT_TXT";
	public static String NO_PROPERTIES_XML = "NO_PROPERTIES_XML";
	public static String NOT_A_TESTCASE = "NOT_A_TESTCASE";
	public static String GENERAL_EXCEPTION = "GENERAL_EXCEPTION";

	public CommitResponder() {
	}

	@Override
	public Response makeResponse(FitNesseContext context, Request request) throws Exception {
		return makeValidResponse(request, context);
	}

	private Response makeValidResponse(Request request, FitNesseContext context) {
		SystemVariableSource variableSource = new SystemVariableSource();
		Maybe<String> rootPath = variableSource.findVariable("APPLICATION_WORK");

		String testCase = PathParser.parse(request.getResource()).toString();
		String user = request.getHeader("user");
		String password = request.getHeader("password");
		String comment = request.getInput("comment");
		
		HtmlPage page = context.pageFactory.newPage();
		page.setTitle("Commit " + testCase + ": " + rootPath.getValue());
		page.setPageTitle(new PageTitle("Commit"));
		page.put("localPath", request.getResource());
		
		page.setMainTemplate("commit");

		page.put("testCase", testCase);
		String testCasePath = context.getRootPagePath() + File.separatorChar
				+ testCase.replace('.', File.separatorChar);
		page.put("testCasePath", testCasePath);

		File testCaseDir = new File(testCasePath);
		page.put("rootPath", testCaseDir.getAbsolutePath());
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
		for (File containingFile : testCaseDir.listFiles()) {
			if (containingFile.isDirectory()) {
				return reportError(page,
						"Pfad " + testCaseDir.getAbsolutePath()
								+ " hat Unterverzeichnisse und kann deshalb nicht als TestVerzeichnis erkannt werden",
						NOT_A_TESTCASE);
			}
		}

		String svnComment = comment != null ? comment : "";
		try {
			ISVNAuthenticationManager authManager = null;
			if(user != null && password != null){
				authManager = SVNWCUtil.createDefaultAuthenticationManager(user,password.toCharArray());
			}else {
				authManager = SVNWCUtil.createDefaultAuthenticationManager();
			}
			SVNClientManager clientManager = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(true),
					authManager);
			SVNCommitClient cc = clientManager.getCommitClient();

			SVNCommitInfo doCommit = cc.doCommit(new File[] { testCaseDir }, false, svnComment, null, null, false, true,
					SVNDepth.INFINITY);
			if (doCommit.getErrorMessage() != null) {
				return reportError(page, "Fehler beim commit: '" + doCommit.getErrorMessage().getFullMessage() + "'" + doCommit.getErrorMessage().getErrorCode(),
						COMMIT_ERROR);
			}
			page.put("message", "commited with revision " + doCommit.getNewRevision());
			page.put("revision", doCommit.getNewRevision());
		} catch (SVNException svnException) {
			svnException.printStackTrace(System.err);
			return reportError(page, "SVNException beim commit: '" + svnException.getMessage() + "' '"+ svnException.getErrorMessage().getErrorCode() + "'", svnException.getErrorMessage().getErrorCode().getDescription());
		} catch (Throwable e) {
			e.printStackTrace(System.err);
			return reportError(page, "Exception vom type " + e.getClass().getName() + " mit Nachricht '" +  e.getMessage() + "'",
					GENERAL_EXCEPTION);
		}

		return getResponse(page);
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
