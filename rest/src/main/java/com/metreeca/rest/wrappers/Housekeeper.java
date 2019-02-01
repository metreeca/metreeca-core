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

package com.metreeca.rest.wrappers;

import com.metreeca.rest.*;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;

import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.Values.time;
import static com.metreeca.tray.Tray.tool;

import static java.util.Arrays.asList;


/**
 * RDF housekeeper.
 *
 * <p>Executes housekeeping tasks on successful request completion.</p>
 *
 * <p>When processing {@linkplain Request#safe() unsafe} requests, wrapped handlers are executed inside a single
 * transaction on the system {@linkplain Graph#Factory graph database}, which is automatically committed on {@linkplain
 * Response#success() successful} response or rolled back otherwise.</p>
 */
public final class Housekeeper implements Wrapper {

	/**
	 * Creates a SPARQL Update housekeeping task.
	 *
	 * <p>The script is executed with the following pre-defined bindings:</p>
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
	 * <td>the value of the response {@linkplain Response#item() focus item}</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>stem</td>
	 * <td>the {@linkplain IRI#getNamespace() namespace} of the IRI bound to the {@code this} variable</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>name</td>
	 * <td>the local {@linkplain IRI#getLocalName() name} of the IRI bound to the {@code this} variable</td>
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
	 * @param update the SPARQL Update housekeeping script to be executed by this processor on successful request
	 *               processing; empty scripts are ignored
	 *
	 * @return an RDF housekeeping task executing the SPARQL {@code update} script
	 *
	 * @throws NullPointerException if {@code update} is null
	 */
	public static BiConsumer<Response, RepositoryConnection> sparql(final String update) {

		if ( update == null ) {
			throw new NullPointerException("null update");
		}

		return (response, connection) -> {

			if ( !update.isEmpty() ) {

				final IRI item=response.item();
				final IRI stem=iri(item.getNamespace());
				final Literal name=literal(item.getLocalName());

				final IRI user=response.request().user();
				final Literal time=time(true);

				final Update operation=connection.prepareUpdate(QueryLanguage.SPARQL, update, response.request().base());

				operation.setBinding("this", item);
				operation.setBinding("stem", stem);
				operation.setBinding("name", name);

				operation.setBinding("user", user);
				operation.setBinding("time", time);

				operation.execute();
			}

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Collection<BiConsumer<Response, RepositoryConnection>> tasks;

	private final Graph graph=tool(Graph.Factory);


	/**
	 * Creates an RDF housekeeper.
	 *
	 * @param tasks the housekeeping tasks to be executed on {@linkplain Response#success() successful} request
	 *              processing; each task is handled the returned response and a connection to the shared {@linkplain
	 *              Graph#Factory graph} tool
	 *
	 * @throws NullPointerException if {@code tasks} is null or contains null values
	 */
	@SafeVarargs public Housekeeper(final BiConsumer<Response, RepositoryConnection>... tasks) {
		this(asList(tasks));
	}

	/**
	 * Creates an RDF housekeeper.
	 *
	 * @param tasks the housekeeping tasks to be executed on {@linkplain Response#success() successful} request
	 *              processing; each task is handled the returned response and a connection to the shared {@linkplain
	 *              Graph#Factory graph} tool
	 *
	 * @throws NullPointerException if {@code tasks} is null or contains null values
	 */
	public Housekeeper(final Collection<BiConsumer<Response, RepositoryConnection>> tasks) {

		if ( tasks == null ) {
			throw new NullPointerException("null tasks");
		}

		if ( tasks.contains(null) ) {
			throw new NullPointerException("null task");
		}

		this.tasks=new ArrayList<>(tasks);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		return new Connector()
				.wrap(housekeeper())
				.wrap(handler);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper housekeeper() {
		return handler -> request -> handler.handle(request).map(response -> {

			if ( response.success() && !tasks.isEmpty() ) {
				graph.update(connection -> { tasks.forEach(task -> task.accept(response, connection)); });
			}

			return response;

		});

	}

}
