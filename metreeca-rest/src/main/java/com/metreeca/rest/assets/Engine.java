/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.assets;

import com.metreeca.core.*;
import com.metreeca.json.Query;
import com.metreeca.json.Shape;
import com.metreeca.json.shapes.And;

import javax.json.JsonException;
import javax.json.JsonValue;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import static com.metreeca.core.Either.Left;
import static com.metreeca.core.Either.Right;
import static com.metreeca.core.MessageException.status;
import static com.metreeca.core.Response.BadRequest;
import static com.metreeca.core.Response.UnprocessableEntity;


/**
 * Model-driven storage engine.
 *
 * <p>Manages storage transactions, performs storage-specific shape/payload tasks and handles model-driven CRUD actions
 * on resources and containers.</p>
 */
public interface Engine {

	/**
	 * Retrieves the default engine factory.
	 *
	 * @return the default engine factory, which throws an exception reporting the service as undefined
	 */
	public static Supplier<Engine> engine() {
		return () -> { throw new IllegalStateException("undefined engine service"); };
	}

	/**
	 * Retrieves the default shape factory.
	 *
	 * @return the default shape factory, which returns an {@linkplain And#and() empty conjunction}
	 */
	public static Supplier<Shape> shape() {
		return And::and;
	}


	//// !!!
	// /////////////////////////////////////////////////////////////////////////////////////////////////////////

