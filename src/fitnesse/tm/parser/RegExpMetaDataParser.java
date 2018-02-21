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
package fitnesse.tm.parser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fitnesse.tm.services.TestCaseDataProvider.MetaDataTag;

public class RegExpMetaDataParser implements MetaDataParser {

	private String parent;
	private int valueRegExpGroup;
	private Pattern pattern;

	public RegExpMetaDataParser(String pattern, String parent, int valueRegExpGroup) {
		this.pattern = Pattern.compile(pattern);
		this.parent = parent;
		this.valueRegExpGroup = valueRegExpGroup;
	}

	public MetaDataTag findInContentLine(String line) {
		Matcher matcher = pattern.matcher(line);
		while (matcher.find()) {
			String value = matcher.group(valueRegExpGroup);
			
			if (value != null && value.length() > 0) {
				MetaDataTag tag = new MetaDataTag(parent, value);
				return tag;
			}
		}
		return null;
	}

}
