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

package com.metreeca.rest.services;

import com.metreeca.json.*;
import com.metreeca.json.queries.Stats;
import com.metreeca.json.queries.Terms;
import com.metreeca.json.shapes.Field;
import com.metreeca.rest.Wrapper;
import com.metreeca.rest.operators.Creator;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.metreeca.json.Values.term;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.*;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;


/**
 * Model-driven storage engine.
 *
 * <p>Handles model-driven CRUD operations on resource managed by a specific storage backend.</p>
 */
public interface Engine {

	public static IRI terms=term("terms");
	public static IRI stats=term("stats");

	public static IRI value=term("value");
	public static IRI count=term("count");

	public static IRI min=term("min");
	public static IRI max=term("max");


	/**
	 * Resource annotation properties.
	 *
	 * <p>RDF properties for human-readable resource annotations (e.g. {@code rdf:label}, {@code rdfs:comment}, …).</p>
	 */
	public static Set<IRI> Annotations=unmodifiableSet(new HashSet<>(asList(RDFS.LABEL, RDFS.COMMENT)));


	/**
	 * Generates the response shape for a stats query.
	 *
	 * @param query the reference stats query
	 *
	 * @return a stats response shape incorporating resource {@linkplain #Annotations annotations} extracted from {@code
	 * query}
	 *
	 * @throws NullPointerException if {@code query} is null
	 */
	public static Shape StatsShape(final Stats query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		final Shape resource=ValueShape(query);

		return and(

				field(count, required(), datatype(XSD.INTEGER)),
				field(min, optional(), resource),
				field(max, optional(), resource),

				field(stats, multiple(),
						field(count, required(), datatype(XSD.INTEGER)),
						field(min, required(), resource),
						field(max, required(), resource)
				)

		);
	}

	/**
	 * Generates the response shape for a terms query.
	 *
	 * @param query the reference terms query
	 *
	 * @return a terms response shape incorporating resource {@linkplain #Annotations annotations} extracted from {@code
	 * query}
	 *
	 * @throws NullPointerException if {@code query} is null
	 */
	public static Shape TermsShape(final Terms query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		final Shape resource=ValueShape(query);

		return and(
				field(terms, multiple(),
						field(value, required(), resource),
						field(count, required(), datatype(XSD.INTEGER))
				)
		);
	}


	/**
	 * Generates the value shape for a query.
	 *
	 * @param query the reference query
	 *
	 * @return a value shape incorporating resource {@linkplain #Annotations annotations} extracted from {@code query}
	 *
	 * @throws NullPointerException if {@code query} is null
	 */
	public static Shape ValueShape(final Query query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		return and(query.shape()
				.redact(Mode, Convey)
				.walk(query.path())
				.map(Field::fields)
				.orElseGet(Stream::empty)
				.filter(field -> Annotations.contains(field.iri())));
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the default engine factory.
	 *
	 * @return the default engine factory, which throws an exception reporting the service as undefined
	 */
	public static Supplier<Engine> engine() {
		return () -> { throw new IllegalStateException("undefined engine service"); };
	}


	//// Wrappers //////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a transaction wrapper.
	 *
	 * @return a wrapper ensuring that requests are handled within a single transaction on the storage backend
	 */
	public Wrapper transaction();


	//// CRUD Operations ///////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Handles creation requests.
	 *
	 * @param frame a frame describing the linked data resource to be created
	 * @param shape a shape describing the {@code frame} {@link Frame#model() model}
	 *
	 * @return an optional containing {@code frame}, possibly extended with server managed properties, if the frame
	 * {@linkplain Frame#focus() focus} IRI was not already present in the storage backend; an empty optional, otherwise
	 *
	 * @throws NullPointerException if either {@code frame} or {@code shape} is null
	 * @implSpec Concrete implementations must assume that {@code frame} was already configured with a unique focus IRI
	 * for the resource to be created and its model rewritten accordingly, for instance by an outer {@link Creator}
	 * handler
	 */
	public Optional<Frame> create(final Frame frame, final Shape shape);

	/**
	 * Handles retrieval requests.
	 *
	 * @param frame a frame focused on the linked data resource to be retrieved
	 * @param query a query describing the expected response model
	 *
	 * @return an optional containing a frame describing the {@code frame} focus, possibly filtered according to {@code
	 * query}, if the storage backend contained a matching linked data resource; an empty optional, otherwise
	 *
	 * @throws NullPointerException if either {@code item} or {@code query} is null
	 */
	public Optional<Frame> relate(final Frame frame, final Query query);

	/**
	 * Handles updating requests.
	 *
	 * @param frame a frame describing the linked data resource to be updated
	 * @param shape a shape describing the {@code frame} {@link Frame#model() model}
	 *
	 * @return an optional containing {@code frame}, possibly extended with server managed properties, if the frame
	 * {@linkplain Frame#focus() focus} IRI was already present in the storage backend; an empty optional, otherwise
	 *
	 * @throws NullPointerException if either {@code frame} or {@code shape} is null
	 */
	public Optional<Frame> update(final Frame frame, final Shape shape);

	/**
	 * Handles deletion requests.
	 *
	 * @param frame a frame focused on the linked data resource to be deleted
	 * @param shape a shape describing the {@code frame} model to be deleted
	 *
	 * @return an optional containing a frame describing the deleted {@code frame}, if it was present in the storage
	 * backend; an empty optional, otherwise
	 *
	 * @throws NullPointerException if either {@code frame} or {@code shape} is null
	 * @implSpec If the deleted model is not readily available, concrete implementations may return a frame with an
	 * empty
	 * model
	 */
	public Optional<Frame> delete(final Frame frame, Shape shape);

}
