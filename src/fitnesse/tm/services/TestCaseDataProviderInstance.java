package fitnesse.tm.services;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class TestCaseDataProviderInstance {
	static private Map<String,TestCaseDataProvider> instances = new TreeMap<String, TestCaseDataProvider>();
	
	public static void init(String rootPath, String projectName) throws IOException{
		instances.put(projectName, new TestCaseDataProviderImpl(rootPath, projectName));
	}
	
	public static TestCaseDataProvider getInstance(String projectName) {
		if(!instances.containsKey(projectName)){
			throw new IllegalArgumentException("unknonw project '" + projectName + "'");
		}
		return instances.get(projectName);
	}
	
}
