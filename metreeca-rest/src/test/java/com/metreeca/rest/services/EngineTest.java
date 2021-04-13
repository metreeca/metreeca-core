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

package com.metreeca.rest.services;

import com.metreeca.json.Frame;
import com.metreeca.json.Shape;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.FrameAssert.assertThat;
import static com.metreeca.json.Shape.*;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.queries.Items.items;
import static com.metreeca.json.queries.Terms.terms;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.filter;
import static com.metreeca.json.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.json.shapes.MinInclusive.minInclusive;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Map.Entry.comparingByKey;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

public abstract class EngineTest {

	private static final IRI base=iri("http://example.com/");
	private static final IRI term=item("/terms/");

	private static IRI item(final String name) {
		return iri(base, name);
	}

	private static IRI term(final String name) {
		return iri(term, name);
	}


	private static final IRI Alias=item("Alias");
	private static final IRI Employee=item("Employee");

	private static final IRI code=term("code");
	private static final IRI forename=term("forename");
	private static final IRI surname=term("surname");
	private static final IRI email=term("email");
	private static final IRI title=term("title");
	private static final IRI seniority=term("seniority");
	private static final IRI office=term("office");
	private static final IRI supervisor=term("supervisor");
	private static final IRI subordinate=term("subordinate");


	private static final Shape EmployeeShape=and(

			filter(clazz(Employee)),

			field(RDF.TYPE, exactly(Employee)),

			field(RDFS.LABEL, required(), datatype(XSD.STRING)),
			field(RDFS.COMMENT, optional(), datatype(XSD.STRING)),

			field(code, required(), required(), datatype(XSD.STRING)),

			field(forename, required(), datatype(XSD.STRING)),
			field(surname, required(), datatype(XSD.STRING)),

			field(email, required(), datatype(XSD.STRING)),
			field(title, required(), datatype(XSD.STRING)),
			field(seniority, required(), minInclusive(literal(1)), maxInclusive(literal(5))),

			field(office, required()),
			field(supervisor, optional()),
			field(subordinate, multiple())

	);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final IRI container=item("/employees/");
	private static final IRI resource=iri(container, "/1370");

	private static final Frame delta=frame(resource) // !!! merge original frame
			.value(RDF.TYPE, Employee)
			.string(forename, "Tino")
			.string(surname, "Faussone")
			.string(email, "tfaussone@classicmodelcars.com")
			.string(title, "Sales Rep")
			.integer(seniority, 1);

