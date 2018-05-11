/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.next.wrappers;

import com.metreeca.next.*;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

import static com.metreeca.next.Binder.binder;
import static com.metreeca.next.wrappers.Transactor.transactor;
import static com.metreeca.spec.things.Values.iri;
import static com.metreeca.spec.things.Values.statement;
import static com.metreeca.spec.things.Values.timestamp;
import static com.metreeca.tray.Tray.tool;

import static org.eclipse.rdf4j.query.QueryLanguage.SPARQL;


/**
 * Activity tracer.
 */
public final class Tracer implements Wrapper {

	public static Tracer tracer() {
		return new Tracer();
	}


	private Value task=RDF.NIL;
	private String sparql="";

	private Predicate<Response.Reader> test=reader -> true;


	private Tracer() {}


	public Tracer task(final Value task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		this.task=task;

		return this;
	}

	public Tracer sparql(final String sparql) {

		if ( sparql == null ) {
			throw new NullPointerException("null sparql");
		}

		this.sparql=sparql;

		return this;
	}


	public Tracer test(final Predicate<Response.Reader> test) {

		if ( test == null ) {
			throw new NullPointerException("null test");
		}

		this.test=test;

		return this;
	}


	@Override public Handler wrap(final Handler handler) {
		return (request, response) -> {
			try (final RepositoryConnection connection=tool(Graph.Tool).connect()) {
				transactor(connection, true).wrap((_request, _response) -> handler.exec(

						writer ->

								writer.copy(_request).done(),

						reader -> {

							if ( reader.success() && test.test(reader) ) {

								final String method=_request.method();

								final IRI trace=iri();

								final IRI user=_request.user();
								final IRI item=reader.focus();
								final Value task=!this.task.equals(RDF.NIL) ? this.task // !!! refactor
										: method.equals(Request.GET) ? Link.relate
										: method.equals(Request.PUT) ? Link.update
										: method.equals(Request.DELETE) ? Link.delete
										: method.equals(Request.POST) ? reader.status() == Response.Created ? Link.create : Link.update
										: _request.safe() ? Link.relate : Link.update;


								final Literal time=timestamp();

								// add default trace record

								final Collection<Statement> model=new ArrayList<>();

								model.add(statement(trace, RDF.TYPE, Link.Trace));
								model.add(statement(trace, Link.Item, item));
								model.add(statement(trace, Link.Task, task));
								model.add(statement(trace, Link.User, user));
								model.add(statement(trace, Link.Time, time));

								connection.add(model);

								// add custom info

								if ( !sparql.isEmpty() ) {
									binder()

											.set("this", trace)
											.set("item", item)
											.set("task", task)
											.set("user", user)
											.set("time", time)

											.bind(connection.prepareUpdate(SPARQL, sparql, _request.base()))

											.execute();
								}

							}

							_response.copy(reader).done();

						}

				)).handle(request, response);
			}
		};
	}

}
