// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.reporting.history;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;
import java.util.logging.Level;

import javax.swing.text.DateFormatter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.json.JSONArray;
import org.json.JSONObject;

import fitnesse.FitNesseContext;
import fitnesse.reporting.BaseFormatter;
import fitnesse.testrunner.WikiTestPage;
import fitnesse.testsystems.Assertion;
import fitnesse.testsystems.ExceptionResult;
import fitnesse.testsystems.ExecutionLogListener;
import fitnesse.testsystems.ExecutionResult;
import fitnesse.testsystems.Expectation;
import fitnesse.testsystems.Instruction;
import fitnesse.testsystems.TableCell;
import fitnesse.testsystems.TestResult;
import fitnesse.testsystems.TestSummary;
import fitnesse.testsystems.TestSystem;
import fitnesse.util.TimeMeasurement;
import fitnesse.wiki.PageData;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.WikiPageUtil;

public class TestXmlFormatter extends BaseFormatter implements ExecutionLogListener, Closeable {
	private final FitNesseContext context;
	private final WriterFactory writerFactory;
	private TimeMeasurement currentTestStartTime;
	private TimeMeasurement totalTimeMeasurement;
	private StringBuilder outputBuffer;
	protected final TestExecutionReport testResponse;
	private TestExecutionReport.TestResult currentResult;

	public TestXmlFormatter(FitNesseContext context, final WikiPage page, WriterFactory writerFactory) {
		super(page);
		this.context = context;
		this.writerFactory = writerFactory;
		totalTimeMeasurement = new TimeMeasurement().start();
		testResponse = new TestExecutionReport(context.version, page.getPageCrawler().getFullPath().toString());
		resetTimer();
	}

	public long startedAt() {
		return totalTimeMeasurement.startedAt();
	}

	public long runTime() {
		return currentTestStartTime.elapsed();
	}

	@Override
	public void testStarted(WikiTestPage testPage) {
		resetTimer();
		appendHtmlToBuffer(WikiPageUtil.getHeaderPageHtml(getPage()));
		currentResult = newTestResult();
		testResponse.addResult(currentResult);
		currentResult.relativePageName = testPage.getName();
		currentResult.tags = testPage.getData().getAttribute(PageData.PropertySUITES);
	}

	@Override
	public void testOutputChunk(String output) {
		appendHtmlToBuffer(output);
	}

	@Override
	public void testAssertionVerified(Assertion assertion, TestResult testResult) {
		if (testResult == null) {
			return;
		}
		Instruction instruction = assertion.getInstruction();
		Expectation expectation = assertion.getExpectation();
		TestExecutionReport.InstructionResult instructionResult = new TestExecutionReport.InstructionResult();
		currentResult.addInstruction(instructionResult);

		String id = instruction.getId();

		instructionResult.instruction = instruction.toString();
		instructionResult.slimResult = testResult.toString();
		try {
			TestExecutionReport.Expectation expectationResult = new TestExecutionReport.Expectation();
			instructionResult.addExpectation(expectationResult);
			expectationResult.instructionId = id;
			expectationResult.type = expectation.getClass().getSimpleName();
			expectationResult.actual = testResult.getActual();
			expectationResult.expected = testResult.getExpected();
			expectationResult.evaluationMessage = testResult.getMessage();
			if (testResult.getExecutionResult() != null) {
				expectationResult.status = testResult.getExecutionResult().toString();
			}
			if (expectation instanceof TableCell) {
				TableCell cell = (TableCell) expectation;
				expectationResult.col = Integer.toString(cell.getCol());
				expectationResult.row = Integer.toString(cell.getRow());
			}
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Unable to process assertion " + assertion + " with test result " + testResult, e);
		}
	}

	@Override
	public void testExceptionOccurred(Assertion assertion, ExceptionResult exceptionResult) {
		Instruction instruction = assertion.getInstruction();
		Expectation expectation = assertion.getExpectation();
		TestExecutionReport.InstructionResult instructionResult = new TestExecutionReport.InstructionResult();
		currentResult.addInstruction(instructionResult);

		String id = instruction.getId();

		instructionResult.instruction = instruction.toString();
		try {
			TestExecutionReport.Expectation expectationResult = new TestExecutionReport.Expectation();
			instructionResult.addExpectation(expectationResult);
			expectationResult.instructionId = id;
			expectationResult.type = expectation.getClass().getSimpleName();
			expectationResult.evaluationMessage = exceptionResult.getMessage();
			expectationResult.status = exceptionResult.getExecutionResult().toString();
		} catch (Exception e) {
			LOG.log(Level.WARNING,
					"Unable to process assertion " + assertion + " with exception result " + exceptionResult, e);
		}
	}

