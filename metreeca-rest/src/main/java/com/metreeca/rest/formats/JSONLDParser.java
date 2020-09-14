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

package com.metreeca.rest.formats;

import com.metreeca.json.*;
import com.metreeca.json.queries.Stats;
import com.metreeca.json.queries.Terms;
import com.metreeca.json.shapes.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import javax.json.*;
import java.io.StringReader;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;

import static com.metreeca.json.Order.decreasing;
import static com.metreeca.json.Order.increasing;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.queries.Items.items;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.fields;
import static com.metreeca.json.shapes.Meta.aliases;
import static com.metreeca.rest.Request.search;
import static com.metreeca.rest.Xtream.decode;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

final class JSONLDParser {

	private static final java.util.regex.Pattern StepPattern=java.util.regex.Pattern.compile("(?:^|\\.)(\\w+\\b)");


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Shape shape;

	private final JSONLDDecoder decoder;


	JSONLDParser(final IRI focus, final Shape shape, final Map<String, String> keywords) {
		this.shape=shape;
		this.decoder=new JSONLDDecoder(focus, shape, keywords);
	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	Query parse(final String query) throws JsonException, NoSuchElementException {
		return query.isEmpty() ? items(shape)
				: query.startsWith("%7B") ? json(decode(query))
				: query.startsWith("{") ? json(query)
				: form(query);
	}


	private Query json(final String query) {
		return json(Optional
				.of(query)
				.map(v -> Json.createReader(new StringReader(v)).readValue())
				.filter(JsonObject.class::isInstance)
				.map(JsonValue::asJsonObject)
				.orElseGet(() -> error("filter is not an object"))
		);
	}

	private Query json(final JsonObject json) {

		final Shape filter=filter(json);

		final List<IRI> terms=terms(json);
		final List<IRI> stats=stats(json);

		final List<Order> order=order(json);

		final int offset=offset(json);
		final int limit=limit(json);

		final Shape filtered=and(shape, Guard.filter().then(filter)); // filtering only >> don't include in results

		return terms != null ? Terms.terms(filtered, terms)
				: stats != null ? Stats.stats(filtered, stats)
				: items(filtered, order, offset, limit);
	}


	private Query form(final String query) {
		return json(Json.createObjectBuilder(search(query).entrySet().stream()

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

		final List<String> strings=values.stream()
				.flatMap(value -> Arrays.stream(value.split(",")))
				.collect(toList());

		return strings.size() == 1 ? strings.get(0) : strings;
	}


	//// Query Properties /////////////////////////////////////////////////////////////////////////////////////////////

	private Shape filter(final JsonObject query) {
		return and(query.entrySet().stream()

				.filter(entry -> !entry.getKey().startsWith("_")) // ignore reserved properties
				.filter(entry -> !entry.getValue().equals(JsonValue.NULL)) // ignore null properties

				.map(entry -> { // longest matches first

					final String key=entry.getKey();
					final JsonValue value=entry.getValue();

					return key.startsWith("^") ? filter(key.substring(1), value, shape, this::datatype)
							: key.startsWith("@") ? filter(key.substring(1), value, shape, this::clazz)

							: key.startsWith(">=") ? filter(key.substring(2), value, shape, this::minInclusive)
							: key.startsWith("<=") ? filter(key.substring(2), value, shape, this::maxInclusive)
							: key.startsWith(">") ? filter(key.substring(1), value, shape, this::minExclusive)
							: key.startsWith("<") ? filter(key.substring(1), value, shape, this::maxExclusive)

							: key.startsWith("$>") ? filter(key.substring(2), value, shape, this::minLength)
							: key.startsWith("$<") ? filter(key.substring(2), value, shape, this::maxLength)
							: key.startsWith("*") ? filter(key.substring(1), value, shape, this::pattern)
							: key.startsWith("~") ? filter(key.substring(1), value, shape, this::like)

							: key.startsWith("#>") ? filter(key.substring(2), value, shape, this::minCount)
							: key.startsWith("#<") ? filter(key.substring(2), value, shape, this::maxCount)

							: key.startsWith("%") ? filter(key.substring(1), value, shape, this::in)
							: key.startsWith("!") ? filter(key.substring(1), value, shape, this::all)
							: key.startsWith("?") ? filter(key.substring(1), value, shape, this::any)

							: filter(key, value, shape, this::any);

				})

				.collect(toList())
		);
	}


	private Shape filter(final String path,
			final JsonValue value, final Shape shape, final BiFunction<JsonValue, Shape, Shape> mapper) {
		return filter(steps(path, shape), value, shape, mapper);
	}

	private Shape filter(final List<IRI> path,
			final JsonValue value, final Shape shape, final BiFunction<JsonValue, Shape, Shape> mapper) {

		return path.isEmpty() ? mapper.apply(value, shape)
				: Field.field(head(path), filter(tail(path), value, field(head(path), shape), mapper));
	}


	private List<IRI> terms(final JsonObject query) {
		return Optional.ofNullable(query.get("_terms"))

				.filter(v -> !v.equals(JsonValue.NULL))

				.map(v -> v instanceof JsonString ? (JsonString)v : error("_terms is not a string"))
				.map(path -> path(path.getString(), shape))

				.orElse(null);
	}

	private List<IRI> stats(final JsonObject query) {
		return Optional.ofNullable(query.get("_stats"))

				.filter(v -> !v.equals(JsonValue.NULL))

				.map(v -> v instanceof JsonString ? (JsonString)v : error("_stats is not a string"))
				.map(path -> path(path.getString(), shape))

				.orElse(null);
	}


	private List<IRI> path(final String path, final Shape shape) {
		return path(steps(path, shape), shape);
	}

	private List<IRI> path(final List<IRI> path, final Shape shape) {

		if ( !path.isEmpty() ) {

			path(tail(path), field(head(path), shape)); // validate tail

		}

		return path; // return whole path
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


	//// Paths ////////////////////////////////////////////////////////////////////////////////////////////////////////

	private List<IRI> steps(final String path, final Shape shape) {
		try {

			final String path1=path.trim();

			final List<IRI> steps=new ArrayList<>();
			final Matcher matcher=StepPattern.matcher(path1);

			int last=0;
			Shape reference=shape;

			while ( matcher.lookingAt() ) {

				final Map<IRI, Shape> fields=fields(reference);
				final Map<IRI, String> aliases=aliases(reference);

				final String step=matcher.group(1);

				final IRI iri=aliases.entrySet().stream()

						.filter(entry -> entry.getValue().equals(step))
						.map(Map.Entry::getKey)
						.findFirst()

						.orElseThrow(() -> new NoSuchElementException("unknown path step <"+step+">"));


				steps.add(iri);
				reference=fields.get(iri);

				matcher.region(last=matcher.end(), path1.length());
			}

			if ( last != path1.length() ) {
				throw new JsonException("malformed path ["+path1+"]");
			}

			return steps;

		} catch ( final JsonException|NoSuchElementException e ) {

			throw e;

		} catch ( final RuntimeException e ) {

			throw new JsonException(e.getMessage(), e);

		}
	}


	private IRI head(final List<IRI> path) {
		return path.get(0);
	}

	private List<IRI> tail(final List<IRI> path) {
		return path.subList(1, path.size());
	}


	private Shape field(final IRI name, final Shape shape) {

		final Shape nested=fields(shape).get(name);

		if ( nested == null ) {
			throw new NoSuchElementException("unknown path step <"+name+">");
		}

		return nested;
	}


	//// Values ///////////////////////////////////////////////////////////////////////////////////////////////////////

	private List<Value> values(final JsonValue value, final Shape shape) {
		return decoder.values(value, shape).map(Map.Entry::getKey).collect(toList());
	}

	private Value value(final JsonValue value, final Shape shape) {
		return decoder.value(value, shape).getKey();
	}


	//// Shapes ///////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shape datatype(final JsonValue value, final Shape shape) {
		return value instanceof JsonString
				? Datatype.datatype(iri(((JsonString)value).getString()))
				: error("datatype value is not a string");
	}

	private Shape clazz(final JsonValue value, final Shape shape) {
		return value instanceof JsonString
				? Clazz.clazz(iri(((JsonString)value).getString()))
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


	private Shape minLength(final JsonValue value, final Shape shape) {
		return value instanceof JsonNumber
				? MinLength.minLength(((JsonNumber)value).intValue())
				: error("length is not a number");
	}

	private Shape maxLength(final JsonValue value, final Shape shape) {
		return value instanceof JsonNumber
				? MaxLength.maxLength(((JsonNumber)value).intValue())
				: error("length is not a number");
	}

	private Shape pattern(final JsonValue value, final Shape shape) {
		return value instanceof JsonString
				? ((JsonString)value).getString().isEmpty() ? and() : Pattern.pattern(((JsonString)value).getString())
				: error("pattern is not a string");
	}

	private Shape like(final JsonValue value, final Shape shape) {
		return value instanceof JsonString
				? ((JsonString)value).getString().isEmpty() ? and() : Like.like(((JsonString)value).getString(), true)
				: error("pattern is not a string");
	}


	private Shape minCount(final JsonValue value, final Shape shape) {
		return value instanceof JsonNumber
				? MinCount.minCount(((JsonNumber)value).intValue())
				: error("length is not a number");
	}

	private Shape maxCount(final JsonValue value, final Shape shape) {
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
