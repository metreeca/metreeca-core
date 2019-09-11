/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rdf.codecs;

import com.metreeca.rdf.Form;
import com.metreeca.rdf.ValuesTest;
import com.metreeca.tree.Shape;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonValue;

import static com.metreeca.rdf.Values.*;
import static com.metreeca.rdf.ValuesTest.item;
import static com.metreeca.tree.shapes.Datatype.datatype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import static javax.json.Json.createValue;


final class JSONDecoderTest {

	private Map<Value, Collection<Statement>> values(final JsonValue value) {
		return values(value, null);
	}

	private Map<Value, Collection<Statement>> values(final JsonValue value, final Shape shape) {

		return new JSONDecoder(ValuesTest.Base) {}
				.values(value, shape, null)
				.entrySet()
				.stream()
				.map(entry -> entry(entry.getKey(), entry.getValue().collect(toList())))
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

	}


	private Map.Entry<Value, Collection<Statement>> value(final JsonValue value) {
		return value(value, null);
	}

	private Map.Entry<Value, Collection<Statement>> value(final JsonValue value, final Shape shape) {

		final Map.Entry<Value, Stream<Statement>> entry=new JSONDecoder(ValuesTest.Base) {}.value(value, shape, null);

		return entry(entry.getKey(), entry.getValue().collect(toList()));
	}


	private Map.Entry<Value, Collection<Statement>> value(final Value value) {
		return value(value, list());
	}

