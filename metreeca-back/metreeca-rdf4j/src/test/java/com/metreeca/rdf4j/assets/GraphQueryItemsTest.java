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

package com.metreeca.rdf4j.assets;

import com.metreeca.json.Shape;
import com.metreeca.json.Values;
import com.metreeca.json.queries.Items;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.Order.decreasing;
import static com.metreeca.json.Order.increasing;
import static com.metreeca.json.Values.Root;
import static com.metreeca.json.Values.term;
import static com.metreeca.json.queries.Items.items;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.filter;
import static com.metreeca.json.shapes.Link.link;
import static com.metreeca.rdf4j.assets.GraphQueryBaseTest.exec;
import static com.metreeca.rdf4j.assets.GraphTest.tuples;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

final class GraphQueryItemsTest {

	private static GraphQueryBase.Options options() {
		return new GraphQueryBase.Options() {
			@Override public <V> V get(final Supplier<V> option) {
				return option.get();
			}
		};
	}


	private Collection<Statement> query(final Items items) {
		return new GraphQueryItems(options()).process(Root, items);
	}


	@Test void testEmptyShape() {
		exec(() -> assertThat(query(

				items(and())

		)).isEmpty());
	}

	@Test void testEmptyResultSet() {
		exec(() -> {
			assertThat(query(

					items(field(RDF.TYPE, all(RDF.NIL)))

			)).isEmpty();
		});
	}

	@Test void testEmptyProjection() {
		exec(() -> assertThat(query(

				items(filter(clazz(term("Office"))))

		)).isIsomorphicTo(GraphTest.graph(

				"construct {\n"
						+"\n"
						+"\t<> ldp:contains ?office.\n"
						+"\t\n"
						+"} where {\n"
						+"\n"
						+"\t?office a :Office\n"
						+"\n"
						+"}"

		)));
	}

	@Test void testMatching() {
		exec(() -> assertThat(query(

				items(field(RDF.TYPE, filter(all(term("Employee")))))

		)).isIsomorphicTo(GraphTest.graph(

				"construct { <> ldp:contains ?employee. ?employee a :Employee }"
						+" where { ?employee a :Employee }"

		)));
	}

	@Test void testSorting() {
		exec(() -> {

			final String query="select ?employee "
					+" where { ?employee a :Employee; rdfs:label ?label; :office ?office }";

			final Shape shape=and(
					filter(clazz(term("Employee"))), field(RDFS.LABEL), field(term("office"))
			);

			final Function<Items, List<Value>> actual=edges -> query(edges)
					.stream()
					.filter(Values.pattern(null, Shape.Contains, null))
					.map(Statement::getObject)
					.distinct()
					.collect(toList());

			final Function<String, List<Value>> expected=sparql -> tuples(sparql)
					.stream()
					.map(map -> map.get("employee"))
					.distinct()
					.collect(toList());


			assertThat(actual.apply(items(shape)))
					.as("default (on value)")
					.containsExactlyElementsOf(expected.apply(query+" order by ?employee"));

			assertThat(actual.apply(items(shape, singletonList(increasing(RDFS.LABEL)))))
					.as("custom increasing")
					.containsExactlyElementsOf(expected.apply(query+" order by ?label"));

			assertThat(actual.apply(items(shape, singletonList(decreasing(RDFS.LABEL)))))
					.as("custom decreasing")
					.containsExactlyElementsOf(expected.apply(query+" order by desc(?label)"));

			assertThat(actual.apply(items(shape, asList(increasing(term("office")),
					increasing(RDFS.LABEL)))))
					.as("custom combined")
					.containsExactlyElementsOf(expected.apply(query+" order by ?office ?label"));

			assertThat(actual.apply(items(shape, singletonList(decreasing()))))
					.as("custom on root")
					.containsExactlyElementsOf(expected.apply(query+" order by desc(?employee)"));

		});
	}

	@Test void testSortingWithLink() {
		exec(() -> assertThat(

				query(items(

						and(
								filter(clazz(term("Alias"))),
								link(OWL.SAMEAS, field(term("code")))
						),

						singletonList(decreasing(term("code")))

				))
						.stream()
						.filter(Values.pattern(null, Shape.Contains, null))
						.map(Statement::getObject)
						.distinct()
						.collect(toList())

		).isEqualTo(tuples(

				"select ?alias { ?alias a :Alias; owl:sameAs/:code ?code } order by desc(?code)"

				)

						.stream()
						.map(map -> map.get("alias"))
						.collect(toList())

		));
	}

}
