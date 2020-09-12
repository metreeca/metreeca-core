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

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Map;

import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Field.fields;
import static com.metreeca.json.shapes.Guard.relate;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.When.when;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;


final class FieldTest {

	@Nested final class Optimization {

		@Test void testPruneDeadFields() {
			assertThat(field(RDF.VALUE, or())).isEqualTo(and());
		}

	}


	@Nested final class Inspection {

		@SafeVarargs private final <K, V> Map<K, V> map(final Map.Entry<K, V>... entries) {
			return Arrays.stream(entries).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
		}

		private <K, V> Map.Entry<K, V> entry(final K key, final V value) {
			return new SimpleImmutableEntry<>(key, value);
		}


		@Test void testInspectFields() {
			assertThat(fields(

					field(RDF.VALUE, and())

			)).isEqualTo(singletonMap(

					RDF.VALUE, and()

			));
		}

		@Test void testInspectConjunctions() {
			assertThat(fields(and(

					field(RDF.VALUE, and()),
					field(RDF.TYPE, clazz(RDF.FIRST)),
					field(RDF.TYPE, clazz(RDF.REST))

			))).isEqualTo(map(

					entry(RDF.VALUE, and()),
					entry(RDF.TYPE, and(clazz(RDF.FIRST), clazz(RDF.REST)))

			));
		}

		@Test void testInspectDisjunctions() {
			assertThat(fields(or(

					field(RDF.VALUE, and()),
					field(RDF.TYPE, clazz(RDF.FIRST)),
					field(RDF.TYPE, clazz(RDF.REST))

			))).isEqualTo(map(

					entry(RDF.VALUE, and()),
					entry(RDF.TYPE, or(clazz(RDF.FIRST), clazz(RDF.REST)))

			));
		}

		@Test void testInspectOptions() {
			assertThat(fields(when(relate(),

					field(RDF.VALUE, and()),
					field(RDF.TYPE, maxCount(1))

			))).isEqualTo(map(

					entry(RDF.VALUE, and()),
					entry(RDF.TYPE, maxCount(1))

			));
		}


		@Test void testInspectOtherShapes() {
			assertThat(fields(and())).isEmpty();
		}

	}

}
