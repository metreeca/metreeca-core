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

import java.util.Collection;

import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.Shape.*;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.Values.literal;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.filter;
import static com.metreeca.json.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.json.shapes.MinInclusive.minInclusive;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

public final class EngineData {

	public static final IRI base=iri("http://example.com/");
	public static final IRI term=item("/terms/");


	public static IRI item(final String name) {
		return iri(base, name);
	}

	public static IRI term(final String name) {
		return iri(term, name);
	}


	public static final IRI Alias=term("Alias");
	public static final IRI Employee=term("Employee");

	public static final IRI code=term("code");
	public static final IRI forename=term("forename");
	public static final IRI surname=term("surname");
	public static final IRI email=term("email");
	public static final IRI title=term("title");
	public static final IRI seniority=term("seniority");
	public static final IRI office=term("office");
	public static final IRI supervisor=term("supervisor");
	public static final IRI subordinate=term("subordinate");


	public static final IRI aliases=item("/aliases/");
	public static final IRI employees=item("/employees/");
	public static final IRI container=item("/container/");


	public static final Shape EmployeeShape=and(

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

	public static final Collection<Frame> resources=unmodifiableList(asList(

			frame(iri(aliases, "1002"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1002")),

			frame(iri(employees, "1002"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("Diane Murphy"),
							literal("Diane Murphy", "en"),
							literal("Diane Murphy", "it")
					)
					.string(code, "1002")
					.string(forename, "Diane")
					.string(surname, "Murphy")
					.string(email, "dmurphy@classicmodelcars.com")
					.string(title, "President")
					.integer(seniority, 5)
					.value(office, item("/offices/1"))
					.values(subordinate,
							iri(employees, "1056"),
							iri(employees, "1076")
					),

			frame(iri(aliases, "1056"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1056")),

			frame(iri(employees, "1056"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("Mary Patterson"),
							literal("Mary Patterson", "en"),
							literal("Mary Patterson", "it")
					)
					.string(code, "1056")
					.string(forename, "Mary")
					.string(surname, "Patterson")
					.string(email, "mpatterso@classicmodelcars.com")
					.string(title, "VP Sales")
					.integer(seniority, 4)
					.value(office, item("/offices/1"))
					.value(supervisor, iri(employees, "1002"))
					.values(subordinate,
							iri(employees, "1088"),
							iri(employees, "1102"),
							iri(employees, "1143"),
							iri(employees, "1621")
					),

			frame(iri(aliases, "1076"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1076")),

			frame(iri(employees, "1076"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("Jeff Firrelli"),
							literal("Jeff Firrelli", "en"),
							literal("Jeff Firrelli", "it")
					)
					.string(code, "1076")
					.string(forename, "Jeff")
					.string(surname, "Firrelli")
					.string(email, "jfirrelli@classicmodelcars.com")
					.string(title, "VP Marketing")
					.integer(seniority, 4)
					.value(office, item("/offices/1"))
					.value(supervisor, iri(employees, "1002")),

			frame(iri(aliases, "1088"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1088")),

			frame(iri(employees, "1088"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("William Patterson"),
							literal("William Patterson", "en"),
							literal("William Patterson", "it")
					)
					.string(code, "1088")
					.string(forename, "William")
					.string(surname, "Patterson")
					.string(email, "wpatterson@classicmodelcars.com")
					.string(title, "Sales Manager (APAC)")
					.integer(seniority, 3)
					.value(office, item("/offices/6"))
					.value(supervisor, iri(employees, "1056"))
					.values(subordinate,
							iri(employees, "1611"),
							iri(employees, "1612"),
							iri(employees, "1619")
					),

			frame(iri(aliases, "1102"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1102")),

			frame(iri(employees, "1102"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("Gerard Bondur"),
							literal("Gerard Bondur", "en"),
							literal("Gerard Bondur", "it")
					)
					.string(code, "1102")
					.string(forename, "Gerard")
					.string(surname, "Bondur")
					.string(email, "gbondur@classicmodelcars.com")
					.string(title, "Sale Manager (EMEA)")
					.integer(seniority, 4)
					.value(office, item("/offices/4"))
					.value(supervisor, iri(employees, "1056"))
					.values(subordinate,
							iri(employees, "1337"),
							iri(employees, "1370"),
							iri(employees, "1401"),
							iri(employees, "1501"),
							iri(employees, "1504"),
							iri(employees, "1702")
					),

			frame(iri(aliases, "1143"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1143")),

			frame(iri(employees, "1143"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("Anthony Bow"),
							literal("Anthony Bow", "en"),
							literal("Anthony Bow", "it")
					)
					.string(code, "1143")
					.string(forename, "Anthony")
					.string(surname, "Bow")
					.string(email, "abow@classicmodelcars.com")
					.string(title, "Sales Manager (NA)")
					.integer(seniority, 3)
					.value(office, item("/offices/1"))
					.value(supervisor, iri(employees, "1056"))
					.values(subordinate,
							iri(employees, "1165"),
							iri(employees, "1166"),
							iri(employees, "1188"),
							iri(employees, "1216"),
							iri(employees, "1286"),
							iri(employees, "1323")
					),

			frame(iri(aliases, "1165"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1165")),

			frame(iri(employees, "1165"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("Leslie Jennings"),
							literal("Leslie Jennings", "en"),
							literal("Leslie Jennings", "it")
					)
					.string(code, "1165")
					.string(forename, "Leslie")
					.string(surname, "Jennings")
					.string(email, "ljennings@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 1)
					.value(office, item("/offices/1"))
					.value(supervisor, iri(employees, "1143")),

			frame(iri(aliases, "1166"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1166")),

			frame(iri(employees, "1166"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("Leslie Thompson"),
							literal("Leslie Thompson", "en"),
							literal("Leslie Thompson", "it")
					)
					.string(code, "1166")
					.string(forename, "Leslie")
					.string(surname, "Thompson")
					.string(email, "lthompson@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 1)
					.value(office, item("/offices/1"))
					.value(supervisor, iri(employees, "1143")),

			frame(iri(aliases, "1188"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1188")),

			frame(iri(employees, "1188"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("Julie Firrelli"),
							literal("Julie Firrelli", "en"),
							literal("Julie Firrelli", "it")
					)
					.string(code, "1188")
					.string(forename, "Julie")
					.string(surname, "Firrelli")
					.string(email, "jfirrelli@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 1)
					.value(office, item("/offices/2"))
					.value(supervisor, iri(employees, "1143")),

			frame(iri(aliases, "1216"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1216")),

			frame(iri(employees, "1216"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("Steve Patterson"),
							literal("Steve Patterson", "en"),
							literal("Steve Patterson", "it")
					)
					.string(code, "1216")
					.string(forename, "Steve")
					.string(surname, "Patterson")
					.string(email, "spatterson@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 2)
					.value(office, item("/offices/2"))
					.value(supervisor, iri(employees, "1143")),

			frame(iri(aliases, "1286"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1286")),

			frame(iri(employees, "1286"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("Foon Yue Tseng"),
							literal("Foon Yue Tseng", "en"),
							literal("Foon Yue Tseng", "it")
					)
					.string(code, "1286")
					.string(forename, "Foon Yue")
					.string(surname, "Tseng")
					.string(email, "ftseng@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 1)
					.value(office, item("/offices/3"))
					.value(supervisor, iri(employees, "1143")),

			frame(iri(aliases, "1323"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1323")),

			frame(iri(employees, "1323"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("George Vanauf"),
							literal("George Vanauf", "en"),
							literal("George Vanauf", "it")
					)
					.string(code, "1323")
					.string(forename, "George")
					.string(surname, "Vanauf")
					.string(email, "gvanauf@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 1)
					.value(office, item("/offices/3"))
					.value(supervisor, iri(employees, "1143")),

			frame(iri(aliases, "1337"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1337")),

			frame(iri(employees, "1337"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("Loui Bondur"),
							literal("Loui Bondur", "en"),
							literal("Loui Bondur", "it")
					)
					.string(code, "1337")
					.string(forename, "Loui")
					.string(surname, "Bondur")
					.string(email, "lbondur@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 1)
					.value(office, item("/offices/4"))
					.value(supervisor, iri(employees, "1102")),

			frame(iri(aliases, "1370"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1370")),

			frame(iri(employees, "1370"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("Gerard Hernandez"),
							literal("Gerard Hernandez", "en"),
							literal("Gerard Hernandez", "it")
					)
					.string(code, "1370")
					.string(forename, "Gerard")
					.string(surname, "Hernandez")
					.string(email, "ghernande@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 2)
					.value(office, item("/offices/4"))
					.value(supervisor, iri(employees, "1102")),

			frame(iri(aliases, "1401"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1401")),

			frame(iri(employees, "1401"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("Pamela Castillo"),
							literal("Pamela Castillo", "en"),
							literal("Pamela Castillo", "it")
					)
					.string(code, "1401")
					.string(forename, "Pamela")
					.string(surname, "Castillo")
					.string(email, "pcastillo@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 2)
					.value(office, item("/offices/4"))
					.value(supervisor, iri(employees, "1102")),

			frame(iri(aliases, "1501"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1501")),

			frame(iri(employees, "1501"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("Larry Bott"),
							literal("Larry Bott", "en"),
							literal("Larry Bott", "it")
					)
					.string(code, "1501")
					.string(forename, "Larry")
					.string(surname, "Bott")
					.string(email, "lbott@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 2)
					.value(office, item("/offices/7"))
					.value(supervisor, iri(employees, "1102")),

			frame(iri(aliases, "1504"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1504")),

			frame(iri(employees, "1504"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("Barry Jones"),
							literal("Barry Jones", "en"),
							literal("Barry Jones", "it")
					)
					.string(code, "1504")
					.string(forename, "Barry")
					.string(surname, "Jones")
					.string(email, "bjones@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 1)
					.value(office, item("/offices/7"))
					.value(supervisor, iri(employees, "1102")),

			frame(iri(aliases, "1611"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1611")),

			frame(iri(employees, "1611"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("Andy Fixter"),
							literal("Andy Fixter", "en"),
							literal("Andy Fixter", "it")
					)
					.string(code, "1611")
					.string(forename, "Andy")
					.string(surname, "Fixter")
					.string(email, "afixter@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 1)
					.value(office, item("/offices/6"))
					.value(supervisor, iri(employees, "1088")),

			frame(iri(aliases, "1612"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1612")),

			frame(iri(employees, "1612"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("Peter Marsh"),
							literal("Peter Marsh", "en"),
							literal("Peter Marsh", "it")
					)
					.string(code, "1612")
					.string(forename, "Peter")
					.string(surname, "Marsh")
					.string(email, "pmarsh@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 1)
					.value(office, item("/offices/6"))
					.value(supervisor, iri(employees, "1088")),

			frame(iri(aliases, "1619"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1619")),

			frame(iri(employees, "1619"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("Tom King"),
							literal("Tom King", "en"),
							literal("Tom King", "it")
					)
					.string(code, "1619")
					.string(forename, "Tom")
					.string(surname, "King")
					.string(email, "tking@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 2)
					.value(office, item("/offices/6"))
					.value(supervisor, iri(employees, "1088")),

			frame(iri(aliases, "1621"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1621")),

			frame(iri(employees, "1621"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("Mami Nishi"),
							literal("Mami Nishi", "en"),
							literal("Mami Nishi", "it")
					)
					.string(code, "1621")
					.string(forename, "Mami")
					.string(surname, "Nishi")
					.string(email, "mnishi@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 2)
					.value(office, item("/offices/5"))
					.value(supervisor, iri(employees, "1056"))
					.values(subordinate,
							iri(employees, "1625")
					),

			frame(iri(aliases, "1625"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1625")),

			frame(iri(employees, "1625"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("Yoshimi Kato"),
							literal("Yoshimi Kato", "en"),
							literal("Yoshimi Kato", "it")
					)
					.string(code, "1625")
					.string(forename, "Yoshimi")
					.string(surname, "Kato")
					.string(email, "ykato@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 2)
					.value(office, item("/offices/5"))
					.value(supervisor, iri(employees, "1621")),

			frame(iri(aliases, "1702"))
					.value(RDF.TYPE, Alias)
					.value(OWL.SAMEAS, iri(employees, "1702")),

			frame(iri(employees, "1702"))
					.value(RDF.TYPE, Employee)
					.values(RDFS.LABEL,
							literal("Martin Gerard"),
							literal("Martin Gerard", "en"),
							literal("Martin Gerard", "it")
					)
					.string(code, "1702")
					.string(forename, "Martin")
					.string(surname, "Gerard")
					.string(email, "mgerard@classicmodelcars.com")
					.string(title, "Sales Rep")
					.integer(seniority, 2)
					.value(office, item("/offices/4"))
					.value(supervisor, iri(employees, "1102")),


			frame(container).values(Contains,
					iri(employees, "1002"),
					iri(employees, "1056"),
					iri(employees, "1076"),
					iri(employees, "1088"),
					iri(employees, "1102"),
					iri(employees, "1143"),
					iri(employees, "1165"),
					iri(employees, "1166"),
					iri(employees, "1188"),
					iri(employees, "1216"),
					iri(employees, "1286"),
					iri(employees, "1323"),
					iri(employees, "1337"),
					iri(employees, "1370"),
					iri(employees, "1401"),
					iri(employees, "1501"),
					iri(employees, "1504"),
					iri(employees, "1611"),
					iri(employees, "1612"),
					iri(employees, "1619"),
					iri(employees, "1621"),
					iri(employees, "1625"),
					iri(employees, "1702")
			)

	));

}
