/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.tray.iam.policies;

import com.metreeca.tray.iam.Policy;

import org.eclipse.rdf4j.model.IRI;

import java.security.SecureRandom;
import java.util.regex.Pattern;


public final class BasicPolicy extends Policy { // !!! factor from custom policy

	private static final Pattern GroupingPattern=Pattern.compile(".{3}\\B");

	private static final SecureRandom random=new SecureRandom();


	private final int llength=8;
	private final int ulength=32;


	@Override public boolean verify(final IRI usr, final String pwd) {

		if ( usr == null ) {
			throw new NullPointerException("null usr");
		}

		if ( pwd == null ) {
			throw new NullPointerException("null pwd");
		}

		return pwd.length() >= llength;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private char upper() {
		return character('A', 'Z');
	}

	private char lower() {
		return character('a', 'z');
	}

	private char digit() {
		return character('0', '9');
	}

	private char character(final char lower, final char upper) {
		return (char)(random.nextInt(upper-lower+1)+lower);
	}


	private char[] shuffle(final char... chars) { // see https://stackoverflow.com/a/18456998/739773

		for (int i=chars.length-1; i > 0; i--) {

			final int j=random.nextInt(i+1);
			final char c=chars[j];

			chars[j]=chars[i];
			chars[i]=c;
		}

		return chars;
	}

}
