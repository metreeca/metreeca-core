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

import com.metreeca.tree.Order;
import com.metreeca.tree.Query;
import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Optimizer;
import com.metreeca.tree.queries.Stats;
import com.metreeca.tree.queries.Terms;
import com.metreeca.tree.shapes.*;

import java.io.StringReader;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import javax.json.*;

import static com.metreeca.rest.Codecs.decode;
import static com.metreeca.tree.Order.decreasing;
import static com.metreeca.tree.Order.increasing;
import static com.metreeca.tree.queries.Items.items;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.Field.fields;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


final class QueryParser {

	private static final java.util.regex.Pattern StepPattern=
			java.util.regex.Pattern.compile("(?:^|[./])\\s*(?:([:\\w]+)|'([^']*)')\\s*");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Shape shape;
	private final BiFunction<JsonValue, Shape, Object> parser;


	QueryParser(final Shape shape, final BiFunction<JsonValue, Shape, Object> parser) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( parser == null ) {
			throw new NullPointerException("null parser");
		}

		this.shape=shape;
		this.parser=parser;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Parses a JSON object encoding a query.
	 *
	 * @param query either a URL-encoded JSON object or URL query parameters representing a shape-driven linked data
	 *              query
	 *
	 * @return the parsed query
	 *
	 * @throws NullPointerException   if {@code query} is null
	 * @throws JsonException          if {@code query} is malformed
	 * @throws NoSuchElementException if the query encoded by {@code query} referes to data outside the parser shape
	 */
	public Query parse(final String query) throws JsonException, NoSuchElementException {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		return query.isEmpty() ? items(shape)
				: query.startsWith("%7B") ? json(decode(query))
				: query.startsWith("{") ? json(query)
				: form(query);

	}


	private Query json(final String query) {
		return json(Optional
				.of(query)
				.map(v -> Json.createReader(new StringReader(v)).readValue())
				.filter(v -> v instanceof JsonObject)
				.map(JsonValue::asJsonObject)
				.orElseGet(() -> error("filter is not an object"))
		);
	}

	private Query json(final JsonObject json) {

		final Shape filter=filter(json);

		final List<String> terms=terms(json);
		final List<String> stats=stats(json);

		final List<Order> order=order(json);

		final int offset=offset(json);
		final int limit=limit(json);

		final Shape filtered=and(shape, Shape.filter().then(filter)) // mark as filtering only >> don't include in results
				.map(new Optimizer());

		return terms != null ? Terms.terms(filtered, terms)
				: stats != null ? Stats.stats(filtered, stats)
				: items(filtered, order, offset, limit);
	}


	private Query form(final String query) {
		return json(Json.createObjectBuilder(Codecs.parameters(query).entrySet().stream()

				.collect(toMap(Map.Entry::getKey, this::value))

		).build());
	}


	private Object value(final Map.Entry<String, List<String>> field) {

		final String key=field.getKey();
		final List<String> values=field.getValue();

		return key.equals("_terms") || key.equals("_stats") ? path(values)
				: key.equals("_offset") || key.equals("_limit") ? integer(values)
				: strings(values);

	}


	private Object path(final List<String> values) {
		return values.size() == 1 ? values.get(0) : strings(values);
	}

	private Object integer(final List<String> values) {
		if ( values.size() == 1 ) {

			try {

				return Long.parseLong(values.get(0));

			} catch ( final NumberFormatException e ) {

				return strings(values);

			}

		} else {

			return strings(values);

		}
	}

	private Object strings(final Collection<String> values) {
		return values.stream()
				.flatMap(value -> Arrays.stream(value.split(",")))
				.collect(toList());
	}


	//// Query Properties //////////////////////////////////////////////////////////////////////////////////////////////

