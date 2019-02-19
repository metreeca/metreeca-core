/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.engines;

import com.metreeca.form.things.Snippets;
import com.metreeca.form.things.Snippets.Snippet;
import com.metreeca.form.things.Values;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.metreeca.form.things.Values.direct;
import static com.metreeca.form.things.Values.inverse;
import static com.metreeca.form.things.Values.pattern;
import static com.metreeca.tray.Tray.tool;

import static org.eclipse.rdf4j.common.iteration.Iterations.stream;

import static java.lang.Math.max;
import static java.lang.String.format;
import static java.util.Collections.singleton;


abstract class GraphProcessor {

	private final Trace trace=tool(Trace.Factory);


	@FunctionalInterface  static interface Source {

		public Stream<Statement> match(final Resource subject, final IRI predicate, final Value object);

	}


	//// Tracing ///////////////////////////////////////////////////////////////////////////////////////////////////////

	String compile(final Supplier<String> generator) {

		final long start=System.currentTimeMillis();

		final String query=generator.get();

		final long stop=System.currentTimeMillis();

		trace.debug(this, () -> format("executing %s", query.endsWith("\n") ? query : query+"\n"));
		trace.debug(this, () -> format("generated in %d ms", max(1, stop-start)));

		return query;
	}

	void evaluate(final Runnable task) {

		final long start=System.currentTimeMillis();

		task.run();

		final long stop=System.currentTimeMillis();

		trace.debug(this, () -> format("evaluated in %d ms", max(1, stop-start)));

	}


	//// Concise Bounded Description ///////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves a symmetric concise bounded description from a statement source.
	 *
	 * @param focus    the resource whose symmetric concise bounded description is to be retrieved
	 * @param labelled if {@code true}, the retrieved description will be extended with {@code rdf:type} and {@code
	 *                 rdfs:label/comment} annotations for all referenced IRIs
	 * @param model    the statement source the description is to be retrieved from
	 *
	 * @return the symmetric concise bounded description of {@code focus} retrieved from {@code model}
	 *
	 * @throws NullPointerException if either {@code focus} or {@code model} is null
	 */
	Collection<Statement> description(final Value focus, final boolean labelled, final Collection<Statement> model) {
		return description(focus, labelled, (s, p, o) -> model.stream().filter(pattern(s, p, o)));
	}

	/**
	 * Retrieves the symmetric concise bounded description of a resource from a repository.
	 *
	 * @param focus      the resource whose symmetric concise bounded description is to be retrieved
	 * @param labelled   if {@code true}, the retrieved description will be extended with {@code rdf:type} and {@code
	 *                   rdfs:label/comment} annotations for all referenced IRIs
	 * @param connection the connection to the repository the description is to be retrieved from
	 *
	 * @return the symmetric concise bounded description of {@code focus} retrieved from {@code connection}
	 *
	 * @throws NullPointerException if either {@code focus} or {@code connection} is null
	 */
	Collection<Statement> description(final Value focus, final boolean labelled, final RepositoryConnection connection) {

		// !!! optimize for SPARQL

		return description(focus, labelled, (s, p, o) -> stream(connection.getStatements(s, p, o, true)));
	}


	private Collection<Statement> description(final Value focus, final boolean labelled, final Source source) {

		final Model description=new LinkedHashModel();

		final Queue<Value> pending=new ArrayDeque<>(singleton(focus));
		final Collection<Value> visited=new HashSet<>();

		while ( !pending.isEmpty() ) {

			final Value value=pending.remove();

			if ( visited.add(value) ) {
				if ( value.equals(focus) || value instanceof BNode ) {

					source.match((Resource)value, null, null)
							.peek(statement -> pending.add(statement.getObject()))
							.forEach(description::add);

					source.match(null, null, value)
							.peek(statement -> pending.add(statement.getSubject()))
							.forEach(description::add);

				} else if ( labelled && value instanceof IRI ) {

					source.match((Resource)value, RDF.TYPE, null).forEach(description::add);
					source.match((Resource)value, RDFS.LABEL, null).forEach(description::add);
					source.match((Resource)value, RDFS.COMMENT, null).forEach(description::add);

				}
			}

		}

		return description;

	}


	//// SPARQL DSL ////////////////////////////////////////////////////////////////////////////////////////////////////

	static Snippet path(final Collection<IRI> path) {
		return list(path.stream().map(Values::format), '/');
	}


	static Snippet path(final Object source, final Collection<IRI> path, final Object target) {
		return source == null || path == null || path.isEmpty() || target == null ? Snippets.nothing()
				: Snippets.snippet(source, " ", path(path), " ", target, " .\n");
	}

	static Snippet edge(final Object source, final IRI iri, final Object target) {
		return source == null || iri == null || target == null ? Snippets.nothing() : direct(iri)
				? Snippets.snippet(source, " ", Values.format(iri), " ", target, " .\n")
				: Snippets.snippet(target, " ", Values.format(inverse(iri)), " ", source, " .\n");
	}


	static Snippet list(final Stream<?> items, final Object separator) {
		return items == null ? Snippets.nothing() : Snippets.snippet(items.flatMap(item -> Stream.of(separator, item)).skip(1));
	}


	static Snippet var() {
		return var(new Object());
	}

	static Snippet var(final Object object) {
		return object == null ? Snippets.nothing() : Snippets.snippet((Object)"?", Snippets.id(object));
	}

}
