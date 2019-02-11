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

package com.metreeca.rest;

import com.metreeca.form.*;
import com.metreeca.form.queries.Edges;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;
import com.metreeca.form.things.Sets;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.metreeca.form.queries.Edges.edges;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.rest.Result.Value;


/**
 * Resource engine {thread-safe}.
 *
 * <p>Manages CRUD operations on linked data resources.</p>
 *
 * <p><strong>Warning</strong> / Implementations must be thread-safe.</p>
 */
public interface Engine {

	/**
	 * Retrieves a resource.
	 *
	 * @param resource the IRI identifying the resource whose description is to be retrieved
	 *
	 * @return the description of {@code resource}; empty if a description for {@code resource} was not found
	 *
	 * @throws NullPointerException          if {@code resource} is null
	 * @throws UnsupportedOperationException if resource retrieval is not supported by this engine
	 */
	public default Collection<Statement> relate(final IRI resource) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		return relate(resource, shape -> Value(edges(shape)), (shape, model) -> model).value().orElseGet(Sets::set);
	}

	/**
	 * Retrieves a filtered resource.
	 *
	 * @param resource the IRI identifying the resource whose filtered description is to be retrieved
	 * @param parser   the query parser; the function is applied to the possibly wildcard shape driving retrieval
	 *                 operations for this engine and is expected to combine it with caller-provided constraints,
	 *                 returning either a value describing a query to be applied to the target {@code resource} or an
	 *                 error describing a parsing issue
	 * @param mapper   the description mapper; the function is applied to the possibly wildcard shape driving retrieval
	 *                 operations for this engine and to the model generated by applying the query returned by {@code
	 *                 parser} to the target {@code resource} and is expected to return a final filtered description;
	 *                 the model is  empty if a description for the target {@code resource} was not found
	 * @param <V>      the type of the final filtered description generated by {@code mapper}
	 * @param <E>      the type of the parsing error possibly returned by {@code parser}
	 *
	 * @return the final filtered description of the target {@code resource} as generated by {@code mapper}
	 *
	 * @throws NullPointerException          if any argument is null
	 * @throws UnsupportedOperationException if resource retrieval is not supported by this engine
	 */
	public <V, E> Result<V, E> relate(final IRI resource,
			final Function<Shape, Result<? extends Query, E>> parser,
			final BiFunction<Shape, Collection<Statement>, V> mapper
	);

	/**
	 * Retrieves resource items.
	 *
	 * @param resource the IRI identifying the resource whose contained item descriptions are to be retrieved
	 *
	 * @return the description of the items contained in {@code resource}; empty if {@code resource} doesn't contain any
	 * items; includes {@code ldp:contains} statements linking {@code resource} to the contained items
	 *
	 * @throws NullPointerException          if {@code resource} is null
	 * @throws UnsupportedOperationException if resource browsing is not supported by this engine
	 */
	public default Collection<Statement> browse(final IRI resource) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		return browse(resource, shape -> Value(edges(shape)), (shape, model) -> model).value().orElseGet(Sets::set);
	}

	/**
	 * Retrieves filtered resource items.
	 *
	 * @param resource the IRI identifying the resource whose contained item filtered descriptions are to be retrieved
	 * @param parser   the query parser; the function is applied to the possibly wildcard shape driving browsing
	 *                 operations for this engine and is expected to combine it with caller-provided constraints,
	 *                 returning either a value describing a query to be applied to the contained items of the target
	 *                 {@code resource} or an error describing a parsing issue
	 * @param mapper   the description mapper; the function is applied to the possibly wildcard shape driving browsing
	 *                 operations for this engine and to the model generated by applying the query returned by {@code
	 *                 parser} to the contained items of the target {@code resource} and is expected to return a final
	 *                 filtered description; the model is empty if {@code resource} doesn't contain any items matching
	 *                 the parsed query and includes {@code ldp:contains} statements linking {@code resource} to the
	 *                 matched contained items
	 * @param <V>      the type of the final filtered description generated by {@code mapper}
	 * @param <E>      the type of the parsing error possibly returned by {@code parser}
	 *
	 * @return the final filtered description of the target {@code resource} and the matching contained items as
	 * generated by {@code mapper}
	 *
	 * @throws NullPointerException          if any argument is null
	 * @throws UnsupportedOperationException if resource browsing is not supported by this engine
	 */
	public default <V, E> Result<V, E> browse(final IRI resource,
			final Function<Shape, Result<? extends Query, E>> parser,
			final BiFunction<Shape, Collection<Statement>, V> mapper
	) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( parser == null ) {
			throw new NullPointerException("null parser");
		}

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return parser.apply(and()).fold(

				query -> Value(query.map(new Query.Probe<V>() {

					@Override public V probe(final Edges edges) {
						return mapper.apply(and(), set());
					}

					@Override public V probe(final Stats stats) {
						return mapper.apply(Stats.Shape, set(statement(resource, Form.count, literal(0))));
					}

					@Override public V probe(final Items items) {
						return mapper.apply(Items.Shape, set());
					}

				})),

				Result::Error
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a related resource.
	 *
	 * @param resource the IRI identifying the owning resource for the related resource to be created
	 * @param related  the IRI to be assigned to the new related resource
	 * @param model    the description for the new related resource owned by {@code resource}; must describe the related
	 *                 resource using {@code related} as subject
	 *
	 * @return an optional validation report for the operation; empty a description for {@code resource} is already
	 * present
	 *
	 * @throws NullPointerException          if any argument is null or if {@code model} contains null values
	 * @throws UnsupportedOperationException if resource creation is not supported by this engine
	 */
	public Optional<Focus> create(final IRI resource, final IRI related, final Collection<Statement> model);

	/**
	 * Updates a resource.
	 *
	 * @param resource the IRI identifying the resource whose description is to be updated
	 * @param model    the updated description for {@code resource}
	 *
	 * @return an optional validation report for the operation; empty if a description for {@code resource} was not
	 * found
	 *
	 * @throws NullPointerException          if either {@code resource} or {@code model} is null or if {@code model}
	 *                                       contains null values
	 * @throws UnsupportedOperationException if resource updating is not supported by this engine
	 */
	public Optional<Focus> update(final IRI resource, final Collection<Statement> model);

	/**
	 * Deletes a resource.
	 *
	 * @param resource the IRI identifying the resource whose description is to be deleted
	 *
	 * @return an optional IRI identifying the deleted resource; empty if a description for {@code resource} was not
	 * found
	 *
	 * @throws NullPointerException          if {@code resource} is {@code null}
	 * @throws UnsupportedOperationException if resource deletion is not supported by this engine
	 */
	public Optional<IRI> delete(final IRI resource);

}
