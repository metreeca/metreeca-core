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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;

import javax.json.JsonException;

import static com.metreeca.form.Order.decreasing;
import static com.metreeca.form.Order.increasing;
import static com.metreeca.form.codecs.BaseCodec.aliases;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Field.fields;
import static com.metreeca.form.things.Values.format;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.literal;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;


@SuppressWarnings("unchecked") public final class QueryParser {

	private static final java.util.regex.Pattern StepPatten
			=java.util.regex.Pattern.compile("(?:^|[./])(\\^?(?:\\w+:.*|\\w+|<[^>]*>))");


	private final Shape shape;


	public QueryParser(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		this.shape=shape;
	}


	/**
	 * Parses a JSON object encoding a query.
	 *
	 * @param json the JSON object encodinf a shape-driven linked data query
	 *
	 * @return the parsed query
	 *
	 * @throws NullPointerException   if {@code json} is null
	 * @throws JsonException          if {@code json} is malformed
	 * @throws NoSuchElementException if the query encoded by {@code json} referes to data outside the {@linkplain
	 *                                #QueryParser(Shape) parser shape}
	 */
	public Query parse(final String json) throws JsonException {

		if ( json == null ) {
			throw new NullPointerException("null json");
		}

		final Map<String, Object> query=query(json);

		final Shape filter=filter(query);

		final List<IRI> stats=stats(query);
		final List<IRI> items=items(query);

		final List<Order> order=order(query);

		final int offset=offset(query);
		final int limit=limit(query);

		final Shape merged=filter == null ? shape
				: and(shape, Shape.filter().then(filter)); // mark as filtering only >> don't include in results

		final Shape optimized=merged.map(new Optimizer());

		return stats != null ? Stats.stats(optimized, stats)
				: items != null ? Items.items(optimized, items)
				: Edges.edges(optimized, order, offset, limit);

	}


	private Map<String, Object> query(final String json) {
		return Optional
				.of(json)
				.filter(v -> !v.isEmpty())
				.map(JSON::decode)
				.map(v -> v instanceof Map ? (Map<String, Object>)v : error("query is not a json object"))
				.orElseGet(JSON::object);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shape filter(final Map<String, Object> query) {

		return query.containsKey("filter") ? Optional

				.ofNullable(query.get("filter"))
				.map(v -> v instanceof Map ? (Map<String, Object>)v : error("filter is not an object"))

				.map(object -> shape(shape, object))
				.orElseGet(() -> error("filter is null")) : null;

	}


	private List<IRI> stats(final Map<String, Object> query) {
		return Optional.ofNullable(query.get("stats"))
				.map(v -> v instanceof String ? (String)v : error("stats field is not a string"))
				.map((path) -> path(path, shape))
				.orElse(null);
	}

	private List<IRI> items(final Map<String, Object> query) {
		return Optional.ofNullable(query.get("items"))
				.map(v -> v instanceof String ? (String)v : error("items field is not a string"))
				.map((path) -> path(path, shape))
				.orElse(null);
	}


	private List<Order> order(final Map<String, Object> query) {
		return Optional
				.ofNullable(query.get("order"))
				.map(this::order)
				.orElse(emptyList());
	}

	private List<Order> order(final Object object) {
		return object instanceof String ? criteria((String)object)
				: object instanceof Collection ? criteria((Collection<?>)object)
				: error("order field is neither a string nor an array of strings");
	}


	private List<Order> criteria(final Collection<?> object) {
		return object.stream()
				.map(v -> v instanceof String ? (String)v : error("order criterion is not a string"))
				.map(this::criterion)
				.collect(toList());
	}

	private List<Order> criteria(final String object) {
		return singletonList(criterion(object));
	}

	private Order criterion(final String criterion) {
		return criterion.startsWith("+") ? increasing(path(criterion.substring(1), shape))
				: criterion.startsWith("-") ? decreasing(path(criterion.substring(1), shape))
				: increasing(path(criterion, shape));
	}


	private int offset(final Map<String, Object> query) {
		return Optional
				.ofNullable(query.get("offset"))
				.map(v -> v instanceof Number ? (Number)v : error("offset not a number"))
				.map(v -> v.intValue() >= 0 ? v : error("negative offset"))
				.orElse(0)
				.intValue();
	}

	private int limit(final Map<String, Object> query) {
		return Optional
				.ofNullable(query.get("limit"))
				.map(v -> v instanceof Number ? (Number)v : error("limit is not a number"))
				.map(v -> v.intValue() >= 0 ? v : error("negative limit"))
				.orElse(0)
				.intValue();
	}


	private <V> V error(final String message) {
		throw new JsonException(message);
	}


	//// Shapes ////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shape shape(final Shape shape, final Map<String, Object> object) {
		return and(object
				.entrySet()
				.stream()
				.map(entry -> shape(entry.getKey(), shape, entry.getValue()))
				.collect(toList())
		);
	}

