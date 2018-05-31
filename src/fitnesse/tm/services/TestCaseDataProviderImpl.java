/*******************************************************************************
 * Copyright (c) 2012 - 2015 Signal Iduna Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Signal Iduna Corporation - initial API and implementation
 * akquinet AG
 *******************************************************************************/
package fitnesse.tm.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import fitnesse.tm.parser.DoubleLineMetaDataParser;
import fitnesse.tm.parser.MetaDataParser;
import fitnesse.tm.parser.RegExpMetaDataParser;
import fitnesse.tm.parser.StandardDropDownMetaDataParser;

public class TestCaseDataProviderImpl implements TestCaseDataProvider {

	private static final Logger logger = Logger.getLogger(TestCaseDataProviderImpl.class.getName());

	public static final int ERROR_CODE_NOT_A_DIRECTORY = -1;
	public static final int ERROR_CODE_NOT_A_TESEDITOR_ROOT = -2;
	public static final int ERROR_CODE_PROJECT_NOT_FOUND = -3;
	public static final int ERROR_CODE_IO_PROBLEM = -7;

	private String rootPath;
	private String projectName;

	private Map<String, String> testCaseMap = new TreeMap<String, String>();
	private Map<String, String> szenarioMap = new TreeMap<String, String>();
	private Map<String, Set<String>> szenarioUsage = new TreeMap<String, Set<String>>();
	private List<ScenarioCall> scenarioCalls = new ArrayList<ScenarioCall>();
	private List<Suite> suites = new ArrayList<Suite>();
	private Collection<Test> tests = new ArrayList<Test>();

	static private Set<String> skipRootFolder = null;

	public TestCaseDataProviderImpl(String rootPath, String projectName) throws IOException {
		if (skipRootFolder == null) {
			skipRootFolder = new TreeSet<String>();
			skipRootFolder.add("PageHeader");
			skipRootFolder.add("PageFooter");
			skipRootFolder.add("SetUp");
			skipRootFolder.add("TearDown");
			skipRootFolder.add("SuiteSetUp");
			skipRootFolder.add("SuiteTearDown");
			skipRootFolder.add("ScenarioLibrary");
			skipRootFolder.add("TemplateLibrary");
		}
		this.rootPath = rootPath;
		this.projectName = projectName;
		int errorCode = readProjectFiles();
		if (errorCode < 0) {
			throw new RuntimeException("Error " + errorCode + " during reading fitnessFiles");
		}
        listFiles(new File(rootPath), rootPath, suites);
		readTests();
	}
	private int readProjectFiles() {

		File rootFolder = new File(rootPath + File.separatorChar + projectName);
		if (!rootFolder.exists() || !rootFolder.isDirectory()) {
			logger.log(Level.SEVERE, rootPath + " conains no FitNesseRoot");
			return ERROR_CODE_PROJECT_NOT_FOUND;
		}

		try {
			readFiles(rootFolder, "", projectName, true);
			resolveSzenarios();
		} catch (IOException e) {
			logger.log(Level.SEVERE, e.getMessage());
			return ERROR_CODE_IO_PROBLEM;
		}
		return 0;
	}

	private void readFiles(File folder, String path, String projectName, boolean root) throws IOException {
		File[] fileArray = folder.listFiles();

		for (int i = 0; i < fileArray.length; i++) {
			String localPath = path;
			if (fileArray[i].isFile() && !root) {
				if ("content.txt".equals(fileArray[i].getName())) {
					try {
						readFitnessFile(localPath, fileArray[i], projectName);
					} catch (IOException e) {
						throw new IOException("could not read file " + path + "." + fileArray[i].getName() + ". Reason "
								+ e.getMessage(), e);
					}
				}
			} else if (fileArray[i].isDirectory()) {
				if (localPath.length() > 0) {
					localPath += ".";
				}
				localPath += fileArray[i].getName();
				if (!root || !skipRootFolder.contains(fileArray[i].getName())) {
					readFiles(fileArray[i], localPath, projectName, false);
				}
			}
		}
	}

