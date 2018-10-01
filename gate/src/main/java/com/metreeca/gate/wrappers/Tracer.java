/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.gate.wrappers;

import com.metreeca.form.Form;
import com.metreeca.form.things.Values;
import com.metreeca.rest.*;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.Update;

import java.util.ArrayList;
import java.util.Collection;

import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.tray.Tray.tool;

import static org.eclipse.rdf4j.query.QueryLanguage.SPARQL;


/**
 * Activity tracer.
 *
 * <p>Creates an audit trail record in the shared {@linkplain Graph#Factory graph} tool on {@linkplain
 * Response#success() successful} request processing by the wrapped handler. Standard records are structured according
 * to the following template and may be extended using a custom SPARQL Update {@linkplain #update(String) script}.</p>
 *
 * <pre>{@code      @prefix : <app://rest.metreeca.com/terms#>
 *
 *     _:node a :Trace;
 *          :item <IRI>;                        # target resource
 *          :task <IRI>;                        # task type
 *          :user <IRI>;                        # actor
 *          :time "timestamp"^^xsd:dateTime;    # ms-precision timestamp
 * }</pre>
 */
public final class Tracer implements Wrapper {

	private IRI task=RDF.NIL;
	private String update="";

	private final Graph graph=tool(Graph.Factory);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the task value for the generated audit trail records.
	 *
	 * @param task the task value to be included the generated audit trail records or {@link RDF#NIL} to let this tracer
	 *             heuristically choose among the standard predefined values ({@link Form#create}, {@link Form#relate},
	 *             {@link Form#update}, {@link Form#delete}), on the basis of  request method and response code
	 *
	 * @return this tracer
	 *
	 * @throws NullPointerException if {@code task} is null
	 */
	public Tracer task(final IRI task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		this.task=task;

		return this;
	}

	/**
	 * Configures the SPARQL Update script for extending the generated audit trail records.
	 *
	 * <p>The script will be executed with the following pre-defined bindings:</p>
	 *
	 * <table summary="pre-defined bindings">
	 *
	 * <thead>
	 *
	 * <tr>
	 * <th>variable</th>
	 * <th>value</th>
	 * </tr>
	 *
	 * </thead>
	 *
	 * <tbody>
	 *
	 * <tr>
	 * <td>this</td>
	 * <td>the blank node identifying the generated audit trail record</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>item</td>
	 * <td>the value of the response focus {@linkplain Response#item() item}</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>task</td>
	 * <td>the type {@linkplain #task(IRI) tag} of the request</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>user</td>
	 * <td>the IRI identifying the {@linkplain Request#user() user} submitting the request</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>time</td>
	 * <td>an {@code xsd:dateTime} literal representing the current system time with millisecond precision</td>
	 * </tr>
	 *
	 * </tbody>
	 *
	 * </table>
	 *
	 * @param update the SPARQL Update script for extending the generated audit trail records
	 *
	 * @return this tracer
	 *
	 * @throws NullPointerException if {@code update} is null
	 */
	public Tracer update(final String update) {

		if ( update == null ) {
			throw new NullPointerException("null update script");
		}

		this.update=update;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return request -> consumer -> graph.update(connection -> {
			handler.handle(request).map(response -> {

				if ( response.success() ) {

					final String method=request.method();

					final IRI trace=iri();

					final IRI user=request.user();
					final IRI item=response.item();

					final Value task=!this.task.equals(RDF.NIL) ? this.task // !!! refactor
							: method.equals(Request.GET) ? Form.relate
							: method.equals(Request.PUT) ? Form.update
							: method.equals(Request.DELETE) ? Form.delete
							: method.equals(Request.POST) ? response.status() == Response.Created ? Form.create : Form.update
							: request.safe() ? Form.relate : Form.update;


					final Literal time=Values.time(true);

					// add default trace record

					final Collection<Statement> model=new ArrayList<>();

					model.add(statement(trace, RDF.TYPE, Rest.Trace));
					model.add(statement(trace, Rest.item, item));
					model.add(statement(trace, Rest.task, task));
					model.add(statement(trace, Rest.user, user));
					model.add(statement(trace, Rest.time, time));

					connection.add(model);

					// add custom info

					if ( !update.isEmpty() ) {

						final Update update=connection.prepareUpdate(SPARQL, this.update, request.base());

						update.setBinding("this", trace);
						update.setBinding("item", item);
						update.setBinding("task", task);
						update.setBinding("user", user);
						update.setBinding("time", time);

						update.execute();
					}

				}

				return response;

			}).accept(consumer);
		});
	}

}
