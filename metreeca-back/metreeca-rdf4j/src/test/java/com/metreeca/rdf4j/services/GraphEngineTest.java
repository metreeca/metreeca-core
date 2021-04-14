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

package com.metreeca.rdf4j.services;

import com.metreeca.json.Frame;
import com.metreeca.json.Values;
import com.metreeca.rest.services.EngineTest;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.Order.decreasing;
import static com.metreeca.json.Order.increasing;
import static com.metreeca.json.Shape.Contains;
import static com.metreeca.json.queries.Items.items;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.filter;
import static com.metreeca.json.shapes.Link.link;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

final class GraphEngineTest extends EngineTest {

	@Override protected void exec(final Runnable... tasks) {
		GraphTest.exec(tasks);
	}

	@Override protected GraphEngine engine() {
		return new GraphEngine();
	}


	@Test void testSortingDefault() {
		exec(dataset(), () -> assertThat(engine().relate(frame(container), items(

				EmployeeShape

		)).map(frame ->

				frame.frames(Contains).collect(toList())

		)).hasValue(resources.stream()
				.filter(frame -> frame.value(RDF.TYPE).filter(Employee::equals).isPresent())
				.sorted(comparing(Frame::focus, Values::compare))
				.collect(toList())
		));
	}

	@Test void testSortingCustomOnItem() {
		exec(dataset(), () -> assertThat(engine().relate(frame(container), items(

				EmployeeShape, singletonList(decreasing())

		)).map(frame ->

				frame.frames(Contains).collect(toList())

		)).hasValue(resources.stream()
				.filter(frame -> frame.value(RDF.TYPE).filter(Employee::equals).isPresent())
				.sorted(comparing(Frame::focus, Values::compare).reversed())
				.collect(toList())
		));
	}

	@Test void testSortingCustomIncreasing() {
		exec(dataset(), () -> assertThat(engine().relate(frame(container), items(

				EmployeeShape, singletonList(increasing(RDFS.LABEL))

		)).map(frame ->

				frame.frames(Contains).collect(toList())

		)).hasValue(resources.stream()
				.filter(frame -> frame.value(RDF.TYPE).filter(Employee::equals).isPresent())
				.sorted(comparing(frame -> frame.string(RDFS.LABEL).orElse("")))
				.collect(toList())
		));
	}

	@Test void testSortingCustomDecreasing() {
		exec(dataset(), () -> assertThat(engine().relate(frame(container), items(

				EmployeeShape, singletonList(decreasing(RDFS.LABEL))

		)).map(frame ->

				frame.frames(Contains).collect(toList())

		)).hasValue(resources.stream()
				.filter(frame -> frame.value(RDF.TYPE).filter(Employee::equals).isPresent())
				.sorted(Comparator.<Frame, String>comparing(frame -> frame.string(RDFS.LABEL).orElse("")).reversed())
				.collect(toList())
		));
	}

	@Test void testSortingCustomMultiple() {
		exec(dataset(), () -> assertThat(engine().relate(frame(container), items(

				EmployeeShape, asList(increasing(office), increasing(RDFS.LABEL))

		)).map(frame ->

				frame.frames(Contains).collect(toList())

		)).hasValue(resources.stream()
				.filter(frame -> frame.value(RDF.TYPE).filter(Employee::equals).isPresent())
				.sorted(Comparator
						.<Frame, Value>comparing(frame -> frame.value(office).orElse(null), Values::compare)
						.thenComparing(frame -> frame.string(RDFS.LABEL).orElse(""))
				)
				.collect(toList())
		));
	}

	@Test void testSortingWithLinks() {
		exec(dataset(), () -> assertThat(engine().relate(frame(container), items(

				and(filter(clazz(Alias)), link(OWL.SAMEAS, field(code))), singletonList(decreasing(code))

		)).map(frame ->

				frame.frames(Contains).collect(toList())

		)).hasValue(resources.stream()
				.filter(frame -> frame.value(RDF.TYPE).filter(Alias::equals).isPresent())
				.sorted(comparing(frame -> frame.value(code).orElse(null), Values::compare)) // !!! projection?
				.collect(toList())
		));
	}

}