	///**
	// * Retrieves the shape-based query of this request.
	// *
	// * @param request
	// * @param shape  the base shape for the query
	// *
	// * @param format the format supporting {@linkplain Format#path(String, Shape, String) path}/{@linkplain
	// *               Format#value(String, Shape, JsonValue)  value} parsing
	// * @return a value providing access to the combined query merging constraints from {@code shape} and the request
	// * {@linkplain Request#query() query} string, if successfully parsed using {@code format} parsing methods; an
	// error
	// * providing access to the parsing failure, otherwise
	// *
	// * @throws NullPointerException if any argument is null
	// */
	public static Either<MessageException, Query> query(
			final Request request, final Shape shape,
			final Parser<String, List<?>> paths, final Parser<JsonValue, Object> values
	) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		try {

			return Right(new QueryParser(request.base(), shape, paths, values).parse(request.query()));

		} catch ( final JsonException e ) {

			return Left(status(BadRequest, e));

		} catch ( final NoSuchElementException e ) {

			return Left(status(UnprocessableEntity, e));

		}
	}


	@FunctionalInterface public static interface Parser<V, R> {

		public R parse(final String base, final Shape shape, final V v);

	}

	// !!! default path/value parsers (see also RDFFormat)

	///**
	// * Parses a JSON {@linkplain Request#query(Format, Shape) query} field path.
	// *
	// * <p>The default implementation splits paths at dots without further well-formedness checks.</p>
	// *
	// * @param base  the base IRI for resolving relative references
	// * @param shape the base shape for the query
	// * @param path  the query field path to be parsed; may be empty
	// *
	// * @return a list of format-specific path steps parsed from {@code path}
	// *
	// * @throws NullPointerException if any argument is null
	// */
	//public List<?> path(final String base, final Shape shape, final String path) {
	//
	//	if ( base == null ) {
	//		throw new NullPointerException("null base");
	//	}
	//
	//	if ( shape == null ) {
	//		throw new NullPointerException("null shape");
	//	}
	//
	//	if ( path == null ) {
	//		throw new NullPointerException("null path");
	//	}
	//
	//	return asList((Object[])path.split("\\."));
	//}

	///**
	// * Parses a JSON {@linkplain Request#query(Format, Shape) query} field value.
	// *
	// * <p>The default implementation converts JSON values to their native Java representation, using lists of objects
	// * for arrays and maps from strings to objects for objects.</p>
	// *
	// * @param base  the base IRI for resolving relative references
	// * @param shape the base shape for the query
	// * @param value the query value to be parsed
	// *
	// * @return a format-specific value parsed from {@code value}
	// *
	// * @throws NullPointerException if any argument is null
	// */
	//public Object value(final String base, final Shape shape, final JsonValue value) {
	//
	//	if ( base == null ) {
	//		throw new NullPointerException("null base");
	//	}
	//
	//	if ( shape == null ) {
	//		throw new NullPointerException("null shape");
	//	}
	//
	//	if ( value == null ) {
	//		throw new NullPointerException("null value");
	//	}
	//
	//	return value.equals(JsonValue.TRUE) ? true
	//			: value.equals(JsonValue.FALSE) ? false
	//			: value instanceof JsonNumber && ((JsonNumber)value).isIntegral() ? ((JsonNumber)value).longValue()
	//			: value instanceof JsonNumber ? ((JsonNumber)value).doubleValue()
	//			: value instanceof JsonString ? ((JsonString)value).getString()
	//			: value instanceof JsonArray ?
	//			((JsonArray)value).stream().map(v -> value(base, shape, v)).collect(toList())
	//			: value instanceof JsonObject ? ((JsonObject)value).entrySet().stream().collect(toMap(Map.Entry::getKey
	//			, e -> Optional
	//					.ofNullable(fields(shape).get(e.getKey()))
	//					.map(nested -> value(base, nested, e.getValue()))
	//					.orElseThrow(() -> new JsonException("unknown field {"+e.getKey()+"}"))
	//	))
	//			: null;
	//}


	//// Transaction Management ///////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Executes a task within a storage transaction.
	 *
	 * <p>If a transaction is not already active on the underlying storage, begins one and commits it on successful
	 * task completion; if the task throws an exception, the transaction is rolled back and the exception rethrown; in
	 * either case, no action is taken if the transaction was already terminated inside the task.</p>
	 *
	 * <p>Falls back to plain task execution if transactions are not supported by this engine.</p>
	 *
	 * @param task the task to be executed
	 *
	 * @throws NullPointerException if {@code task} is null
	 */
	public default void exec(final Runnable task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		exec(() -> {

			task.run();

			return this;

		});
	}

	/**
	 * Executes a task within a storage transaction.
	 *
	 * <p>If a transaction is not already active on the underlying storage, begins one and commits it on successful
	 * task completion; if the task throws an exception, the transaction is rolled back and the exception rethrown; in
	 * either case, no action is taken if the transaction was already terminated inside the task.</p>
	 *
	 * <p>Falls back to plain task execution if transactions are not supported by this engine.</p>
	 *
	 * @param task the task to be executed
	 * @param <R>  the type of the value returned by the task
	 *
	 * @return the value returned by {@code task}
	 *
	 * @throws NullPointerException if {@code task} is null or returns a null value
	 */
	public <R> R exec(final Supplier<R> task);


	//// Payload Management ///////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Trims message payloads.
	 *
	 * <p>Rewrites the engine-specific message {@linkplain Message#body(Format) payload} retaining only the subset
	 * compatible with the envelope of the message {@linkplain Engine#shape() shape}.</p>
	 *
	 * @param <M>     the type of {@code message}
	 * @param message the message whose engine-specific payload is to be trimmed
	 *
	 * @return a value providing access to the given {@code message} with an updated payload, if its engine-specific
	 * {@linkplain Message#body(Format) payload} is well-formed and compatible with its {@linkplain Engine#shape()
	 * shape}; an error providing access to a failure response builder possibly reporting a validation trace, otherwise
	 *
	 * @throws NullPointerException if {@code message} is null
	 */
	public <M extends Message<M>> Either<MessageException, M> trim(final M message);

	/**
	 * Validates message payloads.
	 *
	 * <p>Validates the engine-specific message {@linkplain Message#body(Format) payload} against the message
	 * {@linkplain Engine#shape() shape}.</p>
	 *
	 * @param <M>     the type of {@code message}
	 * @param message the message whose engine-specific payload is to be validated
	 *
	 * @return a value providing access to {@code message}, if its engine-specific {@linkplain Message#body(Format)
	 * payload} is well-formed and compatible with its {@linkplain Engine#shape() shape}; an error providing access
	 * to a failure response builder possibly reporting a validation trace, otherwise
	 *
	 * @throws NullPointerException if {@code message} is null
	 */
	public <M extends Message<M>> Either<MessageException, M> validate(final M message);


	//// CRUD Actions /////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Handles creation requests.
	 *
	 * <p>Handles creation requests on the linked data resource identified by the request {@linkplain Request#item()
	 * item} possibly using an engine-specific request {@linkplain Message#body(Format) payload} and the message
	 * {@linkplain Engine#shape() shape}.</p>
	 *
	 * @param request a creation request for the managed linked data resource
	 *
	 * @return a lazy response generated for the managed linked data resource in reaction to the creation {@code
	 * request}
	 *
	 * @throws NullPointerException if {@code request} is null
	 */
	public Future<Response> create(final Request request);

	/**
	 * Handles retrieval requests.
	 *
	 * <p>Handles retrieval requests on the linked data resource identified by the request {@linkplain Request#item()
	 * item} possibly using the message {@linkplain Engine#shape() shape}.</p>
	 *
	 * @param request a retrieval request for the managed linked data resource
	 *
	 * @return a lazy response generated for the managed linked data resource in reaction to the retrieval {@code
	 * request}
	 *
	 * @throws NullPointerException if {@code request} is null
	 */
	public Future<Response> relate(final Request request);

	/**
	 * Handles updating requests.
	 *
	 * <p>Handles updating requests on the linked data resource identified by the request {@linkplain Request#item()
	 * item} possibly using an engine-specific request {@linkplain Message#body(Format) payload} and the message
	 * {@linkplain Engine#shape() shape}.</p>
	 *
	 * @param request an updating request for the managed linked data resource
	 *
	 * @return a lazy response generated for the managed linked data resource in reaction to the updating {@code
	 * request}
	 *
	 * @throws NullPointerException if {@code request} is null
	 */
	public Future<Response> update(final Request request);

	/**
	 * Handles deletion requests.
	 *
	 * <p>Handles deletion requests on the linked data resource identified by the request {@linkplain Request#item()
	 * item} possibly using  the message {@linkplain Engine#shape() shape}.</p>
	 *
	 * @param request a deletion request for the managed linked data resource
	 *
	 * @return a lazy response generated for the managed linked data resource in reaction to the deletion {@code
	 * request}
	 *
	 * @throws NullPointerException if {@code request} is null
	 */
	public Future<Response> delete(final Request request);

}
