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

package com.metreeca.rdf.services;

import com.metreeca.rdf.Values;
import com.metreeca.rdf._probes._Inferencer;
import com.metreeca.rdf._probes._Optimizer;
import com.metreeca.rdf.services.Snippets.Snippet;
import com.metreeca.rest.services.Logger;
import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Redactor;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.function.Supplier;

import static com.metreeca.rdf.Values.direct;
import static com.metreeca.rdf.Values.inverse;
import static com.metreeca.rdf.services.Snippets.list;
import static com.metreeca.rdf.services.Snippets.nothing;
import static com.metreeca.rdf.services.Snippets.snippet;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.services.Logger.logger;

import static java.lang.Math.max;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;


abstract class GraphProcessor {

	Shape expand(final Shape shape) { // !!! caching
		return shape

				.map(new _Inferencer())
				.map(new _Optimizer());
	}


	Shape holder(final Shape shape) { // !!! caching
		return shape

				.map(new Redactor(Shape.Area, Shape.Holder))
				.map(new _Optimizer());
	}

	Shape digest(final Shape shape) { // !!! caching
		return shape

				.map(new Redactor(Shape.Area, Shape.Digest))
				.map(new _Optimizer());
	}

	Shape detail(final Shape shape) { // !!! caching
		return shape

				.map(new Redactor(Shape.Area, Shape.Detail))
				.map(new _Optimizer());
	}


	Shape convey(final Shape shape) { // !!! caching
		return shape

				.map(new Redactor(Shape.Mode, Shape.Convey))
				.map(new _Optimizer());
	}

	Shape filter(final Shape shape) { // !!! caching
		return shape

				.map(new Redactor(Shape.Mode, Shape.Filter))
				.map(new _Optimizer());
	}


	Iterable<Statement> anchor(final Resource resource, final Shape shape) {
		return filter(shape).map(new Outliner(resource)).collect(toList());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Logger logger=service(logger());


	//// Tracing ///////////////////////////////////////////////////////////////////////////////////////////////////////

	String compile(final Supplier<String> generator) {

		final long start=System.currentTimeMillis();

		final String query=generator.get();

		final long stop=System.currentTimeMillis();

		logger.debug(this, () -> format("executing %s", query.endsWith("\n") ? query : query+"\n"));
		logger.debug(this, () -> format("generated in %d ms", max(1, stop-start)));

		return query;
	}

	void evaluate(final Runnable task) {

		final long start=System.currentTimeMillis();

		task.run();

		final long stop=System.currentTimeMillis();

		logger.debug(this, () -> format("evaluated in %d ms", max(1, stop-start)));

	}


	//// SPARQL DSL ////////////////////////////////////////////////////////////////////////////////////////////////////

	static Snippet path(final Collection<IRI> path) {
		return list(path.stream().map(Values::format), '/');
	}


	static Snippet path(final Object source, final Collection<IRI> path, final Object target) {
		return source == null || path == null || path.isEmpty() || target == null ? nothing()
				: snippet(source, " ", path(path), " ", target, " .\n");
	}

	static Snippet edge(final Object source, final IRI iri, final Object target) {
		return source == null || iri == null || target == null ? nothing() : direct(iri)
				? snippet(source, " ", Values.format(iri), " ", target, " .\n")
				: snippet(target, " ", Values.format(inverse(iri)), " ", source, " .\n");
	}


	static Snippet var() {
		return var(new Object());
	}

	static Snippet var(final Object object) {
		return object == null ? nothing() : snippet((Object)"?", Snippets.id(object));
	}

}
