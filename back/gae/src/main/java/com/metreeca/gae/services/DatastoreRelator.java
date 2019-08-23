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

package com.metreeca.gae.services;

import com.metreeca.gae.GAE;
import com.metreeca.rest.Future;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.rest.services.Logger;
import com.metreeca.tree.Order;
import com.metreeca.tree.Query.Probe;
import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Traverser;
import com.metreeca.tree.queries.Items;
import com.metreeca.tree.queries.Stats;
import com.metreeca.tree.queries.Terms;
import com.metreeca.tree.shapes.*;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.SortDirection;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.metreeca.gae.GAE.key;
import static com.metreeca.gae.formats.EntityFormat.entity;
import static com.metreeca.gae.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.services.Logger.logger;
import static com.metreeca.tree.shapes.Clazz.clazz;

import static com.google.appengine.api.datastore.Entity.KEY_RESERVED_PROPERTY;
import static com.google.appengine.api.datastore.Query.FilterOperator.EQUAL;
import static com.google.appengine.api.datastore.Query.FilterOperator.GREATER_THAN_OR_EQUAL;
import static com.google.appengine.api.datastore.Query.FilterOperator.IN;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;


final class DatastoreRelator extends DatastoreProcessor {

	private final Datastore datastore=service(datastore());
	private final DatastoreSplitter splitter=new DatastoreSplitter();

	private final Logger logger=service(logger());