	private static final Collection<Frame> resources=unmodifiableList(asList(

			frame(item("/aliases/1002"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1002")),

			frame(item("/employees/1002"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "Diane Murphy")
					.string(code, "1002")
					.string(forename, "Diane")
					.string(surname, "Murphy")
					.string(email, "dmurphy@classicmodelcars.com")
					.string(title, "President")
					.integer(seniority, 5)
					.value(office, item("/offices/1"))
					.values(subordinate,
							item("/employees/1056"),
							item("/employees/1076")
					),

			frame(item("/aliases/1056"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1056")),

			frame(item("/employees/1056"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "Mary Patterson")
					.string(code, "1056")
					.string(forename, "Mary")
					.string(surname, "Patterson")
					.string(email, "mpatterso@classicmodelcars.com")
					.string(title, "VP Sales")
					.integer(seniority, 4)
					.value(office, item("/offices/1"))
					.value(supervisor, item("/employees/1002"))
					.values(subordinate,
							item("/employees/1088"),
							item("/employees/1102"),
							item("/employees/1143"),
							item("/employees/1621")
					),

			frame(item("/aliases/1076"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1076")),

			frame(item("/employees/1076"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "Jeff Firrelli")
					.string(code, "1076")
					.string(forename, "Jeff")
					.string(surname, "Firrelli")
					.string(email, "jfirrelli@classicmodelcars.com")
					.string(title, "VP Marketing")
					.integer(seniority, 4)
					.value(office, item("/offices/1"))
					.value(supervisor, item("/employees/1002")),

			frame(item("/aliases/1088"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1088")),

			frame(item("/employees/1088"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "William Patterson")
					.string(code, "1088")
					.string(forename, "William")
					.string(surname, "Patterson")
					.string(email, "wpatterson@classicmodelcars.com")
					.string(title, "Sales Manager (APAC)")
					.integer(seniority, 3)
					.value(office, item("/offices/6"))
					.value(supervisor, item("/employees/1056"))
					.values(subordinate,
							item("/employees/1611"),
							item("/employees/1612"),
							item("/employees/1619")
					),

			frame(item("/aliases/1102"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1102")),

			frame(item("/employees/1102"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "Gerard Bondur")
					.string(code, "1102")
					.string(forename, "Gerard")
					.string(surname, "Bondur")
					.string(email, "gbondur@classicmodelcars.com")
					.string(title, "Sale Manager (EMEA)")
					.integer(seniority, 4)
					.value(office, item("/offices/4"))
					.value(supervisor, item("/employees/1056"))
					.values(subordinate,
							item("/employees/1337"),
							item("/employees/1370"),
							item("/employees/1401"),
							item("/employees/1501"),
							item("/employees/1504"),
							item("/employees/1702")
					),

			frame(item("/aliases/1143"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1143")),

			frame(item("/employees/1143"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "Anthony Bow")
					.string(code, "1143")
					.string(forename, "Anthony")
					.string(surname, "Bow")
					.string(email, "abow@classicmodelcars.com")
					.string(title, "Sales Manager (NA)")
					.integer(seniority, 3)
					.value(office, item("/offices/1"))
					.value(supervisor, item("/employees/1056"))
					.values(subordinate,
							item("/employees/1165"),
							item("/employees/1166"),
							item("/employees/1188"),
							item("/employees/1216"),
							item("/employees/1286"),
							item("/employees/1323")
					),

			frame(item("/aliases/1165"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1165")),

			frame(item("/employees/1165"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "Leslie Jennings")
					.string(code, "1165")
					.string(forename, "Leslie")
					.string(surname, "Jennings")
					.string(email, "ljennings@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 1)
					.value(office, item("/offices/1"))
					.value(supervisor, item("/employees/1143")),

			frame(item("/aliases/1166"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1166")),

			frame(item("/employees/1166"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "Leslie Thompson")
					.string(code, "1166")
					.string(forename, "Leslie")
					.string(surname, "Thompson")
					.string(email, "lthompson@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 1)
					.value(office, item("/offices/1"))
					.value(supervisor, item("/employees/1143")),

			frame(item("/aliases/1188"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1188")),

			frame(item("/employees/1188"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "Julie Firrelli")
					.string(code, "1188")
					.string(forename, "Julie")
					.string(surname, "Firrelli")
					.string(email, "jfirrelli@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 1)
					.value(office, item("/offices/2"))
					.value(supervisor, item("/employees/1143")),

			frame(item("/aliases/1216"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1216")),

			frame(item("/employees/1216"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "Steve Patterson")
					.string(code, "1216")
					.string(forename, "Steve")
					.string(surname, "Patterson")
					.string(email, "spatterson@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 2)
					.value(office, item("/offices/2"))
					.value(supervisor, item("/employees/1143")),

			frame(item("/aliases/1286"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1286")),

			frame(item("/employees/1286"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "Foon Yue Tseng")
					.string(code, "1286")
					.string(forename, "Foon Yue")
					.string(surname, "Tseng")
					.string(email, "ftseng@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 1)
					.value(office, item("/offices/3"))
					.value(supervisor, item("/employees/1143")),

			frame(item("/aliases/1323"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1323")),

			frame(item("/employees/1323"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "George Vanauf")
					.string(code, "1323")
					.string(forename, "George")
					.string(surname, "Vanauf")
					.string(email, "gvanauf@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 1)
					.value(office, item("/offices/3"))
					.value(supervisor, item("/employees/1143")),

			frame(item("/aliases/1337"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1337")),

			frame(item("/employees/1337"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "Loui Bondur")
					.string(code, "1337")
					.string(forename, "Loui")
					.string(surname, "Bondur")
					.string(email, "lbondur@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 1)
					.value(office, item("/offices/4"))
					.value(supervisor, item("/employees/1102")),

			frame(item("/aliases/1370"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1370")),

			frame(item("/employees/1370"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "Gerard Hernandez")
					.string(code, "1370")
					.string(forename, "Gerard")
					.string(surname, "Hernandez")
					.string(email, "ghernande@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 2)
					.value(office, item("/offices/4"))
					.value(supervisor, item("/employees/1102")),

			frame(item("/aliases/1401"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1401")),

			frame(item("/employees/1401"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "Pamela Castillo")
					.string(code, "1401")
					.string(forename, "Pamela")
					.string(surname, "Castillo")
					.string(email, "pcastillo@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 2)
					.value(office, item("/offices/4"))
					.value(supervisor, item("/employees/1102")),

			frame(item("/aliases/1501"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1501")),

			frame(item("/employees/1501"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "Larry Bott")
					.string(code, "1501")
					.string(forename, "Larry")
					.string(surname, "Bott")
					.string(email, "lbott@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 2)
					.value(office, item("/offices/7"))
					.value(supervisor, item("/employees/1102")),

			frame(item("/aliases/1504"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1504")),

			frame(item("/employees/1504"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "Barry Jones")
					.string(code, "1504")
					.string(forename, "Barry")
					.string(surname, "Jones")
					.string(email, "bjones@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 1)
					.value(office, item("/offices/7"))
					.value(supervisor, item("/employees/1102")),

			frame(item("/aliases/1611"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1611")),

			frame(item("/employees/1611"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "Andy Fixter")
					.string(code, "1611")
					.string(forename, "Andy")
					.string(surname, "Fixter")
					.string(email, "afixter@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 1)
					.value(office, item("/offices/6"))
					.value(supervisor, item("/employees/1088")),

			frame(item("/aliases/1612"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1612")),

			frame(item("/employees/1612"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "Peter Marsh")
					.string(code, "1612")
					.string(forename, "Peter")
					.string(surname, "Marsh")
					.string(email, "pmarsh@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 1)
					.value(office, item("/offices/6"))
					.value(supervisor, item("/employees/1088")),

			frame(item("/aliases/1619"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1619")),

			frame(item("/employees/1619"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "Tom King")
					.string(code, "1619")
					.string(forename, "Tom")
					.string(surname, "King")
					.string(email, "tking@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 2)
					.value(office, item("/offices/6"))
					.value(supervisor, item("/employees/1088")),

			frame(item("/aliases/1621"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1621")),

			frame(item("/employees/1621"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "Mami Nishi")
					.string(code, "1621")
					.string(forename, "Mami")
					.string(surname, "Nishi")
					.string(email, "mnishi@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 2)
					.value(office, item("/offices/5"))
					.value(supervisor, item("/employees/1056"))
					.values(subordinate,
							item("/employees/1625")
					),

			frame(item("/aliases/1625"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1625")),

			frame(item("/employees/1625"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "Yoshimi Kato")
					.string(code, "1625")
					.string(forename, "Yoshimi")
					.string(surname, "Kato")
					.string(email, "ykato@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 2)
					.value(office, item("/offices/5"))
					.value(supervisor, item("/employees/1621")),

			frame(item("/aliases/1702"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, item("/employees/1702")),

			frame(item("/employees/1702"))
					.value(RDF.TYPE, Employee)
					.string(RDFS.LABEL, "Martin Gerard")
					.string(code, "1702")
					.string(forename, "Martin")
					.string(surname, "Gerard")
					.string(email, "mgerard@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 2)
					.value(office, item("/offices/4"))
					.value(supervisor, item("/employees/1102"))

	));


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected abstract Engine engine();

	protected abstract void exec(final Runnable... tasks);


	private Runnable dataset() {
		return () -> {

			final Engine engine=engine();

			resources.forEach(frame -> engine.create(frame, EmployeeShape));

		};
	}

	private Optional<Frame> snapshot() {
		return engine().relate(frame(container), items(EmployeeShape));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Nested final class Create {

		@Test void testCreate() {
			exec(() -> {

				final Engine engine=engine();

				assertThat(engine.create(delta, EmployeeShape).map(Frame::focus)).hasValue(resource);
				assertThat(engine.relate(delta, items(EmployeeShape))).hasValue(delta);

			});
		}

		@Test void testReportConflicting() {
			exec(() -> {

				final Engine engine=engine();

				engine.create(delta, EmployeeShape);

				final Optional<Frame> snapshot=snapshot();

				assertThat(engine.create(delta, EmployeeShape)).isEmpty();
				assertThat(snapshot()).isEqualTo(snapshot);

			});
		}

	}

	@Nested final class Update {

		@Test void testUpdate() {
			exec(dataset(), () -> {

				final Engine engine=engine();

				assertThat(engine.update(delta, EmployeeShape).map(Frame::focus)).hasValue(resource);
				assertThat(engine.relate(frame(resource), items(EmployeeShape))).hasValue(delta);

			});
		}

		@Test void testReportUnknown() {
			exec(dataset(), () -> {

				final Optional<Frame> snapshot=snapshot();

				assertThat(engine().update(frame(iri(container, "/unknown")), EmployeeShape)).isEmpty();
				assertThat(snapshot()).isEqualTo(snapshot);

			});
		}

	}

	@Nested final class Delete {

		@Test void testDelete() {
			exec(dataset(), () -> {

				assertThat(engine().delete(frame(resource), EmployeeShape).map(Frame::focus)).hasValue(resource);

				assertThat(engine().relate(frame(container), items(EmployeeShape))).isNotEmpty();

				// in order to support vitual containers, related responds with an empty frame for unknown resources

				assertThat(engine().relate(frame(resource), items(EmployeeShape)).map(Frame::model)).hasValue(emptySet());

			});
		}

		@Test void testReportUnknown() {
			exec(dataset(), () -> {

				final Optional<Frame> snapshot=snapshot();

				assertThat(engine().delete(frame(iri(container, "/unknown")), EmployeeShape)).isEmpty();
				assertThat(snapshot()).isEqualTo(snapshot);

			});
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Nested final class RelateItems {

		@Test void testResource() {
			exec(dataset(), () -> assertThat(

					engine().relate(frame(resource), items(EmployeeShape))

			).hasValue(resources.stream()
					.filter(frame -> frame.focus().equals(resource))
					.findFirst()
					.orElse(null)
			));
		}

		@Test void testContainer() {
			exec(dataset(), () -> assertThat(

					engine().relate(frame(container), items(EmployeeShape))

			).hasValue(frame(container).frames(Contains, resources.stream()
					.filter(frame -> frame.value(RDF.TYPE).filter(Employee::equals).isPresent())
			)));
		}


		@Test void testFilter() {
			exec(dataset(), () -> assertThat(

					engine().relate(frame(container), items(and(EmployeeShape,
							filter(field(title, all(literal("Sales Rep"))))
					)))

			).hasValue(frame(container).frames(Contains, resources.stream()
					.filter(frame -> frame.string(title).filter("Sales Rep"::equals).isPresent())
			)));
		}

	}

	@Nested final class RelateTerms {

		@Test void testSlice() {
			exec(dataset(), () -> assertThat(

					engine().relate(frame(container), terms(EmployeeShape, singletonList(title), 1, 3))

			).hasValueSatisfying(frame -> assertThat(frame)

					.isIsomorphicTo(frame(container).frames(Engine.terms, resources.stream()

							.filter(f -> f.value(RDF.TYPE).filter(Employee::equals).isPresent())

							.collect(groupingBy(f -> f.string(title).orElse(""), counting()))
							.entrySet().stream()

							.sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(comparingByKey()))

							.skip(1)
							.limit(3)

							.map(entry -> frame(bnode())
									.string(Engine.value, entry.getKey())
									.integer(Engine.count, entry.getValue())
							)

					))
			));
		}

	}

}