	@Override
	public void testComplete(WikiTestPage test, TestSummary testSummary) throws IOException {
		currentTestStartTime.stop();
		super.testComplete(test, testSummary);
		currentResult.startTime = currentTestStartTime.startedAt();
		addCountsToResult(currentResult, testSummary);
		currentResult.runTimeInMillis = String.valueOf(currentTestStartTime.elapsed());
		testResponse.tallyPageCounts(ExecutionResult.getExecutionResult(test.getName(), testSummary));
	}

	@Override
	public void testSystemStopped(TestSystem testSystem, Throwable cause) {
		super.testSystemStopped(testSystem, cause);
		if (cause != null) {
			testResponse.tallyPageCounts(ExecutionResult.ERROR);
		}
	}

	protected TestExecutionReport.TestResult newTestResult() {
		return new TestExecutionReport.TestResult();
	}

	@Override
	public void close() throws IOException {
		setTotalRunTimeOnReport(totalTimeMeasurement);

		if (currentResult != null) {
			currentResult.content = outputBuffer == null ? null : outputBuffer.toString();
			outputBuffer = null;
		}
		writeResults();
	}

	private void resetTimer() {
		currentTestStartTime = new TimeMeasurement().start();
	}

	protected void setTotalRunTimeOnReport(TimeMeasurement totalTimeMeasurement) {
		testResponse.setTotalRunTimeInMillis(totalTimeMeasurement);
	}

	protected void writeResults() throws IOException {
		WikiPage page = getPage();

		String testName = "";
		while (!page.isRoot()) {
			if (testName.length() > 0) {
				testName = "." + testName;
			}
			testName = page.getName() + testName;
			page = page.getParent();
		} 
		writeResults(writerFactory.getWriter(context, getPage(), getPageCounts(), totalTimeMeasurement.startedAt()),
				testName);
	}

	@Override
	public int getErrorCount() {
		return getPageCounts().getWrong() + getPageCounts().getExceptions();
	}

	protected void writeResults(Writer writer, String testName) throws IOException {
		VelocityContext velocityContext = new VelocityContext();
		velocityContext.put("response", testResponse);
		Template template = context.pageFactory.getVelocityEngine().getTemplate("testResults.vm");
		template.merge(velocityContext, writer);
		writer.close();

		StringWriter stringWriter = new StringWriter();
		template.merge(velocityContext, stringWriter);
		if (System.getProperty("testMgmtServer") != null) {
			LOG.fine("pushing result to " + System.getProperty("testMgmtServer"));
			try {
				URL url = new URL(System.getProperty("testMgmtServer"));
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-Type", "application/json");
				conn.setDoOutput(true);
				conn.connect();

				JSONObject jsonObject1 = new JSONObject();
				jsonObject1.put("attachment-files", new JSONArray());
				jsonObject1.put("content", stringWriter.toString());
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
				jsonObject1.put("date", dateFormat.format(new Date()));
				jsonObject1.put("testname", testName);

				OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
				out.write(jsonObject1.toString());
				out.close();

				if (conn.getResponseCode() != 200) {
					throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
				}

				BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

				StringBuffer result = new StringBuffer();
				String output;
				while ((output = br.readLine()) != null) {
					result.append(output);
				}
				if (!"true".equals(result.toString())) {
					LOG.severe("the serve testMgmtServer " + System.getProperty("testMgmtServer") + " could not read the testresutl");
					LOG.severe("Message '" + result.toString() + "'");
				}
			} catch (Exception e) {
				LOG.severe("the serve testMgmtServer " + System.getProperty("testMgmtServer") + " has thrown an exception");
				LOG.severe("Message " + e.getMessage());
				e.printStackTrace();
			}
		} else {
			LOG.fine("testMgmtServer is not set. Result ist not published on the TestMgmtServer");
		}

	}

	protected TestSummary getPageCounts() {
		return testResponse.getFinalCounts();
	}

	private void addCountsToResult(TestExecutionReport.TestResult currentResult, TestSummary testSummary) {
		currentResult.right = Integer.toString(testSummary.getRight());
		currentResult.wrong = Integer.toString(testSummary.getWrong());
		currentResult.ignores = Integer.toString(testSummary.getIgnores());
		currentResult.exceptions = Integer.toString(testSummary.getExceptions());
	}

	private void appendHtmlToBuffer(String output) {
		if (outputBuffer == null) {
			outputBuffer = new StringBuilder();
		}
		outputBuffer.append(output);
	}

	@Override
	public void commandStarted(ExecutionContext context) {
		testResponse.addExecutionContext(context.getCommand(), context.getTestSystemName());
	}

	@Override
	public void stdOut(String output) {
		testResponse.addStdOut(output);
	}

	@Override
	public void stdErr(String output) {
		testResponse.addStdErr(output);
	}

	@Override
	public void exitCode(int exitCode) {
		testResponse.exitCode(exitCode);
	}

	@Override
	public void exceptionOccurred(Throwable e) {
		testResponse.exceptionOccurred(e);
	}

	public interface WriterFactory {
		Writer getWriter(FitNesseContext context, WikiPage page, TestSummary counts, long time) throws IOException;
	}

}
