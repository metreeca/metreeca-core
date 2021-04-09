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

import org.eclipse.rdf4j.model.vocabulary.*;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Any.any;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Lang.lang;
import static com.metreeca.json.shapes.Link.link;
import static com.metreeca.json.shapes.Localized.localized;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.Range.range;
import static com.metreeca.json.shapes.When.when;

import static org.assertj.core.api.Assertions.assertThat;


final class ShapeInferencerTest {

	private Shape expand(final Shape shape) {
		return shape.map(new ShapeInferencer());
	}


	@Test void testDatatype() {

		final Shape datatype=datatype(XSD.BOOLEAN);

		assertThat(expand(datatype))
				.as("xsd:boolean has exclusive values and closed range")
				.isEqualTo(and(datatype, and(maxCount(1), range(literal(false), literal(true)))));
	}

	@Test void testClazz() {

		final Shape clazz=clazz(RDF.NIL);

		assertThat(expand(clazz))
				.as("classed values are resources")
				.isEqualTo(and(clazz, datatype(ResourceType)));
	}

	@Test void testRange() {

		final Shape heterogeneous=range(literal(1, true), literal(2.0, true));

		assertThat(expand(heterogeneous))
				.as("maximum focus size is equal to the size of the allowed value set")
				.isEqualTo(and(heterogeneous, maxCount(2)));

		final Shape uniform=range(literal(1), literal(2));

		assertThat(expand(uniform))
				.as("if unique, focus values share the datatype of the allowed value set")
				.isEqualTo(and(uniform, and(maxCount(2), datatype(XSD.INTEGER))));

	}

	@Test void testLang() {

		final Shape lang=lang("en", "it");

		assertThat(expand(lang))
				.as("tagged literals have fixed datatype")
				.isEqualTo(and(lang, datatype(RDF.LANGSTRING)));
	}

	@Test void testLangLocalized() {

		final Shape localized=and(localized(), lang("en", "it"));

		assertThat(expand(localized))
				.as("maximum focus size is equal to the size of the allowed tag set")
				.isEqualTo(and(localized, datatype(RDF.LANGSTRING), maxCount(2)));
	}


	@Test void testAll() {

		final Shape all=all(literal(1), literal(2));

		assertThat(expand(all))
				.as("minimum focus size is equal to the size of the required value set")
				.isEqualTo(and(all, minCount(2)));
	}

	@Test void testAny() {

		final Shape any=any(literal(1), literal(2));

		assertThat(expand(any))
				.as("minimum focus size is 1")
				.isEqualTo(and(any, minCount(1)));
	}

	@Test void testLocalized() {

		final Shape localized=localized();

		assertThat(expand(localized))
				.as("localized literals have fixed datatype")
				.isEqualTo(and(localized, datatype(RDF.LANGSTRING)));
	}


	@Test void testField() {

		final Shape nested=clazz(RDF.NIL);

		assertThat(expand(field(RDF.VALUE, nested)))
				.as("nested shapes are expanded")
				.isEqualTo(and(
						datatype(ResourceType),
						field(RDF.VALUE, and(nested, datatype(ResourceType)))
				));

		assertThat(expand(field(RDF.TYPE)))
				.as("rdf:type field have resource subjects and IRI objects")
				.isEqualTo(and(
						datatype(ResourceType),
						field(RDF.TYPE, datatype(IRIType))
				));

	}

	@Test void testFieldDirect() {

		final Shape plain=field(RDF.VALUE);

		assertThat(expand(plain))
				.as("field subjects are resources")
				.isEqualTo(and(plain, datatype(ResourceType)));

		final Shape typed=and(field(RDF.VALUE), datatype(IRIType));

		assertThat(expand(typed))
				.as("field subjects are IRIs if explicitly typed")
				.isEqualTo(and(typed, and(plain, datatype(IRIType))));

	}

	@Test void testFieldInverse() {

		final Shape plain=field(inverse(RDF.VALUE));

		assertThat(expand(plain))
				.as("reverse field objects are resources")
				.isEqualTo(field(inverse(RDF.VALUE), datatype(ResourceType)));

		final Shape typed=field(inverse(RDF.VALUE), datatype(IRIType));

		assertThat(expand(typed))
				.as("reverse field objects are IRIs if explicitly typed")
				.isEqualTo(field(inverse(RDF.VALUE), datatype(IRIType)));

	}


	@Test void testLink() {

		final Shape nested=clazz(RDF.NIL);

		assertThat(expand(link(OWL.SAMEAS, nested)))
				.as("nested shapes are expanded")
				.isEqualTo((and(
						datatype(ResourceType),
						link(OWL.SAMEAS, nested, datatype(ResourceType)))
				));

		assertThat(expand(link(OWL.SAMEAS, field(RDF.VALUE))))
				.as("links have resource subjects and objects")
				.isEqualTo(and(datatype(ResourceType), link(OWL.SAMEAS, datatype(ResourceType), field(RDF.VALUE))));

	}


	@Test void testAnd() {

		final Shape and=and(clazz(RDF.FIRST), clazz(RDF.REST));

		assertThat(expand(and))
				.as("nested shapes are expanded")
				.isEqualTo(and(
						and(clazz(RDF.FIRST), datatype(ResourceType)),
						and(clazz(RDF.REST), datatype(ResourceType))
				));
	}

	@Test void testOr() {

		final Shape or=or(clazz(RDF.FIRST), clazz(RDF.REST));

		assertThat(expand(or))
				.as("nested shapes are expanded")
				.isEqualTo(or(
						and(clazz(RDF.FIRST), datatype(ResourceType)),
						and(clazz(RDF.REST), datatype(ResourceType))
				));
	}

	@Test void testWhen() {

		final Shape when=when(clazz(RDF.NIL), clazz(RDF.FIRST), clazz(RDF.REST));

		assertThat(expand(when))
				.as("nested shapes are expanded")
				.isEqualTo(when(
						and(clazz(RDF.NIL), datatype(ResourceType)),
						and(clazz(RDF.FIRST), datatype(ResourceType)),
						and(clazz(RDF.REST), datatype(ResourceType))
				));
	}

}
