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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface TestCaseDataProvider {

	Collection<Test> getAllTests() throws IOException;
	Collection<ScenarioCall> getAllScenarioCalls() throws IOException;
	Collection<Suite> getAllSuites() throws IOException;

	public static class ScenarioCall {
		private List<String> parameters = new ArrayList<String>();
		private String path;
		private String name;
		ScenarioCall(String name, String path, List<String> parameters){
			this.name = name;
			this.path = path;
			if(parameters != null){
				this.parameters.addAll(parameters);
			}
		}
		public List<String> getParameters() {
			return parameters;
		}
		public String getPath() {
			return path;
		}
		public String getName() {
			return name;
		}
		public String toString() {
			String toString = "name: '"+ name + "', path: '" + path + "'";
			for(int parameterIndex = 0; parameterIndex < parameters.size(); parameterIndex++){
				toString +=", parameter" + parameterIndex + ": '" + parameters.get(parameterIndex) + "'";
			}
			return toString;
		}
	}

	public class MetaDataTag {
		private String name; 
		private String value;

		public MetaDataTag(String name, String value){
			this.name = name;
			this.value = value;
		}
		@Override
		public String toString() {
			return name + ": " + value;
		}

		public String getName() {
			return name;
		}
		public String getValue() {
			return value;
		}
	}

	public class Test {
		
		public Test(String path) {
			this.path = path;
			if(path.indexOf('.') != -1){
				this.name = path.substring(path.lastIndexOf('.')+1);
			}else {
				this.name = path;
			}
		}
		public String getName() {
			return name;
		}
		public String getContent() {
			return content;
		}
		public void setContent(String content) {
			this.content = content;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public String getPath() {
			return path;
		}
		public List<MetaDataTag> getMetaDataTags() {
			return metaDataTags;
		}
		private List<MetaDataTag> metaDataTags = new ArrayList<MetaDataTag>();
		private String path;
		private String name;
		private String content;
		private String description;
	}

	public static class Suite {
		private String name;
		private Collection<String> testReferences = new ArrayList<String>();
		
		public Suite(String name){
			this.name = name;
		}
		public String getName() {
			return name;
		}

		public Collection<String> getTestReferences() {
			return testReferences;
		}

	}
}
