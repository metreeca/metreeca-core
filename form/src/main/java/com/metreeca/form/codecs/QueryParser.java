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

package com.metreeca.form.codecs;

import com.metreeca.form.Order;
import com.metreeca.form.Query;
import com.metreeca.form.Shape;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.queries.Edges;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;
import com.metreeca.form.shapes.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;

import javax.json.*;

import static com.metreeca.form.Order.decreasing;
import static com.metreeca.form.Order.increasing;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Field.fields;
import static com.metreeca.form.things.Values.format;

import static java.util.Collections.disjoint;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;


public final class QueryParser extends JSONDecoder {

	private static final java.util.regex.Pattern StepPatten
			=java.util.regex.Pattern.compile("(?:^|[./])(\\^?(?:\\w+:.*|\\w+|<[^>]*>))");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Shape shape;


	public QueryParser(final Shape shape, final String base) {

		super(base);

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		this.shape=shape;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Parses a JSON object encoding a query.
	 *
	 * @param json the JSON object encoding a shape-driven linked data query
	 *
	 * @return the parsed query
	 *
	 * @throws NullPointerException   if {@code json} is null
	 * @throws JsonException          if {@code json} is malformed
	 * @throws NoSuchElementException if the query encoded by {@code json} referes to data outside the {@linkplain
	 *                                #QueryParser(Shape, String) parser shape}
	 */
	public Query parse(final String json) throws JsonException {

		if ( json == null ) {
			throw new NullPointerException("null json");
		}

		final JsonObject query=Optional.of(json)
				.filter(v -> !v.isEmpty())
				.map(v -> Json.createReader(new StringReader(v)).readValue())
				.map(v -> v instanceof JsonObject ? (JsonObject)v : error("filter is not an object"))
				.orElse(JsonValue.EMPTY_JSON_OBJECT);

		final Shape filter=filter(query);

		final List<IRI> stats=stats(query);
		final List<IRI> items=items(query);

		final List<Order> order=order(query);

		final int offset=offset(query);
		final int limit=limit(query);

		final Shape merged=filter == null ? shape
				: and(shape, com.metreeca.form.Shape.filter().then(filter)); // mark as filtering only >> don't include in results

		final Shape optimized=merged.map(new Optimizer());

		return stats != null ? Stats.stats(optimized, stats)
				: items != null ? Items.items(optimized, items)
				: Edges.edges(optimized, order, offset, limit);

	}


	//// Query Properties //////////////////////////////////////////////////////////////////////////////////////////////

	private Shape filter(final JsonObject query) {
		return Optional.ofNullable(query.get("filter"))

				.filter(v -> !v.equals(JsonValue.NULL))

				.map(v -> v instanceof JsonObject ? v.asJsonObject() : error("filter is not an object"))
				.map(object -> shape(object, shape))

				.orElse(null);
	}


	private List<IRI> stats(final JsonObject query) {
		return Optional.ofNullable(query.get("stats"))

				.filter(v -> !v.equals(JsonValue.NULL))

				.map(v -> v instanceof JsonString ? (JsonString)v : error("stats field is not a string"))
				.map((path) -> path(path.getString(), shape))

				.orElse(null);
	}

	private List<IRI> items(final JsonObject query) {
		return Optional.ofNullable(query.get("items"))

				.filter(v -> !v.equals(JsonValue.NULL))

				.map(v -> v instanceof JsonString ? (JsonString)v : error("items field is not a string"))
				.map(path -> path(path.getString(), shape))

				.orElse(null);
	}


	private List<Order> order(final JsonObject query) {
		return Optional.ofNullable(query.get("order"))

				.filter(v -> !v.equals(JsonValue.NULL))

				.map(this::order)

				.orElse(emptyList());
	}

	private List<Order> order(final JsonValue object) {
		return object instanceof JsonString ? criteria((JsonString)object)
				: object instanceof JsonArray ? criteria((JsonArray)object)
				: error("order field is neither a string nor an array of strings");
	}


	private List<Order> criteria(final JsonArray object) {
		return object.stream()
				.map(v -> v instanceof JsonString ? (JsonString)v : error("order criterion is not a string"))
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
		return Optional.ofNullable(query.get("offset"))

				.filter(v -> !v.equals(JsonValue.NULL))

				.map(v -> v instanceof JsonNumber ? (JsonNumber)v : error("offset not a number"))
				.map(JsonNumber::intValue)
				.map(v ->v >= 0 ? v : error("negative offset"))

				.orElse(0);
	}

	private int limit(final JsonObject query) {
		return Optional.ofNullable(query.get("limit"))

				.filter(v -> !v.equals(JsonValue.NULL))

				.map(v -> v instanceof JsonNumber ? (JsonNumber)v : error("limit is not a number"))
				.map(JsonNumber::intValue)
				.map(v -> v >= 0 ? v : error("negative limit"))

				.orElse(0);
	}


	//// Paths /////////////////////////////////////////////////////////////////////////////////////////////////////////

	private List<IRI> path(final String path, final Shape shape) {

		final Collection<String> steps=new ArrayList<>();

		final Matcher matcher=StepPatten.matcher(path);

		final int length=path.length();

		int last=0;

		while ( matcher.lookingAt() ) {
			steps.add(matcher.group(1));
			matcher.region(last=matcher.end(), length);
		}

		if ( last != length ) {
			throw new JsonException("malformed path ["+path+"]");
		}

		return path(steps, shape);
	}


	private List<IRI> path(final Iterable<String> steps, final Shape shape) {

		final List<IRI> edges=new ArrayList<>();

		Shape reference=shape;

		for (final String step : steps) {

			final Map<IRI, com.metreeca.form.Shape> fields=fields(reference);
			final Map<IRI, String> aliases=aliases(reference);

			final Map<String, IRI> index=new HashMap<>();

			// leading '^' for inverse edges added by Values.Inverse.toString() and Values.format(IRI)

			for (final IRI edge : fields.keySet()) {
				index.put(format(edge), edge); // inside angle brackets
				index.put(edge.toString(), edge); // naked IRI
			}

			for (final Map.Entry<IRI, String> entry : aliases.entrySet()) {
				index.put(entry.getValue(), entry.getKey());
			}

			final IRI edge=index.get(step);

			if ( edge == null ) {
				throw new NoSuchElementException("unknown path step ["+step+"]");
			}

			edges.add(edge);
			reference=fields.get(edge);

		}

		return edges;

	}


	//// Shapes ////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shape shape(final JsonObject object, final Shape shape) {
		return and(object
				.entrySet()
				.stream()
				.map(entry -> shape(entry.getKey(), entry.getValue(), shape))
				.collect(toList())
		);
	}

	private Shape shape(final String key, final JsonValue value, final Shape shape) {

		if ( value.equals(JsonValue.NULL) ) { return and(); } else {

			switch ( key ) {

				case "^": return datatype(value);
				case "@": return clazz(value);

				case ">": return minExclusive(value, shape);
				case "<": return maxExclusive(value, shape);
				case ">=": return minInclusive(value, shape);
				case "<=": return maxInclusive(value, shape);

				case ">#": return minLength(value);
				case "#<": return maxLength(value);
				case "*": return pattern(value);
				case "~": return like(value);

				case ">>": return minCount(value);
				case "<<": return maxCount(value);
				case "!": return all(value, shape);
				case "?": return any(value, shape);

				default:

					return field(
							path(key, shape),
							value instanceof JsonObject && disjoint(value.asJsonObject().keySet(), Reserved)
									? (JsonObject)value
									: Json.createObjectBuilder().add("?", value).build(),
							shape
					);

			}
		}

	}


	private Shape datatype(final JsonValue value) {
		return value instanceof JsonString
				? Datatype.datatype(iri(resolve(((JsonString)value).getString())))
				: error("datatype IRI is not a string");
	}


	private Shape clazz(final JsonValue value) {
		return value instanceof JsonString
				? Clazz.clazz(iri(resolve(((JsonString)value).getString())))
				: error("class IRI is not a string");
	}


	private Shape minExclusive(final JsonValue value, final Shape shape) {
		return value != null
				? MinExclusive.minExclusive(value(value, shape, null).getKey())
				: error("value is null");
	}

	private Shape maxExclusive(final JsonValue value, final Shape shape) {
		return value != null
				? MaxExclusive.maxExclusive(value(value, shape, null).getKey())
				: error("value is null");
	}

	private Shape minInclusive(final JsonValue value, final Shape shape) {
		return value != null
				? MinInclusive.minInclusive(value(value, shape, null).getKey())
				: error("value is null");
	}

	private Shape maxInclusive(final JsonValue value, final Shape shape) {
		return value != null
				? MaxInclusive.maxInclusive(value(value, shape, null).getKey())
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

	private Shape all(final JsonValue value, final Shape shape) {
		if ( value.getValueType() == JsonValue.ValueType.NULL ) { return error("value is null"); } else {

			final Collection<Value> values=values(value, shape, null).keySet();

			return values.isEmpty() ? and() : All.all(values);
		}
	}

	private Shape any(final JsonValue value, final Shape shape) {
		if ( value.getValueType() == JsonValue.ValueType.NULL ) { return error("value is null"); } else {

			final Collection<Value> values=values(value, shape, null).keySet();

			return values.isEmpty() ? and() : Any.any(values);
		}
	}


	private Shape field(final List<IRI> path, final JsonObject object, final Shape shape) {
		if ( path.isEmpty() ) { return shape(object, shape); } else {

			final Map<IRI, Shape> fields=fields(shape); // !!! optimize (already explored during path parsing)

			final IRI head=path.get(0);
			final List<IRI> tail=path.subList(1, path.size());

			return Field.field(head, field(tail, object, fields.get(head)));
		}
	}

}
