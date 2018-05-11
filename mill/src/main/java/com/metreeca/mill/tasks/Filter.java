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

package com.metreeca.mill.tasks;

import com.metreeca.mill.Task;
import com.metreeca.mill._Cell;
import com.metreeca.tray.Tool;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Arrays.asList;


/**
 * Feed filtering task.
 */
public final class Filter implements Task {

	// !!! glob/regexp patterns

	private static final Pattern AbsolutePattern=Pattern.compile("^\\w+:|^\\*");


	private Predicate<_Cell> predicate=cell -> true;


	public Filter(final String... patterns) {
		this(asList(patterns));
	}

	public Filter(final Collection<String> patterns) {

		if ( patterns == null ) {
			throw new NullPointerException("null patterns");
		}

		if ( patterns.contains(null) ) {
			throw new NullPointerException("null pattern");
		}

		this.predicate=cell -> patterns.stream().anyMatch(pattern -> cell.focus().stringValue().endsWith(pattern));

		// !!! this.patterns=patterns.stream()
		//		.map(pattern -> AbsolutePattern.matcher(pattern).lookingAt() ? pattern : "*/"+pattern)
		//		.collect(toSet());
	}

	public Filter(final Predicate<_Cell> predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		this.predicate=predicate;
	}


	@Override public Stream<_Cell> execute(final Tool.Loader tools, final Stream<_Cell> items) {
		return items.filter(predicate);
	}


	private boolean glob(String pattern, final String name) { // !!! refactor

		String rest=null;
		final int pos=pattern.indexOf('*');
		if ( pos != -1 ) {
			rest=pattern.substring(pos+1);
			pattern=pattern.substring(0, pos);
		}

		if ( pattern.length() > name.length() ) { return false; }

		// handle the part up to the first *
		for (int i=0; i < pattern.length(); i++) {
			if ( pattern.charAt(i) != '?'
					&& !pattern.substring(i, i+1).equalsIgnoreCase(name.substring(i, i+1)) ) { return false; }
		}

		// recurse for the part after the first *, if any
		if ( rest == null ) {
			return pattern.length() == name.length();
		} else {
			for (int i=pattern.length(); i <= name.length(); i++) {
				if ( glob(rest, name.substring(i)) ) { return true; }
			}
			return false;
		}
	}

}
