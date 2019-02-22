/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.form;

import org.assertj.core.api.AbstractAssert;

import static com.metreeca.form.things.Strings.indent;


public final class FocusAssert extends AbstractAssert<FocusAssert, Focus> {

	public static FocusAssert assertThat(final Focus Focus) {
		return new FocusAssert(Focus);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private FocusAssert(final Focus Focus) {
		super(Focus, FocusAssert.class);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public FocusAssert isValid() {

		isNotNull();

		if ( actual.assess(Issue.Level.Error) ) {
			failWithMessage(
					"expected focus to contain no errors but was <\n%s\n>",
					indent(actual.prune(Issue.Level.Warning))
			);
		}

		return this;
	}

	public FocusAssert isNotValid() {

		isNotNull();

		if ( !actual.assess(Issue.Level.Error) ) {
			failWithMessage("expected focus to contain errors but was <\n%s\n>", indent(actual));
		}

		return this;
	}

}
