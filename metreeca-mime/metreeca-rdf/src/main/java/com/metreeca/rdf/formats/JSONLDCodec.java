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

package com.metreeca.rdf.formats;

import com.metreeca.json.Shape;
import com.metreeca.json.probes.Redactor;
import com.metreeca.json.probes.Traverser;
import com.metreeca.json.shapes.*;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.stream.Stream;


abstract class JSONLDCodec {

	protected Shape driver(final Shape shape) { // !!! caching
		return shape

				.map(new Redactor(Shape.Role, values -> true))
				.map(new Redactor(Shape.Task, values -> true))
				.map(new Redactor(Shape.Area, values -> true))
				.map(new Redactor(Shape.Mode, Shape.Convey)) // remove internal filtering shapes

				.map(new _RDFInferencer()) // infer implicit constraints to drive json shorthands
				.map(new _RDFOptimizer());

	}

	protected Stream<Map.Entry<String, String>> keywords(final Shape shape) {
		return shape.map(new KeywordsProbe());
	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class KeywordsProbe extends Traverser<Stream<Map.Entry<String, String>>> {

		@Override public Stream<Map.Entry<String, String>> probe(final Shape shape) {
			return Stream.empty();
		}

		@Override public Stream<Map.Entry<String, String>> probe(final Meta meta) {

			final String label=meta.label().toString();

			return label.startsWith("@")
					? Stream.of(new SimpleImmutableEntry<>(label, meta.value().toString()))
					: Stream.empty();
		}

		@Override public Stream<Map.Entry<String, String>> probe(final Field field) {
			return Stream.empty();
		}

		@Override public Stream<Map.Entry<String, String>> probe(final And and) {
			return and.shapes().stream().flatMap(s -> s.map(this));
		}

		@Override public Stream<Map.Entry<String, String>> probe(final Or or) {
			return or.shapes().stream().flatMap(s -> s.map(this));
		}

		@Override public Stream<Map.Entry<String, String>> probe(final When when) {
			return Stream.of(when.pass(), when.fail()).flatMap(s -> s.map(this));
		}

	}

}
