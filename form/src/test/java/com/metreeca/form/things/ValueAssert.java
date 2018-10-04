/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.form.things;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Condition;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import static com.metreeca.form.things.Values.format;


public final class ValueAssert extends AbstractAssert<ValueAssert, Value> {

	public static ValueAssert assertThat(final Value value) {
		return new ValueAssert(value);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private ValueAssert(final Value value) {
		super(value, ValueAssert.class);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public ValueAssert isIRI() {
		return isNotNull().satisfies(new Condition<>(
				value -> value instanceof IRI,
				"expected <%s> to be an IRI",
				format(actual)
		));
	}

	public ValueAssert hasNamespace(final String namespace) {

		if ( namespace == null ) {
			throw new NullPointerException("null namespace");
		}

		return isIRI().satisfies(new Condition<>(
				value -> ((IRI)value).getNamespace().equals(namespace),
				"expected <%s> to be in the <%s> namespace",
				format(actual), namespace
		));
	}

}
