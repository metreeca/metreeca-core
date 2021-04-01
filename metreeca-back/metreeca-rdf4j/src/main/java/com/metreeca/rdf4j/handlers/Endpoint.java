/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rdf4j.handlers;

import com.metreeca.rdf4j.services.Graph;
import com.metreeca.rest.Request;
import com.metreeca.rest.handlers.Delegator;
import com.metreeca.rest.services.Logger;

import java.util.*;

import static com.metreeca.rest.Toolbox.service;

import static java.util.Arrays.asList;
import static java.util.Collections.*;


/**
 * SPARQL 1.1 endpoint handler.
 *
 * <p>Provides a standard SPARQL 1.1 endpoint exposing the contents of a {@linkplain #graph(Graph) target graph}.</p>
 *
 * <p>Both {@linkplain #query(Collection) query} and {@linkplain #update(Collection) update} operations are disabled,
 * unless otherwise specified.</p>
 *
 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-overview-20130321/">SPARQL 1.1 Overview</a>
 */
public abstract class Endpoint<T extends Endpoint<T>> extends Delegator {

	private Graph graph=service(Graph.graph());

	private Set<Object> query=singleton(new Object()); // roles enabled for query operations (unmatchable by default)
	private Set<Object> update=singleton(new Object()); // roles enabled for update operations (unmatchable by default)

	private final Logger logger=service(Logger.logger());


	@SuppressWarnings("unchecked") private T self() {
		return (T)this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected boolean queryable(final Collection<Object> roles) {
		return query.isEmpty() || !disjoint(query, roles);
	}

	protected boolean updatable(final Collection<Object> roles) {
		return update.isEmpty() || !disjoint(update, roles);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected Graph graph() {
		return graph;
	}


	protected Set<Object> query() {
		return unmodifiableSet(query);
	}

	protected Set<Object> update() {
		return unmodifiableSet(update);
	}


	protected Logger logger() {
		return logger;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the target graph.
	 *
	 * <p>By default configured to the shared {@linkplain Graph#graph() graph}.</p>
	 *
	 * @param graph the target graph for SPARQL operations on this endpoint
	 *
	 * @return this endpoint
	 *
	 * @throws NullPointerException if {@code graph} is null
	 */
	public T graph(final Graph graph) {

		if ( graph == null ) {
			throw new NullPointerException("null graph");
		}

		this.graph=graph;

		return self();
	}


	/**
	 * Configures the roles for query operations.
	 *
	 * <p>By default configured to block all query operations.</p>
	 *
	 * @param roles the user {@linkplain Request#roles(Object...) roles} enabled to perform query operations on this
	 *              endpoint; empty for public access
	 *
	 * @return this endpoint
	 *
	 * @throws NullPointerException if {@code roles} is null or contains null values
	 */
	public T query(final Object... roles) {
		return query(asList(roles));
	}

	/**
	 * Configures the roles for query operations.
	 *
	 * <p>By default configured to block all query operations.</p>
	 *
	 * @param roles the user {@linkplain Request#roles(Object...) roles} enabled to perform query operations on this
	 *              endpoint; empty for public access
	 *
	 * @return this endpoint
	 *
	 * @throws NullPointerException if {@code roles} is null or contains null values
	 */
	public T query(final Collection<?> roles) {

		if ( roles == null || roles.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null roles");
		}

		this.query=new HashSet<>(roles);

		return self();
	}


	/**
	 * Configures the roles for update operations.
	 *
	 * <p>By default configured to block all update operations.</p>
	 *
	 * @param roles the user {@linkplain Request#roles(Object...) roles} enabled to perform update operations on this
	 *              endpoint; empty for public access
	 *
	 * @return this endpoint
	 *
	 * @throws NullPointerException if {@code roles} is null or contains null values
	 */
	public T update(final Object... roles) {
		return update(asList(roles));
	}

	/**
	 * Configures the roles for update operations.
	 *
	 * <p>By default configured to block all update operations.</p>
	 *
	 * @param roles the user {@linkplain Request#roles(Object...) roles} enabled to perform update operations on this
	 *              endpoint; empty for public access
	 *
	 * @return this endpoint
	 *
	 * @throws NullPointerException if {@code roles} is null or contains null values
	 */
	public T update(final Collection<?> roles) {

		if ( roles == null || roles.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null roles");
		}

		this.update=new HashSet<>(roles);

		return self();
	}

}
