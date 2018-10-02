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

package com.metreeca.form.probes;

import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.shapes.When;
import com.metreeca.form.shifts.Step;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.Test.test;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.shapes.Virtual.virtual;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;


final class RedactorTest {

	private static final Redactor empty=new Redactor(emptyMap());

	private static final Redactor first=new Redactor(singletonMap(RDF.VALUE, singleton(RDF.FIRST)));
	private static final Redactor rest=new Redactor(singletonMap(RDF.VALUE, singleton(RDF.REST)));
	private static final Redactor any=new Redactor(singletonMap(RDF.VALUE, singleton(Form.any)));


	@Test void testIgnoreUnconditionalShapes() {

		final Shape shape=and();

		assertThat((Object)shape).as("unconditional shape").isEqualTo(shape.accept(empty));

	}

	@Test void testIgnoreUndefinedConditions() {

		final When when=When.when(RDF.VALUE, RDF.FIRST);

		assertThat((Object)when).as("undefined variable").isEqualTo(when.accept(empty));

	}

	@Test void testReplaceDefinedConditions() {

		final When when=When.when(RDF.VALUE, RDF.FIRST);

		assertThat((Object)and()).as("included value").isEqualTo(when.accept(first));
		assertThat((Object)or()).as("excluded value").isEqualTo(when.accept(rest));
		assertThat((Object)and()).as("wildcard value").isEqualTo(when.accept(any));

	}

	@Test void testRedactNestedShapes() {

		final When nested=When.when(RDF.VALUE, RDF.FIRST);

		assertThat((Object)trait(RDF.VALUE, and())).as("trait").isEqualTo(trait(RDF.VALUE, nested).accept(first));

		assertThat((Object)virtual(trait(RDF.VALUE, and()), Step.step(RDF.NIL))).as("virtual").isEqualTo(virtual(trait(RDF.VALUE, nested), Step.step(RDF.NIL)).accept(first));

		assertThat((Object)and(and())).as("conjunction").isEqualTo(and(nested).accept(first));
		assertThat((Object)or(and())).as("disjunction").isEqualTo(or(nested).accept(first));
		assertThat((Object)test(and(), and(), and())).as("option").isEqualTo(test(and(), and(), nested).accept(first));

	}

}
