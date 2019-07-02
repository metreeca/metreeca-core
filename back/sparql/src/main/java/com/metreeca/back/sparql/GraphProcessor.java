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

package com.metreeca.back.sparql;

import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Redactor;
import com.metreeca.form.things.Snippets;
import com.metreeca.form.things.Snippets.Snippet;
import com.metreeca.form.things.Values;
import com.metreeca.tray.Trace;

import org.eclipse.rdf4j.model.*;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.metreeca.form.shapes.Memoizing.memoizable;
import static com.metreeca.form.things.Snippets.list;
import static com.metreeca.form.things.Snippets.nothing;
import static com.metreeca.form.things.Snippets.snippet;
import static com.metreeca.form.things.Values.direct;
import static com.metreeca.form.things.Values.inverse;

import static java.lang.Math.max;
import static java.lang.String.format;


abstract class GraphProcessor {

	static final Function<Shape, Shape> convey=memoizable(s -> s
			.map(new Redactor(Form.mode, Form.convey))
			.map(new Optimizer())
	);

	static final Function<Shape, Shape> filter=memoizable(s -> s
			.map(new Redactor(Form.mode, Form.filter))
			.map(new Optimizer())
	);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Trace trace;


	GraphProcessor(final Trace trace) {

		this.trace=trace;

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