	Future<Response> handle(final Request request) {
		return request.container() ? container(request) : resource(request);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Future<Response> container(final Request request) {
		return request.query(splitter.resource(request.shape()), entity()::value)
				.value(query -> query.map(new QueryProbe(request.path())))
				.fold(request::reply, request::reply);
	}

	private Future<Response> resource(final Request request) {
		return request.reply(response -> datastore.exec(service -> {

			final Shape shape=convey(request.shape());
			final Key key=key(request.path(), shape);

			final Query query=new Query(key.getKind()) // !!! set filters from filter shape?
					.setFilter(new Query.FilterPredicate(KEY_RESERVED_PROPERTY, EQUAL, key));

			// ;( projecting only properties actually included in the shape would lower costs, as projection queries
			// are counted as small operations: unfortunately, a number of limitations apply:
			// https://cloud.google.com/appengine/docs/standard/java/datastore/projectionqueries#Java_Limitations_on_projections
			// https://cloud.google.com/appengine/docs/standard/java/datastore/projectionqueries#Java_Projections_and_multiple_valued_properties

			return Optional.ofNullable(service.prepare(query).asSingleEntity())

					.map(entity -> response
							.status(OK)
							.shape(shape)
							.body(entity(), entity)
					)

					.orElseGet(() -> response
							.status(NotFound)
					);

		}));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final class QueryProbe implements Probe<Function<Response, Response>> {

		private final String path;


		private QueryProbe(final String path) {
			this.path=path;
		}


		@Override public Function<Response, Response> probe(final Items items) { // !!! refactor

			final Shape convey=convey(items.getShape());
			final Shape filter=filter(items.getShape());

			// compile query

			final Query query=new Query(clazz(filter).orElse("*"));

			Optional.ofNullable(filter.map(new FilterProbe(""))).ifPresent(query::setFilter);

			final List<String> inequalities=filter.map(new InequalityProbe("")).distinct().collect(toList());
			final List<Order> orders=items.getOrders();

			if ( inequalities.isEmpty() && orders.isEmpty() ) {

				query.addSort(KEY_RESERVED_PROPERTY); // force a consistent default ordering

			} else {

				// sort first on inequality filtered paths
				// see https://cloud.google.com/appengine/docs/standard/java/datastore/query-restrictions#properties_used_in_inequality_filters_must_be_sorted_first

				inequalities.forEach(query::addSort);

				orders.forEach(order -> query.addSort(
						String.join(".", order.getPath()), // !!! handle/reject path steps containing dots
						order.isInverse() ? SortDirection.DESCENDING : SortDirection.ASCENDING
				));

			}

			// !!! re-sort as postprocessor if inequalities are used and sorting criteria are not consistent


			// compile post-processing predicate

			final Predicate<Object> predicate=Optional.ofNullable(filter.map(new PredicateProbe())).orElse(o -> true);


			// compile fetch options // !!! support cursors

			final FetchOptions options=FetchOptions.Builder.withDefaults();

			Optional.of(items.getOffset()).filter(offset -> offset > 0).ifPresent(options::offset);
			Optional.of(items.getLimit()).filter(limit -> limit > 0).ifPresent(options::limit);


			// retrieve data

			logger.info(DatastoreRelator.this, String.format("executing query %s", query));

			return datastore.exec(service -> {

				final Entity container=new Entity("*", path);

				container.setProperty(GAE.contains,
						stream(service.prepare(query).asIterable(options).spliterator(), false)
								.filter(predicate)
								.map(this::embed) // ;( entities can't be embedded
								.collect(toList())
				);

				return response -> response
						.status(OK) // containers are virtual and respond always with 200 OK
						.shape(convey)
						.body(entity(), container);

			});

		}

		@Override public Function<Response, Response> probe(final Terms terms) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Function<Response, Response> probe(final Stats stats) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}


		private EmbeddedEntity embed(final Entity entity) {

			final EmbeddedEntity resource=new EmbeddedEntity();

			resource.setKey(entity.getKey());
			resource.setPropertiesFrom(entity);

			return resource;
		}

	}


	/**
	 * Identifies paths subject to inequality constraints.
	 */
	private static final class InequalityProbe extends Traverser<Stream<String>> {

		private final String path;


		private InequalityProbe(final String path) {
			this.path=path;
		}


		@Override public Stream<String> probe(final Shape shape) {
			return Stream.empty();
		}


		@Override public Stream<String> probe(final Guard guard) {
			throw new UnsupportedOperationException(guard.toString());
		}


		@Override public Stream<String> probe(final MinExclusive minExclusive) { return Stream.of(path); }

		@Override public Stream<String> probe(final MaxExclusive maxExclusive) {return Stream.of(path); }

		@Override public Stream<String> probe(final MinInclusive minInclusive) { return Stream.of(path); }

		@Override public Stream<String> probe(final MaxInclusive maxInclusive) { return Stream.of(path); }


		@Override public Stream<String> probe(final Field field) {
			return field.getShape().map(new InequalityProbe(path.isEmpty()? field.getName() : path+"."+field.getName()));
		}

		@Override public Stream<String> probe(final And and) {
			return and.getShapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<String> probe(final Or or) {
			return or.getShapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<String> probe(final When when) {
			throw new UnsupportedOperationException(when.toString());
		}

	}

	private static final class FilterProbe implements Shape.Probe<Query.Filter> {

		private final String path;


		private FilterProbe(final String path) {
			this.path=path;
		}


		@Override public Query.Filter probe(final Meta meta) {
			return null;
		}

		@Override public Query.Filter probe(final Guard guard) {
			throw new UnsupportedOperationException(guard.toString());
		}


		@Override public Query.Filter probe(final Datatype datatype) {
			return null; // handled by predicate
		}

		@Override public Query.Filter probe(final Clazz clazz) {
			return null; // top-level handled by query / nested handled by predicate
		}


		@Override public Query.Filter probe(final MinExclusive minExclusive) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Query.Filter probe(final MaxExclusive maxExclusive) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Query.Filter probe(final MinInclusive minInclusive) {
			return new Query.FilterPredicate(path, GREATER_THAN_OR_EQUAL, minInclusive.getValue());
		}

		@Override public Query.Filter probe(final MaxInclusive maxInclusive) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}


		@Override public Query.Filter probe(final MinLength minLength) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Query.Filter probe(final MaxLength maxLength) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Query.Filter probe(final Pattern pattern) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Query.Filter probe(final Like like) {
			return null; // handled by predicate
		}


		@Override public Query.Filter probe(final MinCount minCount) {
			return null; // handled by predicate
		}

		@Override public Query.Filter probe(final MaxCount maxCount) {
			return null; // handled by predicate
		}

		@Override public Query.Filter probe(final In in) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Query.Filter probe(final All all) {

			final Set<Object> values=all.getValues();

			return values.isEmpty() ? null
					: values.size() == 1 ? new Query.FilterPredicate(path, EQUAL, values.iterator().next())
					: new Query.CompositeFilter(Query.CompositeFilterOperator.AND, values.stream()
					.map(value -> new Query.FilterPredicate(path, EQUAL, value))
					.collect(toList())
			);
		}

		@Override public Query.Filter probe(final Any any) {

			final Set<Object> values=any.getValues();

			return values.isEmpty() ? null
					: values.size() == 1 ? new Query.FilterPredicate(path, EQUAL, values.iterator().next())
					: new Query.FilterPredicate(path, IN, values);
		}


		@Override public Query.Filter probe(final Field field) { // !!! intercept/handle field names containing dots
			return field.getShape().map(new FilterProbe(path.isEmpty() ? field.getName() : path+"."+field.getName()));
		}


		@Override public Query.Filter probe(final And and) {

			final List<Query.Filter> filters=and.getShapes().stream()
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.collect(toList());

			return filters.isEmpty() ? null
					: filters.size() == 1 ? filters.get(0)
					: new Query.CompositeFilter(Query.CompositeFilterOperator.AND, filters);
		}

		@Override public Query.Filter probe(final Or or) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Query.Filter probe(final When when) {
			throw new UnsupportedOperationException(when.toString());
		}

	}

	private static final class PredicateProbe implements Shape.Probe<Predicate<Object>> {

		@Override public Predicate<Object> probe(final Meta meta) {
			return null;
		}

		@Override public Predicate<Object> probe(final Guard guard) {
			throw new UnsupportedOperationException(guard.toString());
		}


		@Override public Predicate<Object> probe(final Datatype datatype) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Predicate<Object> probe(final Clazz clazz) {

			final String name=clazz.getName();

			return values -> stream(values).allMatch(value
					-> value instanceof Entity && ((Entity)value).getKey().getKind().equals(name)
					|| value instanceof EmbeddedEntity && ((EmbeddedEntity)value).getKey().getKind().equals(name)
			);
		}


		@Override public Predicate<Object> probe(final MinExclusive minExclusive) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Predicate<Object> probe(final MaxExclusive maxExclusive) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Predicate<Object> probe(final MinInclusive minInclusive) {
			return null; // handled by query
		}

		@Override public Predicate<Object> probe(final MaxInclusive maxInclusive) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}


		@Override public Predicate<Object> probe(final MinLength minLength) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Predicate<Object> probe(final MaxLength maxLength) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Predicate<Object> probe(final Pattern pattern) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Predicate<Object> probe(final Like like) {

			final java.util.regex.Pattern pattern=java.util.regex.Pattern.compile(like.toExpression());

			return values -> stream(values).allMatch(value ->
					value != null && pattern.matcher(value.toString()).find()
			);
		}


		@Override public Predicate<Object> probe(final MinCount minCount) {

			final int limit=minCount.getLimit();

			return values -> count(values) >= limit;
		}

		@Override public Predicate<Object> probe(final MaxCount maxCount) {

			final int limit=maxCount.getLimit();

			return values -> count(values) <= limit;
		}

		@Override public Predicate<Object> probe(final In in) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Predicate<Object> probe(final All all) {
			return null; // handled by query
		}

		@Override public Predicate<Object> probe(final Any any) {
			return null; // handled by query
		}


		@Override public Predicate<Object> probe(final Field field) {

			final String name=field.getName();
			final Predicate<Object> predicate=field.getShape().map(this);

			return predicate == null ? null : values -> stream(values).allMatch(value ->
					value instanceof PropertyContainer && predicate.test(((PropertyContainer)value).getProperty(name))
			);
		}


		@Override public Predicate<Object> probe(final And and) {

			final List<Predicate<Object>> predicates=and.getShapes().stream()
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.collect(toList());

			return predicates.isEmpty() ? null
					: predicates.size() == 1 ? predicates.get(0)
					: values -> predicates.stream().allMatch(predicate -> predicate.test(values));
		}

		@Override public Predicate<Object> probe(final Or or) {

			final List<Predicate<Object>> predicates=or.getShapes().stream()
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.collect(toList());

			return predicates.isEmpty() ? null
					: predicates.size() == 1 ? predicates.get(0)
					: values -> predicates.stream().anyMatch(predicate -> predicate.test(values));
		}

		@Override public Predicate<Object> probe(final When when) {
			throw new UnsupportedOperationException(when.toString());
		}


		private int count(final Object value) {
			return value == null ? 0
					: value instanceof Collection ? ((Collection<?>)value).size()
					: 1;
		}

		private Stream<?> stream(final Object value) {
			return value == null ? Stream.empty()
					: value instanceof Collection ? ((Collection<?>)value).stream()
					: Stream.of(value);
		}

	}

}