	private void readFitnessFile(String path, File file, String projectName) throws IOException {

		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		StringBuffer content = new StringBuffer();
		boolean szenario = false;
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("!|scenario")) {
					szenario = true;
				}
				content.append(line + "\n");
			}
		} finally {
			reader.close();
		}
		if (szenario) {
			String szenarioLines[] = content.toString().split("\\n");
			for (int lineIndex = 0; lineIndex < szenarioLines.length; lineIndex++) {
				if (szenarioLines[lineIndex].startsWith("!|scenario |")) {
					String szenarioCallLine = szenarioLines[lineIndex].substring("!|scenario |".length());
					String callElements[] = szenarioCallLine.split("\\|");
					if (callElements.length < 1) {
						throw new RuntimeException("illegal scenario call :" + szenarioCallLine);
					}
					String call = callElements[0];
					if (call.endsWith("_")) {
						call = call.substring(0, call.length() - 1);
					}
					call = call.trim();
					List<String> parameters = new ArrayList<String>();
					if (callElements.length > 1) {
						String parameterStrings[] = callElements[1].split(",");
						for (int parameterIndex = 0; parameterIndex < parameterStrings.length; parameterIndex++) {
							parameters.add(parameterStrings[parameterIndex].trim());
						}
					}
					String parentPath = path;
					if (path.indexOf(".") > 1) {
						parentPath = projectName + "." + parentPath.substring(0, parentPath.lastIndexOf("."));
					}

					scenarioCalls.add(new ScenarioCall(call, parentPath, parameters));
				}
			}
			szenarioMap.put(path, content.toString());
		} else {
			String contentString = content.toString();
			if (!contentString.startsWith("!content")) {
				testCaseMap.put(projectName + "." + path, contentString);
			}
		}

	}

	private void resolveSzenarios() throws IOException {

		for (String testCase : testCaseMap.keySet()) {
			String testCaseContent = testCaseMap.get(testCase);
			try {
				resolveSzenariosReferences(testCase, testCaseContent);
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Fehler in " + testCase + ". Fehler: " + e.getMessage());
			}
			testCaseMap.put(testCase, testCaseContent);
		}
	}

	private void resolveSzenariosReferences(String testCase, String testCaseContent) {
		logger.log(Level.FINE, "resolving testcase '" + testCase + "'");
		String includeKeyString = "!include <" + projectName + ".";

		StringTokenizer stringTokenizer = new StringTokenizer(testCaseContent, "\n");
		while (stringTokenizer.hasMoreTokens()) {
			String line = stringTokenizer.nextToken();
			if (line.startsWith(includeKeyString)) {
				String szenarioKey = line.substring(includeKeyString.length());
				if (!szenarioMap.containsKey(szenarioKey)) {
					throw new RuntimeException("Unbekanntes  Szenario '" + szenarioKey + "' gefunden.'");
				}
				resolveSzenariosReferences(testCase, szenarioMap.get(szenarioKey));
				if (!szenarioUsage.containsKey(szenarioKey)) {
					szenarioUsage.put(szenarioKey, new TreeSet<String>());
				}
				szenarioUsage.get(szenarioKey).add(testCase);
			}
		}
		return;
	}

	public Set<String> getAllTestCaseNames() {
		return testCaseMap.keySet();
	}

	private String getSourceInternal(String testCaseKey) {
		if (testCaseMap.containsKey(testCaseKey)) {
			return testCaseMap.get(testCaseKey);
		}
		if (szenarioMap.containsKey(testCaseKey)) {
			return szenarioMap.get(testCaseKey);
		}
		return null;
	}

	private Set<String> getAllSzenarioKeys() {
		return szenarioMap.keySet();
	}

	public String getRootPath() {
		return rootPath;
	}

	static final public String SZENARIO_END_MARKER = "+++++++++++++++++++++++++";

	public List<String> getSource(String key) {
		if (!getAllTestCaseNames().contains(key)) {
			throw new IllegalArgumentException("Der Testfall '" + key + "' wurde nicht im DataProvider gefunden");
		}
		String content = getSourceInternal(key);
		List<String> list = new ArrayList<String>();
		list.addAll(Arrays.asList(content.split("\n")));

		Iterator<String> iterator = list.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().trim().length() == 0) {
				iterator.remove();
			}
		}

		Map<String, String> scenarioKeys = new TreeMap<String, String>();
		for (int index = 0; index < list.size(); index++) {
			String line = list.get(index);
			if (line.startsWith("!include <")) {
				String scenarioKey = line.substring(line.indexOf('.') + 1).trim();
				String name = scenarioKey.trim();
				if (name.indexOf('.') != -1) {
					name = name.substring(name.lastIndexOf('.') + 1);
				}
				if (!getAllSzenarioKeys().contains(scenarioKey)) {
					logger.log(Level.SEVERE, "Der Szenario '" + scenarioKey + "' wurde nicht im DataProvider gefunden");
					// throw new IllegalArgumentException(
					// "Der Szenario '" + scenarioKey + "' wurde nicht im
					// DataProvider gefunden");
				}
				index++;
				line = index < list.size() ? list.get(index) : "";
				if ("!|script|".equals(line)) {
					index++;
					line = index < list.size() ? list.get(index) : "";
					if (!line.startsWith("|") || !line.endsWith("|")) {
						throw new IllegalArgumentException(
								"Zeile nach !|script|' in dem Testfall '" + key + "' hat nicht das richtige Format");
					}
					String name2 = line.substring(1, line.length() - 1).replaceAll("\\s", "");
					if (!name.equals(name2)) {
						throw new IllegalArgumentException(
								"Der Szenarioname aus dem Include '" + name + "' und dem Aufruf '" + name2
										+ "' passen in dem Testfall '" + key + "' nicht zusammen");
					}
					expandSzenario(list, index, getSourceInternal(scenarioKey));
				} else {
					if (line.startsWith("!include <")) {
						index--;
					}
					scenarioKeys.put(name, scenarioKey);
				}
			}
			if (line.startsWith("!|") && !line.startsWith("!|scenario |")) {
				String szenarioName = line.substring(2, line.length() - 1).replaceAll("\\s", "");
				if (!scenarioKeys.containsKey(szenarioName)) {
					throw new IllegalArgumentException("Szenarienaufruf " + szenarioName + " wurde nicht gefunden");
				}
				index++;
				line = index < list.size() ? list.get(index) : "";
				List<String> parameter = convertStringToArray(line);
				if (parameter.size() == 0) {
					throw new IllegalArgumentException(
							"Keine Parameter gefunden für Szenario " + szenarioName + " in zeile '" + line + "'");
				}
				List<String> scenarien = new ArrayList<String>();
				index++;
				line = index < list.size() ? list.get(index) : "";
				do {
					List<String> values = convertStringToArray(line);
					if (values.size() < parameter.size()) {
						for (int index2 = values.size(); index2 < parameter.size(); index2++) {
							values.add("");
						}
					}
					String scenarienContent = getSourceInternal(scenarioKeys.get(szenarioName));
					if (scenarienContent != null) {
						scenarienContent = replaceParameter(parameter, values, scenarienContent);
						scenarienContent += "\n" + SZENARIO_END_MARKER + "\n";
						scenarien.add(scenarienContent);
					} else {
						logger.log(Level.SEVERE, "no szenario for scenarioKeys '" + scenarioKeys + "'");
					}
					index++;
					line = index < list.size() ? list.get(index) : "";
				} while (!"".equals(line) && !"#".equals(line));

				for (int index2 = scenarien.size() - 1; index2 >= 0; index2--) {
					list.addAll(index, Arrays.asList(StringUtils.split(scenarien.get(index2), "\n")));
				}
				index--;
			}
			if ("|note|scenario|".equals(line)) {
				index++;
				line = index < list.size() ? list.get(index) : "";
				List<String> values = convertStringToArray(line);
				if (values.size() == 0) {
					throw new IllegalArgumentException(
							"Ungültiger Szenarienaufruf in Zeile '" + line + "' für Testfall " + key);
				}
				String szenarioName = values.remove(0);
				if (szenarioName.endsWith(";")) {
					szenarioName = szenarioName.substring(0, szenarioName.length() - 1);
				}
				if (!scenarioKeys.containsKey(szenarioName)) {
					throw new IllegalArgumentException("Szenarienaufruf " + szenarioName + " wurde nicht gefunden");
				}
				String scenarioContent = getSourceInternal(scenarioKeys.get(szenarioName));
				List<String> parameter = getParameterFromScenario(scenarioContent);
				if (values.size() < parameter.size()) {
					for (int index2 = values.size(); index2 < parameter.size(); index2++) {
						values.add("");
					}
				}
				scenarioContent = replaceParameter(parameter, values, scenarioContent);
				expandSzenario(list, index, scenarioContent);
			}
		}
		return list;

	}

	private String replaceParameter(List<String> parameter, List<String> values, String scenarioContent) {
		for (int index2 = 0; index2 < parameter.size(); index2++) {
			String value = values.get(index2);
			value = value.replaceAll("\\$", "\\\\\\$");
			String param = parameter.get(index2);
			if (param.startsWith("{")) {
				param = "\\" + param;
			}
			scenarioContent = scenarioContent.replaceAll("@" + param, value);
			scenarioContent = scenarioContent.replaceAll("@\\{" + param + "\\}", value);
		}
		return scenarioContent;
	}

	private List<String> getParameterFromScenario(String scenarioContent) {
		List<String> list = new ArrayList<String>();
		list.addAll(Arrays.asList(scenarioContent.split("\n")));
		Iterator<String> lineIterator = list.iterator();
		String firstLine = null;
		do {
			firstLine = lineIterator.next();
		} while (firstLine.startsWith("!includ") || firstLine.trim().length() == 0);

		firstLine = StringUtils.replace(firstLine, "||", "| |");
		String[] parts = StringUtils.split(firstLine, "|");
		if (parts.length != 4) {
			throw new IllegalArgumentException("Ungültige erste Zeile eines Szenarios: '" + firstLine + "'");
		}
		parts = parts[3].split(",");
		List<String> parameters = new ArrayList<String>();
		for (String parameter : parts) {
			if (parameter.trim().length() > 0) {
				parameters.add(parameter.trim());
			}
		}
		return parameters;
	}

	private void expandSzenario(List<String> list, int index, String szenario) {
		Iterator<String> iterator;
		szenario += "\n" + SZENARIO_END_MARKER + "\n";
		List<String> subList = new ArrayList<String>();
		subList.addAll(Arrays.asList(szenario.split("\n")));
		iterator = subList.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().trim().length() == 0) {
				iterator.remove();
			}
		}
		if (index + 1 < list.size()) {
			list.addAll(index + 1, subList);
		} else {
			list.addAll(subList);
		}
	}

	private List<String> convertStringToArray(String line) {
		line = StringUtils.replace(line, "||", "| |");
		List<String> list = new ArrayList<String>(Arrays.asList(StringUtils.split(line, "|")));
		for (int index = 0; index < list.size(); index++) {
			list.set(index, list.get(index).trim());
		}
		return list;
	}

	@Override
	public Collection<ScenarioCall> getAllScenarioCalls() throws IOException {
		return scenarioCalls;
	}


	private void listFiles(File directory, String root, List<Suite> suites) throws IOException {
		for (File file : directory.listFiles()) {
			// TODO: Bitte durch vernünftiges Parsen ersetzen
			if (file.getAbsolutePath().endsWith(".txt") && file.length() > 40) {
				BufferedReader brTest = null;
				try {
					brTest = new BufferedReader(new FileReader(file));
					String line = brTest.readLine();
					if (line != null && line.startsWith("!contents")) {
						String suiteName = file.getAbsolutePath();
						suiteName = suiteName.substring(suiteName.indexOf(root) + root.length() + 1);
						suiteName = suiteName.substring(0, suiteName.length() - "/content.txt".length());
						suiteName = suiteName.replace('\\', '.');
						Suite suite = new Suite(suiteName);
						while (line != null) {
							if (line.startsWith("!see .")) {
								suite.getTestReferences().add(line.substring("!see .".length()).trim());
							}
							line = brTest.readLine();
						}
						if(suite.getTestReferences().size() > 0){
							suites.add(suite);
						}
					}
				} finally {
					brTest.close();
				}
			}
			if (!file.getName().equals(".svn") && file.isDirectory()) {
				listFiles(file, root, suites);
			}
		}
	}

	private void readTests() {
		List<MetaDataParser> parserList = new ArrayList<MetaDataParser>();

		parserList.add(new RegExpMetaDataParser(
				"^(\\|.*Produkt\\|)([^|]*)(\\|.*aus Tabelle\\|B1100a_TableLinkProduktGeneration\\|)$", "Produkt", 2));

		parserList.add(new RegExpMetaDataParser(
				"^(\\|.*aus Generation\\|)([^|]*)(\\|aus Tabelle\\|B1100a_TableLinkProduktGeneration\\|)$",
				"Generation", 2));

		parserList.add(new RegExpMetaDataParser("^(\\|waehle Tarif aus Tabelle\\|)([^|]*)(\\|)", "Tarif", 2));

		parserList.add(new DoubleLineMetaDataParser("\\|note\\| Maske: B9999 Geschäftsvorfall auswählen\\|",
				new RegExpMetaDataParser("^(\\|klicke den Link\\|)([^|]*)(\\|)$", "GeVo", 2)));

		parserList.add(new StandardDropDownMetaDataParser("B1300_DropdownZahlweise", "Zahlweise"));
		parserList.add(new StandardDropDownMetaDataParser("B1100_DropdownProduktgruppe", "Produktgruppe"));

		for (String key :  getAllTestCaseNames()) {
			Test test = new Test(key);
			for (String line :  getSource(key)) {
				for (MetaDataParser metaDataParser : parserList) {
					MetaDataTag tag = metaDataParser.findInContentLine(line);
					if (tag != null) {
						test.getMetaDataTags().add(tag);
					}
				}
			}
			tests.add(test);
		}
	}

	@Override
	public Collection<Suite> getAllSuites() throws IOException {
		return suites;
	}

	@Override
	public Collection<Test> getAllTests() throws IOException {
		return tests;
	}
	public static void main(String args[]) throws IOException {
		String rootPath = "C:\\Testautomatisierung\\FitNessePages";
		String projectName = "NeuelebenTests";
		TestCaseDataProviderImpl TestCaseDataProviderImpl = new TestCaseDataProviderImpl(rootPath, projectName);
		for(Suite suite : TestCaseDataProviderImpl.getAllSuites()){
			System.out.println(suite.getName());
		}
		;
	}
}
