package fitnesse.responders.team;

import java.io.File;

import org.apache.velocity.VelocityContext;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import fitnesse.FitNesseContext;
import fitnesse.Responder;
import fitnesse.http.Request;
import fitnesse.http.Response;
import fitnesse.http.Response.Format;
import fitnesse.http.SimpleResponse;

public class StatusResponder implements Responder {

	private SvnService svnService = SvnService.getInstance();

	public StatusResponder() {
	}

	@Override
	public Response makeResponse(FitNesseContext context, Request request) throws Exception {
		final String localRoot = "C:\\Testautomatisierung\\FitNessePages\\NeuelebenTests";
		final String remoteRoot = "http://svn1.system.local/anonsvn/lvneu/NLv/testautomatisierung/trunk/";
		VelocityContext velocityContext = new VelocityContext();
		if(svnService.updating){
			velocityContext.put("fitnesseStatus", "UPDATING");
		}else {
			velocityContext.put("fitnesseStatus", "RUNNING");
		}

		long localRevisionsNumber = svnService.getLocalRevisionsNumber(localRoot);

		if ((localRevisionsNumber < svnService.getRemoteRevisionsNumber(remoteRoot))) {
			svnService.updateSVN(localRoot);
			localRevisionsNumber = svnService.getLocalRevisionsNumber(localRoot);
		}
		velocityContext.put("svnUpToDate", localRevisionsNumber == svnService.getRemoteRevisionsNumber(remoteRoot));
		velocityContext.put("revisionNumber", localRevisionsNumber);

		SimpleResponse response = new SimpleResponse();
		response.setContentType(Format.JSON);
		response.setContent(context.pageFactory.render(velocityContext, "status.vm"));
		response.setStatus(200);
		return response;
	}

	public static class SvnService {
		private static SvnService instance = new SvnService();
		private boolean updating = false;

		private Long lastLocalRevisionNumber;

		public long getLocalRevisionsNumber(String localRoot) {
			if (!updating) {
				final SVNWCClient client = getClient();
				SVNInfo doInfo;
				try {
					doInfo = client.doInfo(new File(localRoot), null);
					lastLocalRevisionNumber = doInfo.getRevision().getNumber();
				} catch (SVNException e) {
					e.printStackTrace();
				}
			}
			return lastLocalRevisionNumber;
		}

		public long getRemoteRevisionsNumber(String remoteRoot) throws SVNException {
			final SVNWCClient client = getClient();

			SVNURL snvurl = SVNURL.parseURIEncoded(remoteRoot);
			SVNInfo doInfo = client.doInfo(snvurl, SVNRevision.HEAD, SVNRevision.HEAD);
			return doInfo.getRevision().getNumber();
		}

		public synchronized void updateSVN(final String localRoot) {
			if(updating){
				return;
			}
			updating = true;
			Thread thread = new Thread() {
				public void run() {
					ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager();
					File file = new File(localRoot);

					SVNClientManager cm = null;
					SVNUpdateClient uc = null;
					try {
						cm = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(true), authManager);
						uc = cm.getUpdateClient();
						uc.doUpdate(new File[] { file }, SVNRevision.HEAD, SVNDepth.INFINITY, true, true);
					} catch (SVNException e) {
						if (uc != null) {
							SVNWCClient wcc = cm.getWCClient();
							try {
								wcc.doCleanup(file, true);
								uc.doUpdate(new File[] { file }, SVNRevision.HEAD, SVNDepth.INFINITY, true, true);
							} catch (SVNException ex) {
								System.out.println(e);
								ex.printStackTrace();
							}
						}
					} finally {
						if (cm != null) {
							cm.dispose();
						}
						updating = false;
					}

				};
			};
			thread.start();
		}

		private SVNWCClient getClient() {
			ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager();

			ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
			return new SVNWCClient(authManager, options);
		}

		public static SvnService getInstance() {
			return instance;
		}

	}
}