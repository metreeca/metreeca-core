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

package com.metreeca.rdf;

import org.eclipse.rdf4j.model.*;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Arrays.asList;


@FunctionalInterface public interface Path {

	public static Path direct(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return (subject, model) -> model.stream()

				.peek(Objects::requireNonNull)

				.filter(s -> s.getSubject().equals(subject))
				.filter(s -> s.getPredicate().equals(predicate))

				.map(Statement::getObject);
	}

	public static Path inverse(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return (subject, model) -> model.stream()

				.peek(Objects::requireNonNull)

				.filter(s -> s.getObject().equals(subject))
				.filter(s -> s.getPredicate().equals(predicate))

				.map(Statement::getSubject);
	}


	public static Path union(final IRI... predicates) {

		if ( predicates == null || Arrays.stream(predicates).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null paths");
		}

		final Collection<IRI> paths=new HashSet<>(asList(predicates));

		return (subject, model) -> model.stream()

				.peek(Objects::requireNonNull)

				.filter(s -> s.getSubject().equals(subject))
				.filter(s -> paths.contains(s.getPredicate()))

				.map(Statement::getObject);
	}

	public static Path union(final Path... paths) {

		if ( paths == null || Arrays.stream(paths).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null paths");
		}

		return union(asList(paths));
	}

	public static Path union(final Collection<Path> paths) {

		if ( paths == null || paths.stream().anyMatch(Objects::isNull)) {
			throw new NullPointerException("null paths");
		}

		return (subject, model) -> paths.stream().flatMap(path -> path.follow(subject, model));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Stream<Value> follow(final Resource subject, final Collection<Statement> model);

}