	private Shape filter(final JsonObject query) {
		return and(query.entrySet().stream()
				.filter(entry -> !entry.getKey().startsWith("_")) // ignore reserved properties
				.filter(entry -> !entry.getValue().equals(JsonValue.NULL)) // ignore null properties
				.map(entry -> { // longest matches first

					final String key=entry.getKey();
					final JsonValue value=entry.getValue();

					return key.startsWith("^") ? filter(path(key.substring(1), shape), datatype(value))
							: key.startsWith("@") ? filter(path(key.substring(1), shape), clazz(value))

							: key.startsWith(">=") ? filter(path(key.substring(2), shape), minInclusive(value, shape))
							: key.startsWith("<=") ? filter(path(key.substring(2), shape), maxInclusive(value, shape))
							: key.startsWith(">") ? filter(path(key.substring(1), shape), minExclusive(value, shape))
							: key.startsWith("<") ? filter(path(key.substring(1), shape), maxExclusive(value, shape))

							: key.startsWith("$>") ? filter(path(key.substring(2), shape), minLength(value))
							: key.startsWith("$<") ? filter(path(key.substring(2), shape), maxLength(value))
							: key.startsWith("*") ? filter(path(key.substring(1), shape), pattern(value))
							: key.startsWith("~") ? filter(path(key.substring(1), shape), like(value))

							: key.startsWith("#>") ? filter(path(key.substring(2), shape), minCount(value))
							: key.startsWith("#<") ? filter(path(key.substring(2), shape), maxCount(value))

							: key.startsWith("{}") ? filter(path(key.substring(2), shape), in(value, shape))
							: key.startsWith("!") ? filter(path(key.substring(1), shape), all(value, shape))
							: key.startsWith("?") ? filter(path(key.substring(1), shape), any(value, shape))

							: filter(path(key, shape), any(value, shape));

				})
				.collect(toList())
		);
	}

	private Shape filter(final List<String> path, final Shape shape) {
		return path.isEmpty() ? shape : field(path.get(0), filter(path.subList(1, path.size()), shape));
	}


	private List<String> terms(final JsonObject query) {
		return Optional.ofNullable(query.get("_terms"))

				.filter(v -> !v.equals(JsonValue.NULL))

				.map(v -> v instanceof JsonString ? (JsonString)v : error("_terms is not a string"))
				.map(path -> path(path.getString(), shape))

				.orElse(null);
	}

	private List<String> stats(final JsonObject query) {
		return Optional.ofNullable(query.get("_stats"))

				.filter(v -> !v.equals(JsonValue.NULL))

				.map(v -> v instanceof JsonString ? (JsonString)v : error("_stats is not a string"))
				.map((path) -> path(path.getString(), shape))

				.orElse(null);
	}


	private List<Order> order(final JsonObject query) {
		return Optional.ofNullable(query.get("_order"))

				.filter(v -> !v.equals(JsonValue.NULL))

				.map(this::order)

				.orElse(emptyList());
	}

	private List<Order> order(final JsonValue object) {
		return object instanceof JsonString ? criteria((JsonString)object)
				: object instanceof JsonArray ? criteria((JsonArray)object)
				: error("_order is neither a string nor an array of strings");
	}


	private List<Order> criteria(final JsonArray object) {
		return object.stream()
				.map(v -> v instanceof JsonString ? (JsonString)v : error("_order criterion is not a string"))
				.map(this::criterion)
				.collect(toList());
	}

	private List<Order> criteria(final JsonString object) {
		return singletonList(criterion(object.getString()));
	}

	private Order criterion(final JsonString criterion) {
		return criterion(criterion.getString());
	}

	private Order criterion(final String criterion) {
		return criterion.startsWith("+") ? increasing(path(criterion.substring(1), shape))
				: criterion.startsWith("-") ? decreasing(path(criterion.substring(1), shape))
				: increasing(path(criterion, shape));
	}


	private int offset(final JsonObject query) {
		return Optional.ofNullable(query.get("_offset"))

				.filter(v -> !v.equals(JsonValue.NULL))

				.map(v -> v instanceof JsonNumber ? (JsonNumber)v : error("_offset not a number"))
				.map(JsonNumber::intValue)
				.map(v -> v >= 0 ? v : error("negative offset"))

				.orElse(0);
	}

	private int limit(final JsonObject query) {
		return Optional.ofNullable(query.get("_limit"))

				.filter(v -> !v.equals(JsonValue.NULL))

				.map(v -> v instanceof JsonNumber ? (JsonNumber)v : error("_limit is not a number"))
				.map(JsonNumber::intValue)
				.map(v -> v >= 0 ? v : error("negative limit"))

				.orElse(0);
	}


	//// Paths /////////////////////////////////////////////////////////////////////////////////////////////////////////

