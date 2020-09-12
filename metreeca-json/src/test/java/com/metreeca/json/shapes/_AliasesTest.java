/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.json.shapes;

import com.metreeca.json.Values;
import com.metreeca.json.ValuesTest;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.stream.Stream;

import static com.metreeca.json.Values.inverse;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Meta.alias;
import static com.metreeca.json.shapes._Aliases.aliases;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class _AliasesTest {

	@Test void testGuessAliasFromIRI() {

		assertThat(aliases(field(RDF.VALUE, and())))
				.as("direct")
				.isEqualTo(singletonMap(RDF.VALUE, "value"));

		assertThat(aliases(field(inverse(RDF.VALUE), and())))
				.as("inverse")
				.isEqualTo(singletonMap(inverse(RDF.VALUE), "valueOf")); // !!! valueOf?

	}

	@Test void testRetrieveUserDefinedAlias() {
		assertThat(aliases(field(RDF.VALUE, alias("alias"))))
				.as("user-defined")
				.isEqualTo(singletonMap(RDF.VALUE, "alias"));
	}

	@Test void testPreferUserDefinedAliases() {
		assertThat(aliases(and(field(RDF.VALUE, alias("alias")), field(RDF.VALUE, and()))))
				.as("user-defined")
				.isEqualTo(singletonMap(RDF.VALUE, "alias"));
	}


	@Test void testRetrieveAliasFromNestedShapes() {

		assertThat(aliases(and(field(RDF.VALUE, alias("alias")))))
				.as("group")
				.isEqualTo(singletonMap(RDF.VALUE, "alias"));

		assertThat(aliases(field(RDF.VALUE, and(alias("alias")))))
				.as("conjunction")
				.isEqualTo(singletonMap(RDF.VALUE, "alias"));

	}

	@Test void testMergeDuplicateFields() {

		// nesting required to prevent and() from collapsing duplicates

		assertThat(aliases(and(field(RDF.VALUE, and()), and(field(RDF.VALUE, and())))))
				.as("system-guessed")
				.isEqualTo(singletonMap(RDF.VALUE, "value"));

		// nesting required to prevent and() from collapsing duplicates

		assertThat(aliases(and(field(RDF.VALUE, alias("alias")), and(field(RDF.VALUE, alias("alias"))))))
				.as("user-defined")
				.isEqualTo(singletonMap(RDF.VALUE, "alias"));

	}


	@Test void testReportConflictingAliases() {
		assertThatThrownBy(() -> aliases(field(RDF.VALUE, and(alias("one"), alias("two")))))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test void testMergeRepeatedAliases() {
		assertThat(aliases(field(RDF.VALUE, and(alias("one"), alias("one")))))
				.isEqualTo(singletonMap(RDF.VALUE, "one"));
	}

	@Test void testHandleLikelyNamedProperties() {
		assertThat(aliases(and(field(RDF.VALUE, and()), field(iri("urn:example#value"), and()))))
				.isEmpty();
	}


	@Test void testMergeAliases() {
		assertThat(aliases(and(field(RDF.TYPE, and()), field(RDF.VALUE, and()))))
				.as("merged")
				.isEqualTo(Stream.of(

						new SimpleImmutableEntry<>(RDF.TYPE, "type"),
						new SimpleImmutableEntry<>(RDF.VALUE, "value")

				).collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
	}

	@Test void testIgnoreReservedAliases() {

		assertThat(aliases(field(Values.iri(ValuesTest.Base, "@id"), and())))
				.as("ignore reserved system-guessed aliases")
				.isEmpty();

		assertThat(aliases(field(RDF.VALUE, alias("@id"))))
				.as("ignore reserved user-defined aliases")
				.isEqualTo(singletonMap(RDF.VALUE, "value"));

	}

}