	private Map.Entry<Value, Collection<Statement>> value(final Value value, final Collection<Statement> model) {
		return entry(value, model);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Nested final class Arrays {

		@Test void testArray() {

			assertThat(values(array("one", "two")).keySet())
					.as("value array")
					.isEqualTo(set(literal("one"), literal("two")));

			assertThat(values(createValue("one")).keySet())
					.as("singleton value")
					.isEqualTo(set(literal("one")));

		}

	}

	@Nested final class Objects {

		@Test void testResource() {

			assertThat(value(object(entry("_this", "_:id"))))
					.isEqualTo(value(bnode("id")));

			assertThat(value(object(entry("_this", "id"))))
					.isEqualTo(value(item("id")));

		}

		@Test void testIRI() {

			final IRI id=iri(ValuesTest.Base, "id");

			final Function<String, JsonObject> object=path -> object(
					entry("_this", "id"),
					entry(String.format(path, RDF.VALUE), RDF.NIL.stringValue())
			);

			assertThat(value(object.apply("<%s>"), field(RDF.VALUE, datatype(Form.IRIType))))
					.as("bracketed direct field")
					.isEqualTo(value(id, list(statement(id, RDF.VALUE, RDF.NIL))));

			assertThat(value(object.apply("^<%s>"), field(inverse(RDF.VALUE), datatype(Form.IRIType))))
					.as("bracketed inverse field")
					.isEqualTo(value(id, list(statement(RDF.NIL, RDF.VALUE, id))));

			assertThat(value(object.apply("%s"), field(RDF.VALUE, datatype(Form.IRIType))))
					.as("naked direct field")
					.isEqualTo(value(id, list(statement(id, RDF.VALUE, RDF.NIL))));

			assertThat(value(object.apply("^%s"), field(inverse(RDF.VALUE), datatype(Form.IRIType))))
					.as("naked inverse field")
					.isEqualTo(value(id, list(statement(RDF.NIL, RDF.VALUE, id))));

			assertThat(value(object.apply("value"), field(RDF.VALUE, datatype(Form.IRIType))))
					.as("aliased field")
					.isEqualTo(value(id, list(statement(id, RDF.VALUE, RDF.NIL))));

		}


		@Test void testTypedLiteral() {
			assertThat(value(object(entry("_this", "2019-04-02"), entry("_type", XMLSchema.DATE.stringValue()))))
					.isEqualTo(value(literal("2019-04-02", XMLSchema.DATE)));
		}

		@Test void testTaggedLiteral() {
			assertThat(value(object(entry("_this", "string"), entry("_type", "@en"))))
					.isEqualTo(value(literal("string", "en")));
		}

	}

	@Nested final class Strings {

		@Test void testString() {
			assertThat(value(createValue("_this"))).isEqualTo(value(literal("_this")));
		}

		@Test void testTypedString() {

			assertThat(value(createValue("_:id"), datatype(Form.ResourceType)))
					.isEqualTo(value(bnode("id")));

			assertThat(value(createValue("id"), datatype(Form.ResourceType)))
					.isEqualTo(value(iri(ValuesTest.Base, "id")));


			assertThat(value(createValue("_:id"), datatype(Form.BNodeType)))
					.isEqualTo(value(bnode("id")));

			assertThat(value(createValue("id"), datatype(Form.BNodeType)))
					.isEqualTo(value(bnode("id")));


			assertThat(value(createValue("id"), datatype(Form.IRIType)))
					.isEqualTo(value(iri(ValuesTest.Base, "id")));


			assertThat(value(createValue("2019-04-02"), datatype(XMLSchema.DATE)))
					.isEqualTo(value(literal("2019-04-02", XMLSchema.DATE)));

		}

	}

	@Nested final class Numbers {

		@Test void testNumber() {

			assertThat(value(createValue(new BigDecimal("1.0"))))
					.as("integral decimal")
					.isEqualTo(value(literal(new BigDecimal("1.0"))));

			assertThat(value(createValue(new BigDecimal("1.1"))))
					.as("fractional decimal")
					.isEqualTo(value(literal(new BigDecimal("1.1"))));

			assertThat(value(createValue(new BigInteger("1"))))
					.as("integer")
					.isEqualTo(value(literal(new BigInteger("1"))));

			assertThat(value(createValue(1.0d)))
					.as("integral float")
					.isEqualTo(value(literal(new BigDecimal("1.0"))));

			assertThat(value(createValue(1.1d)))
					.as("fractional float")
					.isEqualTo(value(literal(new BigDecimal("1.1"))));

		}

		@Test void testTypedNumber() {

			assertThat(value(createValue(new BigDecimal("1.1")), datatype(XMLSchema.DECIMAL)))
					.as("decimal as decimal")
					.isEqualTo(value(literal(new BigDecimal("1.1"))));

			assertThat(value(createValue(new BigDecimal("1.1")), datatype(XMLSchema.INTEGER)))
					.as("decimal as integer")
					.isEqualTo(value(literal(new BigInteger("1"))));

			assertThat(value(createValue(new BigDecimal("1.1")), datatype(XMLSchema.DOUBLE)))
					.as("decimal as double")
					.isEqualTo(value(literal(1.1d)));


			assertThat(value(createValue(new BigInteger("1")), datatype(XMLSchema.DECIMAL)))
					.as("integer as decimal")
					.isEqualTo(value(literal(new BigDecimal("1"))));

			assertThat(value(createValue(new BigInteger("1")), datatype(XMLSchema.INTEGER)))
					.as("integer as integer")
					.isEqualTo(value(literal(new BigInteger("1"))));

			assertThat(value(createValue(new BigInteger("1")), datatype(XMLSchema.DOUBLE)))
					.as("integer as double")
					.isEqualTo(value(literal(1d)));


			assertThat(value(createValue(1.1d), datatype(XMLSchema.DECIMAL)))
					.as("double as decimal")
					.isEqualTo(value(literal(new BigDecimal("1.1"))));

			assertThat(value(createValue(1.1d), datatype(XMLSchema.INTEGER)))
					.as("double as integer")
					.isEqualTo(value(literal(new BigInteger("1"))));

			assertThat(value(createValue(1.1d), datatype(XMLSchema.DOUBLE)))
					.as("double as double")
					.isEqualTo(value(literal(1.1d)));

		}

	}

	@Nested final class Booleans {

		@Test void testBoolean() {
			assertThat(value(JsonValue.TRUE)).isEqualTo(value(True));
			assertThat(value(JsonValue.FALSE)).isEqualTo(value(False));
		}

	}

}
