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

import fitnesse.tm.services.TestCaseDataProvider.MetaDataTag;

/**
 * Dieser Parser speichert State zwischen zwei findMethoden wird in der ersten
 * Zeile etwas gefunden, wird sich das gemerkt und der zweite Parser wird für
 * den nächsten Methodenaufruf eingesetzt. So können MetaDataTAgs gefunden
 * werden, deren Syntax über zwei Zeilen in der content.txt abgebildet werden,
 * bzw. nur darüber erkannt werden können.
 * 
 * Beispiel:
 * 
 * GeVo |note| Maske: B9999 Geschäftsvorfall auswählen|
 * 
 * |klicke den Link|Vertrag in Kollektiv einhängen|
 * 
 * @author u115582
 *
 */
public class DoubleLineMetaDataParser implements MetaDataParser {

	private boolean lastLineGotAHit;
	private MetaDataParser secondaryParser;
	private String firstLinePattern;

	public DoubleLineMetaDataParser(String firstLinePattern, MetaDataParser secondLineMetaDataParser) {
		this.firstLinePattern = firstLinePattern;
		this.secondaryParser = secondLineMetaDataParser;
	}

	public MetaDataTag findInContentLine(String line) {
		if (lastLineGotAHit) {
			lastLineGotAHit = false;
			return secondaryParser.findInContentLine(line);
		} else {
			if (line.matches(firstLinePattern)) {
				lastLineGotAHit = true;
			}
			return null;
		}
	}

}
