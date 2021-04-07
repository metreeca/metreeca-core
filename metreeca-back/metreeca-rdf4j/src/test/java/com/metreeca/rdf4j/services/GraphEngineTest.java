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
import com.metreeca.json.Shape;
import com.metreeca.rest.services.Engine;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.ValuesTest.decode;
import static com.metreeca.json.ValuesTest.small;
import static com.metreeca.json.queries.Items.items;
import static com.metreeca.json.queries.Terms.terms;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.filter;
import static com.metreeca.rdf4j.services.GraphTest.exec;
import static com.metreeca.rdf4j.services.GraphTest.model;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Collections.singletonList;

final class GraphEngineTest {

	private static final IRI Employee=term("Employee");

	private static final IRI code=term("code");
	private static final IRI forename=term("forename");
	private static final IRI surname=term("surname");
	private static final IRI email=term("email");
	private static final IRI title=term("title");
	private static final IRI office=term("office");
	private static final IRI seniority=term("seniority");
	private static final IRI supervisor=term("supervisor");
	private static final IRI subordinate=term("subordinate");


	private final Frame collection=frame(item("/employees/"));
	private final Frame resource=frame(item("/employees/1370"));

	private final Shape shape=and(

			filter(clazz(Employee)),

			field(RDF.TYPE),
			field(RDFS.LABEL),
			field(RDFS.COMMENT),

			field(code),
			field(forename),
			field(surname),
			field(email),
			field(title),
			field(office),
			field(seniority),
			field(supervisor),
			field(subordinate)

	);

	private final Frame delta=frame(resource.focus())
			.set(forename).string("Tino")
			.set(surname).string("Faussone")
			.set(email).string("tfaussone@classicmodelcars.com")
			.set(title).string("tfaussone@classicmodelcars.com")
			.set(seniority).integer(1);


	@Nested final class Create {

		@Test void testCreate() {
			exec(() -> assertThat(new GraphEngine()

					.create(delta, shape)

			).hasValueSatisfying(frame -> assertThat(model())
					.as("resource description stored into the graph")
					.hasSubset(frame.model())
			));
		}

		@Test void testConflictingSlug() {
			exec(() -> {

				final Engine engine=new GraphEngine();

				engine.create(delta, shape);

				final Model snapshot=model();

				assertThat(engine.create(delta, shape))
						.as("clash reported")
						.isEmpty();

				assertThat(model())
						.as("graph unchanged")
						.isIsomorphicTo(snapshot);

			});
		}

	}

	@Nested final class Relate {

		@Test void testRelate() {
			exec(model(small()), () -> assertThat(new GraphEngine()

					.relate(resource, items(shape))

			).hasValueSatisfying(frame -> assertThat(frame.model())
					.as("items retrieved")
					.isSubsetOf(model(
							"construct where { <employees/1370> ?p ?o }"
					))
			));
		}


		@Test void testBrowse() {
			exec(model(small()), () -> assertThat(new GraphEngine()

					.relate(collection, items(shape))

			).hasValueSatisfying(frame -> assertThat(frame.model())
					.hasStatement(collection.focus(), Shape.Contains, null)
					.hasSubset(model("construct { ?e :email ?email; :seniority ?seniority }\n"
							+"where { ?e a :Employee; :email ?email; :seniority ?seniority }"
					))
			));
		}

		@Test void testBrowseFiltered() {
			exec(model(small()), () -> assertThat(new GraphEngine()

					.relate(collection, items(and(shape, filter(field(title, all(literal("Sales Rep")))))))

			).hasValueSatisfying(frame -> assertThat(frame.model())

					.hasSubset(model(""
							+"construct { ?e :title ?t; :seniority ?seniority }\n"
							+"where { ?e a :Employee; :title ?t, 'Sales Rep'; :seniority ?seniority }"
					))

					.as("only resources matching filter included")
					.doesNotHaveStatement(null, term("title"), literal("President"))
			));
		}

		@Test void testSliceTermsQueries() {
			exec(model(small()), () -> assertThat(new GraphEngine()

					.relate(collection, terms(shape, singletonList(office), 1, 3))

			).hasValueSatisfying(frame -> assertThat(frame.model())

					.isIsomorphicTo(model(""
							+"construct { \n"
							+"\n"
							+"\t<employees/> :terms [:value ?o; :count ?c]. \n"
							+"\t?o rdfs:label ?l\n"
							+"\n"
							+"} where { { select ?o ?l (count(?e) as ?c) {\n"
							+"\n"
							+"\t?e a :Employee; :office ?o.\n"
							+"\t?o rdfs:label ?l.\n"
							+"\n"
							+"} group by ?o ?l order by desc(?c) offset 1 limit 3 } }"
					))

			));
		}

	}

	@Nested final class Update {

		@Test void testUpdate() {
			exec(model(small()), () -> assertThat(new GraphEngine()

					.update(delta, shape)

			).hasValueSatisfying(frame -> assertThat(model())

					.as("updated values inserted")
					.hasSubset(frame.model())

					.as("previous values removed")
					.doesNotHaveSubset(decode("</employees/1370>"
							+":forename 'Gerard';"
							+":surname 'Hernandez'."
					))

			));
		}

		@Test void testReportUnknown() {
			exec(model(small()), () -> assertThat(new GraphEngine()

					.update(frame(item("/employees//unknown")), shape)

			).isEmpty());
		}

	}

	@Nested final class Delete {

		@Test void testDelete() {
			exec(model(small()), () -> assertThat(new GraphEngine()

					.delete(resource, shape)

			).hasValueSatisfying(frame -> {

				assertThat(model("construct where { <employees/1370> ?p ?o }")).isEmpty();
				assertThat(model("construct where { ?s a :Employee; ?p ?o. }")).isNotEmpty();

			}));
		}

		@Test void testReportUnknown() {
			exec(model(small()), () -> assertThat(new GraphEngine()

					.delete(frame(item("/employees//unknown")), shape)

			).isEmpty());
		}

	}

}