	private Shape shape(final String key, final Shape shape, final Object value) {

		switch ( key ) {

			case "^": return datatype(value);
			case "@": return clazz(value);

			case ">": return minExclusive(value);
			case "<": return maxExclusive(value);
			case ">=": return minInclusive(value);
			case "<=": return maxInclusive(value);

			case ">#": return minLength(value);
			case "#<": return maxLength(value);
			case "*": return pattern(value);
			case "~": return like(value);

			case ">>": return minCount(value);
			case "<<": return maxCount(value);
			case "?": return any(value);
			case "!": return all(value);

			default:

				return field(shape, path(key, shape),
						value instanceof Map ? (Map<String, Object>)value : singletonMap("?", value));

		}

	}


	private Shape datatype(final Object value) {
		return value instanceof String
				? Datatype.datatype(iri((String)value))
				: error("datatype IRI is not a string");
	}

	private Shape clazz(final Object value) {
		return value instanceof String
				? Clazz.clazz(iri((String)value))
				: error("class IRI is not a string");
	}


	private Shape minExclusive(final Object value) {
		return value != null
				? MinExclusive.minExclusive(value(value))
				: error("value is null");
	}

	private Shape maxExclusive(final Object value) {
		return value != null
				? MaxExclusive.maxExclusive(value(value))
				: error("value is null");
	}

	private Shape minInclusive(final Object value) {
		return value != null
				? MinInclusive.minInclusive(value(value))
				: error("value is null");
	}

	private Shape maxInclusive(final Object value) {
		return value != null
				? MaxInclusive.maxInclusive(value(value))
				: error("value is null");
	}


	private Shape minLength(final Object value) {
		return value instanceof Number
				? MinLength.minLength(((Number)value).intValue())
				: error("length is not a number");
	}

	private Shape maxLength(final Object value) {
		return value instanceof Number
				? MaxLength.maxLength(((Number)value).intValue())
				: error("length is not a number");
	}

	private Shape pattern(final Object value) {
		return value instanceof String
				? ((String)value).isEmpty() ? and() : Pattern.pattern((String)value)
				: error("pattern is not a string");
	}

	private Shape like(final Object value) {
		return value instanceof String
				? ((String)value).isEmpty() ? and() : Like.like((String)value)
				: error("pattern is not a string");
	}


	private Shape minCount(final Object value) {
		return value instanceof Number
				? MinCount.minCount(((Number)value).intValue())
				: error("length is not a number");
	}

	private Shape maxCount(final Object value) {
		return value instanceof Number
				? MaxCount.maxCount(((Number)value).intValue())
				: error("length is not a number");
	}

	private Shape all(final Object value) {
		return value != null
				? values(value).isEmpty() ? and() : All.all(values(value))
				: error("value is null");
	}

	private Shape any(final Object value) {
		return value != null
				? values(value).isEmpty() ? and() : Any.any(values(value))
				: error("value is null");
	}


	private Shape field(final Shape shape, final List<IRI> path, final Map<String, Object> object) {
		if ( path.isEmpty() ) { return shape(shape, object); } else {

			final Map<IRI, Shape> fields=fields(shape); // !!! optimize (already explored during path parsing)

			final IRI head=path.get(0);
			final List<IRI> tail=path.subList(1, path.size());

			return Field.field(head, field(fields.get(head), tail, object));
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Collection<Value> values(final Object object) {
		return object instanceof Collection ? values((Collection<Object>)object)
				: singleton(value(object));
	}

	private Collection<Value> values(final Collection<Object> array) {
		return array.stream().map(this::value).collect(toList());
	}

	private Value value(final Object object) {
		return object instanceof Map ? value((Map<String, Object>)object)
				: object instanceof BigDecimal ? literal((BigDecimal)object)
				: object instanceof BigInteger ? literal((BigInteger)object)
				: object instanceof Number ? literal(((Number)object).doubleValue())
				: object != null ? literal(object.toString())
				: null;
	}

	private Value value(final Map<String, Object> object) {
		return iri(object.get("this").toString());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
			throw new IllegalArgumentException("malformed path ["+path+"]");
		}

		return path(steps, shape);
	}

	private List<IRI> path(final Iterable<String> steps, final Shape shape) {

		final List<IRI> edges=new ArrayList<>();

		Shape reference=shape;

		for (final String step : steps) {

			final Map<IRI, Shape> fields=fields(reference);
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

}