	private List<String> path(final CharSequence path, final Shape shape) {

		final Collection<String> steps=new ArrayList<>();

		final Matcher matcher=StepPattern.matcher(path);

		final int length=path.length();

		int last=0;

		while ( matcher.lookingAt() ) {

			final String naked=matcher.group(1);
			final String quoted=matcher.group(2);

			steps.add(naked != null ? naked : quoted);
			matcher.region(last=matcher.end(), length);

		}

		if ( last != length ) {
			throw new JsonException("malformed path ["+path+"]");
		}

		return path(steps, shape);
	}

	private List<String> path(final Iterable<String> steps, final Shape shape) {

		final List<String> path=new ArrayList<>();

		Shape reference=shape;

		for (final String step : steps) {

			final Map<String, Shape> fields=fields(reference);

			final Map<String, String> index=new HashMap<>();

			fields.keySet().forEach(label -> index.put(label, label));
			// !!! aliases(reference).forEach((alias, label) -> index.put(alias, label)

			final String label=index.get(step);

			if ( label == null ) {
				throw new NoSuchElementException("unknown path step ["+step+"]");
			}

			path.add(label);
			reference=fields.get(label);

		}

		return path;

	}


	//// Values ////////////////////////////////////////////////////////////////////////////////////////////////////////

	private List<Object> values(final JsonValue value, final Shape shape) {
		return (value instanceof JsonArray ? ((JsonArray)value).stream() : Stream.of(value))
				.map(value1 -> value(value1, shape))
				.collect(toList());
	}

	private Object value(final JsonValue value, final Shape shape) {
		return parser.apply(value, shape);
	}


	//// Shapes ////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shape datatype(final JsonValue value) {
		return value instanceof JsonString
				? Datatype.datatype(((JsonString)value).getString())
				: error("datatype value is not a string");
	}

	private Shape clazz(final JsonValue value) {
		return value instanceof JsonString
				? Clazz.clazz(((JsonString)value).getString())
				: error("class value is not a string");
	}


	private Shape minExclusive(final JsonValue value, final Shape shape) {
		return value != null
				? MinExclusive.minExclusive(value(value, shape))
				: error("value is null");
	}

	private Shape maxExclusive(final JsonValue value, final Shape shape) {
		return value != null
				? MaxExclusive.maxExclusive(value(value, shape))
				: error("value is null");
	}

	private Shape minInclusive(final JsonValue value, final Shape shape) {
		return value != null
				? MinInclusive.minInclusive(value(value, shape))
				: error("value is null");
	}

	private Shape maxInclusive(final JsonValue value, final Shape shape) {
		return value != null
				? MaxInclusive.maxInclusive(value(value, shape))
				: error("value is null");
	}


	private Shape minLength(final JsonValue value) {
		return value instanceof JsonNumber
				? MinLength.minLength(((JsonNumber)value).intValue())
				: error("length is not a number");
	}

	private Shape maxLength(final JsonValue value) {
		return value instanceof JsonNumber
				? MaxLength.maxLength(((JsonNumber)value).intValue())
				: error("length is not a number");
	}

	private Shape pattern(final JsonValue value) {
		return value instanceof JsonString
				? ((JsonString)value).getString().isEmpty() ? and() : Pattern.pattern(((JsonString)value).getString())
				: error("pattern is not a string");
	}

	private Shape like(final JsonValue value) {
		return value instanceof JsonString
				? ((JsonString)value).getString().isEmpty() ? and() : Like.like(((JsonString)value).getString())
				: error("pattern is not a string");
	}


	private Shape minCount(final JsonValue value) {
		return value instanceof JsonNumber
				? MinCount.minCount(((JsonNumber)value).intValue())
				: error("length is not a number");
	}

	private Shape maxCount(final JsonValue value) {
		return value instanceof JsonNumber
				? MaxCount.maxCount(((JsonNumber)value).intValue())
				: error("length is not a number");
	}

	private Shape in(final JsonValue value, final Shape shape) {
		return value.getValueType() == JsonValue.ValueType.NULL
				? error("value is null")
				: In.in(values(value, shape)
		);
	}

	private Shape all(final JsonValue value, final Shape shape) {
		return value.getValueType() == JsonValue.ValueType.NULL
				? error("value is null")
				: All.all(values(value, shape)
		);
	}

	private Shape any(final JsonValue value, final Shape shape) {
		return value.getValueType() == JsonValue.ValueType.NULL
				? error("value is null")
				: Any.any(values(value, shape));
	}


	private <V> V error(final String message) {
		throw new JsonException(message);
	}

}
