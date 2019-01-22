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

import com.metreeca.form.Query;
import com.metreeca.form.Order;
import com.metreeca.form.Shape;
import com.metreeca.form.queries.Edges;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;
import com.metreeca.form.shapes.*;
import com.metreeca.form.shifts.Step;

import org.eclipse.rdf4j.model.Value;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;

import javax.json.JsonException;

import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Any.any;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.form.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.form.shapes.MaxLength.maxLength;
import static com.metreeca.form.shapes.Meta.aliases;
import static com.metreeca.form.shapes.MinCount.minCount;
import static com.metreeca.form.shapes.MinExclusive.minExclusive;
import static com.metreeca.form.shapes.MinInclusive.minInclusive;
import static com.metreeca.form.shapes.MinLength.minLength;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.shapes.Trait.traits;
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

		final List<Step> stats=stats(query);
		final List<Step> items=items(query);

		final List<Order> order=order(query);

		final int offset=offset(query);
		final int limit=limit(query);

		final Shape merged=filter == null ? shape
				: and(shape, Shape.filter(filter)); // mark as filtering only >> don't include in results

		return stats != null ? new Stats(merged, stats)
				: items != null ? new Items(merged, items)
				: new Edges(merged, order, offset, limit);

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
				.map(v -> v instanceof Map ? ((Map<String, Object>)v) : error("filter is not an object"))

				.map(object -> filters(object, shape))
				.orElseGet(() -> error("filter is null")) : null;

	}

	private Shape filters(final Map<String, Object> object, final Shape shape) {

		final List<Shape> filters=object.entrySet().stream()
				.map(entry -> filter(entry.getKey(), entry.getValue(), shape))
				.collect(toList());

		return filters.size() == 1 ? filters.get(0) : and(filters);
	}

	private Shape filter(final String key, final Object value, final Shape shape) {

		if ( key.equals(">>") ) {

			return value instanceof Number ? minCount(((Number)value).intValue()) : error("length is not a number");

		} else if ( key.equals("<<") ) {

			return value instanceof Number ? maxCount(((Number)value).intValue()) : error("length is not a number");

		} else if ( key.equals(">") ) {

			return value != null ? minExclusive(object(value)) : error("value is null");

		} else if ( key.equals("<") ) {

			return value != null ? maxExclusive(object(value)) : error("value is null");

		} else if ( key.equals(">=") ) {

			return value != null ? minInclusive(object(value)) : error("value is null");

		} else if ( key.equals("<=") ) {

			return value != null ? maxInclusive(object(value)) : error("value is null");

		} else if ( key.equals("~") ) {

			return value instanceof String ? like((String)value) : error("pattern is not a string");

		} else if ( key.equals("*") ) {

			return value instanceof String ? pattern((String)value) : error("pattern is not a string");

		} else if ( key.equals(">#") ) {

			return value instanceof Number ? minLength(((Number)value).intValue()) : error("length is not a number");

		} else if ( key.equals("#<") ) {

			return value instanceof Number ? maxLength(((Number)value).intValue()) : error("length is not a number");

		} else if ( key.equals("@") ) {

			return value instanceof String ? Clazz.clazz(iri((String)value)) : error("class IRI is not a string");

		} else if ( key.equals("^") ) {

			return value instanceof String ? datatype(iri((String)value)) : error("datatype IRI is not a string");

		} else if ( key.equals("?") ) {

			return value != null ? existential(objects(value)) : error("value is null");

		} else if ( key.equals("!") ) {

			return value != null ? universal(objects(value)) : error("value is null");

		} else {

			return nested(shape, path(key, shape),
					value instanceof Map ? (Map<String, Object>)value : singletonMap("?", value));

		}

	}


	private Shape like(final String pattern) {
		return pattern.isEmpty() ? and() : Like.like(pattern);
	}

	private Shape pattern(final String pattern) {
		return pattern.isEmpty() ? and() : Pattern.pattern(pattern);
	}


	private Shape existential(final Collection<Value> objects) {
		return objects.isEmpty() ? and() : any(objects);
	}

	private Shape universal(final Collection<Value> objects) {
		return objects.isEmpty() ? and() : all(objects);
	}

	private Shape range(final Collection<Value> objects) {
		return objects.isEmpty() ? and() : In.in(objects);
	}


	private Shape nested(final Shape shape, final List<Step> path, final Map<String, Object> object) {
		if ( path.isEmpty() ) { return filters(object, shape); } else {

			final Map<Step, Shape> traits=traits(shape); // !!! optimize (already explored during path parsing)

			final Step head=path.get(0);
			final List<Step> tail=path.subList(1, path.size());

			return trait(head, nested(traits.get(head), tail, object));
		}
	}


	private List<Step> stats(final Map<String, Object> query) {
		return Optional.ofNullable(query.get("stats"))
				.map(v -> v instanceof String ? (String)v : error("stats field is not a string"))
				.map((path) -> path(path, shape))
				.orElse(null);
	}

	private List<Step> items(final Map<String, Object> query) {
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
		return criterion.startsWith("+") ? new Order(path(criterion.substring(1), shape), false)
				: criterion.startsWith("-") ? new Order(path(criterion.substring(1), shape), true)
				: new Order(path(criterion, shape), false);
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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Collection<Value> objects(final Object object) {
		return object instanceof List ? objects((List<Object>)object)
				: singleton(object(object));
	}

	private Collection<Value> objects(final Collection<Object> array) {
		return array.stream().map(this::object).collect(toList());
	}

	private Value object(final Object object) {
		return object instanceof Map ? object((Map<String, Object>)object)
				: object instanceof BigDecimal ? literal((BigDecimal)object)
				: object instanceof BigInteger ? literal((BigInteger)object)
				: object instanceof Number ? literal(((Number)object).doubleValue())
				: object != null ? literal(object.toString())
				: null;
	}

	private Value object(final Map<String, Object> object) {
		return iri(object.get("this").toString());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private List<Step> path(final String path, final Shape shape) {

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

	private List<Step> path(final Iterable<String> steps, final Shape shape) {

		final List<Step> edges=new ArrayList<>();

		Shape reference=shape;

		for (final String step : steps) {

			final Map<Step, Shape> traits=traits(reference);
			final Map<Step, String> aliases=aliases(reference);

			final Map<String, Step> index=new HashMap<>();

			for (final Step edge : traits.keySet()) {
				index.put(edge.format(), edge); // inside angle brackets
				index.put((edge.isInverse() ? "^" : "")+edge.getIRI(), edge); // naked IRI
			}

			for (final Map.Entry<Step, String> entry : aliases.entrySet()) {
				index.put(entry.getValue(), entry.getKey());
			}

			final Step edge=index.get(step);

			if ( edge == null ) {
				throw new NoSuchElementException("unknown path step ["+step+"]");
			}

			edges.add(edge);
			reference=traits.get(edge);

		}

		return edges;

	}

}
