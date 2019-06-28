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

package com.metreeca.rest;

import com.metreeca.form.Focus;
import com.metreeca.form.Query;
import com.metreeca.form.Shape;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;


/**
 * Storage engine.
 *
 * <p>Manages storage transactions and performs CRUD actions on LDP <a href="https://www.w3.org/TR/ldp/#ldpr">resources</a>
 * and <a href="https://www.w3.org/TR/ldp/#ldpc">containers</a>.</p>
 *
 * <p>When operating on containers, engines select the operating {@linkplain com.metreeca.form.things.Shapes profile}
 * according to the {@code rdf:type} property of the target, as inferred either from {@linkplain
 * com.metreeca.form.shapes.Meta metadata} annotations or {@linkplain com.metreeca.form.shapes.Field field} constraints
 * in the provided shape, defaulting to the <em>Basic</em> profile if no metadata is available:</p>
 *
 * @see <a href="https://www.w3.org/TR/ldp/">Linked Data Platform 1.0</a>
 */
public interface Engine {

	/**
	 * Retrieves the default engine factory.
	 *
	 * @return the default engine factory, which throws an exception reporting the engine as undefined
	 */
	public static Supplier<Engine> engine() {
		return () -> { throw new IllegalStateException("undefined engine tool"); };
	}


	//// Transactions //////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Executes a task within a read-only storage transaction.
	 *
	 * <p>Falls back to plain task execution if transactions are not supported by this engine.</p>
	 *
	 * @param task the task to be executed
	 * @param <R>  the type of the value returned by the task
	 *
	 * @return the value returned by {@code task}
	 *
	 * @throws NullPointerException if {@code task} is null
	 */
	public <R> R reading(final Supplier<R> task);

	/**
	 * Executes a task within a read-write storage transaction.
	 *
	 * <p>Falls back to plain task execution if transactions are not supported by this engine.</p>
	 *
	 * @param task the task to be executed
	 * @param <R>  the type of the value returned by the task
	 *
	 * @return the value returned by {@code task}
	 *
	 * @throws NullPointerException if {@code task} is null
	 */
	public <R> R writing(final Supplier<R> task);


	//// CRUD Operations ///////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Relates a resource.
	 *
	 * @param resource the IRI identifying the resource whose description is to be retrieved
	 * @param query    the query defining the details of {@code resource} to be retrieved;
	 *
	 * @return the description of {@code resource} matched by {@code query}; empty if a matching description for {@code
	 * resource} was not found; related resources matched by {@code query} are linked to {@code resource} with the
	 * {@code ldp:contains} property
	 *
	 * @throws NullPointerException          if either {@code resource} or {@code query} is {@code null}
	 * @throws UnsupportedOperationException if resource retrieval is not supported by this engine
	 */
	public Collection<Statement> relate(final IRI resource, final Query query);

	/**
	 * Creates a resource.
	 *
	 * @param resource the IRI identifying the resource whose description is to be created
	 * @param shape    the validation shape for the description of the resource;
	 * @param model    the description for {@code resource} to be created
	 *
	 * @return an optional validation report for the operation; empty if a description for {@code resource} is already
	 * present
	 *
	 * @throws NullPointerException          if any argument is null or if {@code model} contains null values
	 * @throws UnsupportedOperationException if resource creation is not supported by this engine
	 */
	public Optional<Focus> create(final IRI resource, final Shape shape, Collection<Statement> model);

	/**
	 * Updates a resource.
	 *
	 * @param resource the IRI identifying the resource whose description is to be updated
	 * @param shape    the validation shape for the description of the resource
	 * @param model    the updated description for {@code resource}
	 *
	 * @return an optional validation report for the operation; empty if a description for {@code resource} was not
	 * found
	 *
	 * @throws NullPointerException          if any argument is null or if {@code model} contains null values
	 * @throws UnsupportedOperationException if resource updating is not supported by this engine
	 */
	public Optional<Focus> update(final IRI resource, final Shape shape, Collection<Statement> model);

	/**
	 * Deletes a resource.
	 *
	 * @param resource the IRI identifying the resource whose description is to be deleted
	 * @param shape    the validation shape for the description of the resource
	 *
	 * @return an optional IRI identifying the deleted resource; empty if a description for {@code resource} was not
	 * found
	 *
	 * @throws NullPointerException          if either {@code resource} or {@code shape} is {@code null}
	 * @throws UnsupportedOperationException if resource deletion is not supported by this engine
	 */
	public Optional<Focus> delete(final IRI resource, final Shape shape);

}
