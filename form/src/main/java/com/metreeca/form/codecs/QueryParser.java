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

import org.eclipse.rdf4j.model.IRI;

import java.io.StringReader;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.json.*;

import static com.metreeca.form.Order.decreasing;
import static com.metreeca.form.Order.increasing;
import static com.metreeca.form.shapes.And.and;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;


public final class QueryParser {

	private final Shape shape;
	private final JSONDecoder decoder;


	public QueryParser(final Shape shape, final String base) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( base == null ) {
			throw new NullPointerException("null base");
		}

		this.shape=shape;
		this.decoder=new JSONDecoder(base);

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

		final JsonObject query=query(json);

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


	private JsonObject query(final String json) {
		return Optional.of(json)
				.filter(v -> !v.isEmpty())
				.map(v -> Json.createReader(new StringReader(v)).readValue())
				.map(v -> v instanceof JsonObject ? (JsonObject)v : decoder.error("filter is not an object"))
				.orElse(JsonValue.EMPTY_JSON_OBJECT);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shape filter(final JsonObject query) {

		return query.containsKey("filter") ? Optional

				.ofNullable(query.get("filter"))
				.map(v -> v instanceof JsonObject ? v.asJsonObject() : decoder.error("filter is not an object"))

				.map(object -> decoder.shape(object, shape))
				.orElseGet(() -> decoder.error("filter is null")) : null;

	}


	private List<IRI> stats(final JsonObject query) {
		return Optional.ofNullable(query.get("stats"))
				.map(v -> v instanceof JsonString ? (JsonString)v : decoder.error("stats field is not a string"))
				.map((path) -> decoder.path(path.getString(), shape))
				.orElse(null);
	}

	private List<IRI> items(final JsonObject query) {
		return Optional.ofNullable(query.get("items"))
				.map(v -> v instanceof JsonString ? (JsonString)v : decoder.error("items field is not a string"))
				.map(path -> decoder.path(path.getString(), shape))
				.orElse(null);
	}


	private List<Order> order(final JsonObject query) {
		return Optional
				.ofNullable(query.get("order"))
				.map(this::order)
				.orElse(emptyList());
	}

	private List<Order> order(final JsonValue object) {
		return object instanceof JsonString ? criteria((JsonString)object)
				: object instanceof JsonArray ? criteria((JsonArray)object)
				: decoder.error("order field is neither a string nor an array of strings");
	}


	private List<Order> criteria(final JsonArray object) {
		return object.stream()
				.map(v -> v instanceof JsonString ? (JsonString)v : decoder.error("order criterion is not a string"))
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
		return criterion.startsWith("+") ? increasing(decoder.path(criterion.substring(1), shape))
				: criterion.startsWith("-") ? decreasing(decoder.path(criterion.substring(1), shape))
				: increasing(decoder.path(criterion, shape));
	}


	private int offset(final JsonObject query) {
		return Optional
				.ofNullable(query.get("offset"))
				.map(v -> v instanceof JsonNumber ? (JsonNumber)v : decoder.error("offset not a number"))
				.map(JsonNumber::intValue)
				.map(v ->v >= 0 ? v : decoder.error("negative offset"))
				.orElse(0);
	}

	private int limit(final JsonObject query) {
		return Optional
				.ofNullable(query.get("limit"))
				.map(v -> v instanceof JsonNumber ? (JsonNumber)v : decoder.error("limit is not a number"))
				.map(JsonNumber::intValue)
				.map(v -> v >= 0 ? v : decoder.error("negative limit"))
				.orElse(0);
	}

}
