/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.rdf;

import org.eclipse.rdf4j.model.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;


@FunctionalInterface public interface Path {

	public static Path direct(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return (subject, model) -> model.stream()

				.peek(statement -> { if ( statement == null ) { throw new NullPointerException("null statement"); } })

				.filter(s -> s.getSubject().equals(subject))
				.filter(s -> s.getPredicate().equals(predicate))

				.map(Statement::getObject);
	}

	public static Path inverse(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return (subject, model) -> model.stream()

				.peek(statement -> { if ( statement == null ) { throw new NullPointerException("null statement"); } })

				.filter(s -> s.getObject().equals(subject))
				.filter(s -> s.getPredicate().equals(predicate))

				.map(Statement::getSubject);
	}


	public static Path union(final IRI... predicates) {

		if ( predicates == null || Arrays.stream(predicates).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null paths");
		}

		return union(Arrays.stream(predicates).map(Path::direct).collect(Collectors.toList()));
	}

	public static Path union(final Path... paths) {

		if ( paths == null || Arrays.stream(paths).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null paths");
		}

		return union(asList(paths));
	}

	public static Path union(final Collection<Path> paths) {

		if ( paths == null ) {
			throw new NullPointerException("null paths");
		}

		return (subject, model) -> paths.stream()

				.peek(path -> { if ( path == null ) { throw new NullPointerException("null path"); } })

				.flatMap(path -> path.follow(subject, model));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Stream<Value> follow(final Resource subject, final Collection<Statement> model);

}
