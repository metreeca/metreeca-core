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

import com.metreeca.rdf4j.assets.Graph;
import com.metreeca.rest.Context;
import com.metreeca.rest.Request;
import com.metreeca.rest.assets.Logger;
import com.metreeca.rest.handlers.Delegator;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.disjoint;
import static java.util.Collections.singleton;


/**
 * SPARQL 1.1 endpoint handler.
 *
 * <p>Provides a standard SPARQL 1.1 endpoint exposing the contents of the shared {@linkplain Graph graph}.</p>
 *
 * <p>Both {@linkplain #query(Collection) query} and {@linkplain #update(Collection) update} operations are disabled,
 * unless otherwise specified.</p>
 *
 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-overview-20130321/">SPARQL 1.1 Overview</a>
 */
public abstract class Endpoint<T extends Endpoint<T>> extends Delegator {

	private int timeout=60; // endpoint operations timeout [s]

	private Set<Object> query=singleton(new Object()); // roles enabled for query operations (unmatchable by default)
	private Set<Object> update=singleton(new Object()); // roles enabled for update operations (unmatchable by default)

	private final Graph graph=Context.asset(Graph.graph());
	private final Logger logger=Context.asset(Logger.logger());


	@SuppressWarnings("unchecked") private T self() {
		return (T)this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected int timeout() {
		return timeout;
	}


	protected boolean queryable(final Collection<Object> roles) {
		return query.isEmpty() || !disjoint(query, roles);
	}

	protected boolean updatable(final Collection<Object> roles) {
		return update.isEmpty() || !disjoint(update, roles);
	}


	protected Graph graph() {
		return graph;
	}

	protected Logger logger() {
		return logger;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures timeout for endpoint requests.
	 *
	 * @param timeout the timeout for endpoint requests in seconds; 0 to disable timeouts
	 *
	 * @return this endpoint
	 *
	 * @throws IllegalArgumentException if {@code timeout} is less than 0
	 */
	public T timeout(final int timeout) {

		if ( timeout < 0 ) {
			throw new IllegalArgumentException("illegal timeout ["+timeout+"]");
		}

		this.timeout=timeout;

		return self();
	}


	/**
	 * Configures the roles for query operations.
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
