/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.json;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Condition;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;


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
				Values.format(actual)
		));
	}

	public ValueAssert hasNamespace(final String namespace) {

		if ( namespace == null ) {
			throw new NullPointerException("null namespace");
		}

		return isIRI().satisfies(new Condition<>(
				value -> ((IRI)value).getNamespace().equals(namespace),
				"expected <%s> to be in the <%s> namespace",
				Values.format(actual), namespace
		));
	}

}
