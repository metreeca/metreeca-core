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
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.metreeca.gae.formats.EntityFormat.entity;
import static com.metreeca.gae.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.services.Logger.logger;
import static com.metreeca.tree.Shape.multiple;
import static com.metreeca.tree.Shape.optional;
import static com.metreeca.tree.Shape.required;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Field.field;

import static com.google.appengine.api.datastore.Entity.KEY_RESERVED_PROPERTY;
import static com.google.appengine.api.datastore.FetchOptions.Builder;
import static com.google.appengine.api.datastore.Query.FilterOperator.*;

import static java.util.Comparator.comparing;
import static java.util.Map.Entry.comparingByKey;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;
import static java.util.stream.StreamSupport.stream;


final class DatastoreRelator extends DatastoreProcessor {

	private static final Shape TermShape=and(
			field(GAE.label, and(optional(), datatype(GAE.String)))
	);

	private static final Shape TermsShape=and(
			field(GAE.terms, and(multiple(),
					field(GAE.value, and(required(), TermShape)),
					field(GAE.count, and(required(), datatype(GAE.Integral)))
			))
	);

	private static final Shape StatsShape=and(

			field(GAE.count, and(required(), datatype(GAE.Integral))),
			field(GAE.min, and(optional(), TermShape)),
			field(GAE.max, and(optional(), TermShape)),

			field(GAE.stats, and(multiple(),
					field(GAE.type, and(required(), datatype(GAE.String))),
					field(GAE.count, and(required(), datatype(GAE.Integral))),
					field(GAE.min, and(required(), TermShape)),
					field(GAE.max, and(required(), TermShape))
			))

	);


	private static String property(final Iterable<String> path) {
		return String.join(".", path);  // !!! handle/reject path steps containing dots
	}

	private static String property(final String head, final String tail) {
		return head.isEmpty() ? tail : head+"."+tail;  // !!! handle/reject path steps containing dots
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Datastore datastore=service(datastore());
	private final DatastoreSplitter splitter=new DatastoreSplitter();

	private final Logger logger=service(logger());


	Future<Response> handle(final Request request) {
		return request.container() ? container(request) : resource(request);
	}


	//// Container /////////////////////////////////////////////////////////////////////////////////////////////////////

	private Future<Response> container(final Request request) {
		return request.query(splitter.resource(expand(request.shape())), entity()::value)

				.value(query -> query.map(new Probe<Function<Response, Response>>() {

					@Override public Function<Response, Response> probe(final Items items) {
						return items(request.path(), items);
					}

					@Override public Function<Response, Response> probe(final Terms terms) {
						return terms(request.path(), terms);
					}

					@Override public Function<Response, Response> probe(final Stats stats) {
						return stats(request.path(), stats);
					}

				}))

				.fold(request::reply, request::reply);
	}


	private Function<Response, Response> items(final String path, final Items items) {

		final Shape shape=items.getShape();

		final List<Order> orders=items.getOrders();
		final int offset=items.getOffset();
		final int limit=items.getLimit();

		final Entity container=new Entity(GAE.key(path));

		container.setProperty(GAE.contains, entities(path, shape, orders, offset, limit, entities -> entities

				.map(GAE::embed) // ;( entities can't be embedded
				.collect(toList())

		));

		return response -> response
				.status(OK) // containers are virtual and respond always with 200 OK
				.shape(field(GAE.contains, convey(shape)))
				.body(entity(), container);

	}

	private Function<Response, Response> terms(final String path, final Terms terms) {

		final Entity container=new Entity(GAE.key(path));

		container.setProperty(GAE.terms, values(path, terms.getShape(), terms.getPath(), values -> values

				.collect(groupingBy(v -> v, counting()))
				.entrySet()
				.stream()

				.sorted(Map.Entry.<Object, Long>

						comparingByValue().reversed() // decreasing count

						.thenComparing(comparingByKey(

								comparing(v -> GAE.get(v, "", GAE.label)) // increasing label

										.thenComparing(GAE::compare) // increasing value

						))

				)

				.map(entry -> {

					final EmbeddedEntity embedded=new EmbeddedEntity();

					embedded.setProperty(GAE.value, entry.getKey());
					embedded.setProperty(GAE.count, entry.getValue());

					return embedded;

				})

				.collect(toList())
		));

		return response -> response
				.status(OK)
				.shape(TermsShape)
				.body(entity(), container);

	}

	private Function<Response, Response> stats(final String path, final Stats stats) {

		final class Range {

			private final long count;

			private final Object min;
			private final Object max;

			private Range(final long count, final Object min, final Object max) {

				this.count=count;

				this.min=min;
				this.max=max;
			}


			private int order(final Range range) {
				return -Long.compare(count, range.count);
			}

			private Range merge(final Range range) {
				return new Range(
						count+range.count,
						GAE.compare(min, range.min) <= 0 ? min : range.min,
						GAE.compare(max, range.max) >= 0 ? max : range.max
				);
			}

			private void set(final PropertyContainer container) {

				container.setProperty(GAE.count, count);
				container.setProperty(GAE.min, min);
				container.setProperty(GAE.max, max);

			}

		}

		final Map<String, Range> ranges=values(path, stats.getShape(), stats.getPath(), values -> values

				.collect(groupingBy(GAE::type, reducing(null, v -> new Range(1, v, v), (x, y) ->
						x == null ? y : y == null ? x : x.merge(y)
				)))

		);

		final Entity container=new Entity(GAE.key(path));

		if ( ranges.isEmpty() ) {

			container.setProperty(GAE.count, 0L);

		} else {

			ranges.values().stream() // global stats
					.reduce(Range::merge)
					.orElse(new Range(0, null, null)) // unexpected
					.set(container);

			container.setProperty(GAE.stats, ranges.entrySet().stream() // type-specific stats

					.sorted(Map.Entry
							.<String, Range>comparingByValue(Range::order) // decreasing count
							.thenComparing(comparingByKey()) // increasing type
					)

					.map(entry -> {

						final EmbeddedEntity item=new EmbeddedEntity();

						item.setProperty(GAE.type, entry.getKey());
						entry.getValue().set(item);

						return item;

					})

					.collect(toList())

			);

		}

		return response -> response
				.status(OK)
				.shape(StatsShape)
				.body(entity(), container);

	}


	//// Resource //////////////////////////////////////////////////////////////////////////////////////////////////////

	private Future<Response> resource(final Request request) {
		return request.reply(response -> datastore.exec(service -> {

			final Shape shape=convey(request.shape());
			final Key key=GAE.key(request.path(), shape);

			final Query query=new Query(key.getKind()) // !!! set filters from filter shape?
					.setFilter(new FilterPredicate(KEY_RESERVED_PROPERTY, EQUAL, key));

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

	private <R> R values(
			final String path, final Shape shape,
			final Iterable<String> steps,
			final Function<Stream<Object>, R> task
	) {

		return entities(path, shape, null, 0, 0, entities -> task.apply(entities

				// !!! (€) if property is known to be of a scalar supported type retrieve using a projection
				// https://cloud.google.com/appengine/docs/standard/java/javadoc/com/google/appengine/api/datastore/RawValue.html#getValue--

				.map(entity -> GAE.get(entity, steps))

				.filter(Objects::nonNull)

		));
	}

	private <R> R entities(
			final String path, final Shape shape,
			final List<Order> orders, final int offset, final int limit,
			final Function<Stream<Entity>, R> task
	) {

		// ;( inequality filters are limited to at most one property: identify them and assign to query/predicate
		// see https://cloud.google.com/appengine/docs/standard/java/datastore/query-restrictions#inequality_filters_are_limited_to_at_most_one_property

		final Shape filter=filter(shape);
		final List<String> inequalities=inequalities(filter);

		final Query query=query(path, filter, orders, inequalities);

		final FetchOptions options=Builder.withDefaults(); // !!! support cursors // !!! hard sampling limits?

		if ( offset > 0 ) { options.offset(offset); }
		if ( limit > 0 ) { options.limit(limit); }

		final Optional<Predicate<Object>> predicate=predicate(filter, inequalities); // handle residual constraints
		final Optional<Comparator<Entity>> sorter=sorter(orders, inequalities); // handle inconsistent sorting/inequalities

		logger.info(this, String.format("executing query %s", query));

		return datastore.exec(service -> Optional.of(stream(service.prepare(query).asIterable(options).spliterator(), false))
				.map(entities -> predicate.map(entities::filter).orElse(entities))
				.map(entities -> sorter.map(entities::sorted).orElse(entities))
				.map(task)
				.get()
		);

	}


	private List<String> inequalities(final Shape shape) {
		return shape

				.map(new InequalityProbe(""))
				.collect(groupingBy(identity(), counting()))

				.entrySet()
				.stream()

				.sorted(Map.Entry.<String, Long>comparingByValue().reversed()) // prefer multiple constrains // !!! (€) most selective

				.map(Map.Entry::getKey)
				.collect(toList());
	}


	private Query query(final String path, final Shape shape, final List<Order> orders, final List<String> inequalities) {

		final Key ancestor=GAE.key(path);

		final Query query=clazz(shape)
				.map(clazz -> new Query(clazz, ancestor))
				.orElseGet(() -> new Query(ancestor));

		filter(shape, inequalities).ifPresent(query::setFilter);

		// ;( properties used in inequality filters must be sorted first
		// see https://cloud.google.com/appengine/docs/standard/java/datastore/query-restrictions#properties_used_in_inequality_filters_must_be_sorted_first

		inequalities.forEach(query::addSort);

		if ( orders != null && consistent(inequalities, orders) ) {

			orders.stream().skip(inequalities.size()).forEach(order -> query.addSort(
					order.getPath().isEmpty() ? KEY_RESERVED_PROPERTY : property(order.getPath()),
					order.isInverse() ? SortDirection.DESCENDING : SortDirection.ASCENDING
			));

			if ( orders.stream().noneMatch(o -> o.getPath().isEmpty()) ) { // omit if already included
				query.addSort(KEY_RESERVED_PROPERTY); // force a consistent default/residual ordering
			}

		}

		return query;
	}

	private Optional<Comparator<Entity>> sorter(final List<Order> orders, final List<String> inequalities) {
		return Optional.ofNullable((orders == null || consistent(inequalities, orders)) ? null : orders.stream()

				.map(order -> {

					final List<String> path=order.getPath();
					final boolean inverse=order.isInverse();

					final Comparator<Entity> comparator=comparing(
							path.isEmpty() ? Entity::getKey : e -> GAE.get(e, path), GAE::compare
					);

					return inverse ? comparator.reversed() : comparator;

				})

				.reduce(Comparator::thenComparing)
				.map(comparator -> comparator.thenComparing(comparing(Entity::getKey)))
				.orElse(comparing(Entity::getKey))
		);
	}

	private Optional<Query.Filter> filter(final Shape shape, final List<String> inequalities) {
		return Optional.ofNullable(shape.map(new FilterProbe("",
				inequalities.isEmpty() ? inequalities : inequalities.subList(0, 1)
		)));
	}

	private Optional<Predicate<Object>> predicate(final Shape shape, final List<String> inequalities) {
		return Optional.ofNullable(shape.map(new PredicateProbe("",
				inequalities.isEmpty() ? inequalities : inequalities.subList(1, inequalities.size())
		)));
	}


	private boolean consistent(final List<String> inequalities, final List<Order> orders) {
		return inequalities.isEmpty() || !orders.isEmpty() && property(orders.get(0).getPath()).equals(inequalities.get(0));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private abstract static class RelatorProbe<T> extends Traverser<T> {

		@Override public T probe(final Guard guard) {
			throw new UnsupportedOperationException(guard.toString());
		}

		@Override public T probe(final When when) {
			throw new UnsupportedOperationException(when.toString());
		}

	}


	/**
	 * Identifies paths subject to inequality constraints.
	 */
	private static final class InequalityProbe extends RelatorProbe<Stream<String>> {

		private final String path;


		private InequalityProbe(final String path) {
			this.path=path;
		}


		@Override public Stream<String> probe(final Shape shape) {
			return Stream.empty();
		}


		@Override public Stream<String> probe(final MinExclusive minExclusive) {
			return Stream.of(path);
		}

		@Override public Stream<String> probe(final MaxExclusive maxExclusive) {
			return Stream.of(path);
		}

		@Override public Stream<String> probe(final MinInclusive minInclusive) {
			return Stream.of(path);
		}

		@Override public Stream<String> probe(final MaxInclusive maxInclusive) {
			return Stream.of(path);
		}


		@Override public Stream<String> probe(final Field field) {
			return field.getShape().map(new InequalityProbe(property(path, field.getName())));
		}

		@Override public Stream<String> probe(final And and) {
			return and.getShapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<String> probe(final Or or) {
			throw new UnsupportedOperationException("disjunctive inequality operators {"+or+"}");
		}

	}

	private static final class FilterProbe extends RelatorProbe<Query.Filter> {

		private final String path;
		private final List<String> inequalities;


		private FilterProbe(final String path, final List<String> inequalities) {
			this.path=path;
			this.inequalities=inequalities;
		}


		@Override public Query.Filter probe(final MinExclusive minExclusive) {
			return inequalities.contains(path)
					? op(path, GREATER_THAN, minExclusive.getValue())
					: null;
		}

		@Override public Query.Filter probe(final MaxExclusive maxExclusive) {
			return inequalities.contains(path)
					? op(path, LESS_THAN, maxExclusive.getValue())
					: null;
		}

		@Override public Query.Filter probe(final MinInclusive minInclusive) {
			return inequalities.contains(path)
					? op(path, GREATER_THAN_OR_EQUAL, minInclusive.getValue())
					: null;
		}

		@Override public Query.Filter probe(final MaxInclusive maxInclusive) {
			return inequalities.contains(path)
					? op(path, LESS_THAN_OR_EQUAL, maxInclusive.getValue())
					: null;
		}


		@Override public Query.Filter probe(final All all) {
			return and(all.getValues().stream()
					.map(value -> op(path, EQUAL, value))
					.collect(toList())
			);
		}

		@Override public Query.Filter probe(final Any any) {

			final Set<Object> values=any.getValues();

			return values.isEmpty() ? null
					: values.size() == 1 ? op(path, EQUAL, values.iterator().next())
					: values.stream().noneMatch(v -> v instanceof EmbeddedEntity) ? new FilterPredicate(path, IN, values)
					: values.stream().allMatch(v -> v instanceof EmbeddedEntity) ? new FilterPredicate(key(path), IN,
					values.stream().map(value -> ((EmbeddedEntity)value).getKey()).collect(toList())
			)
					: or(values.stream().map(value -> op(path, EQUAL, value)).collect(toList()));
		}


		@Override public Query.Filter probe(final Field field) {
			return field.getShape().map(new FilterProbe(property(path, field.getName()), inequalities));
		}


		@Override public Query.Filter probe(final And and) {
			return and(and.getShapes().stream()
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.collect(toList())
			);
		}

		@Override public Query.Filter probe(final Or or) {
			return or(or.getShapes().stream()
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.collect(toList())
			);
		}


		private Query.Filter and(final List<Query.Filter> filters) {
			return filters.isEmpty() ? null
					: filters.size() == 1 ? filters.get(0)
					: new Query.CompositeFilter(Query.CompositeFilterOperator.AND, filters);
		}

		private Query.Filter or(final List<Query.Filter> filters) {
			return filters.isEmpty() ? null
					: filters.size() == 1 ? filters.get(0)
					: new Query.CompositeFilter(Query.CompositeFilterOperator.OR, filters);
		}


		private FilterPredicate op(final String path, final Query.FilterOperator op, final Object value) {
			return value instanceof EmbeddedEntity
					? new FilterPredicate(key(path), op, ((EmbeddedEntity)value).getKey())
					: new FilterPredicate(path, op, value);
		}


		private String key(final String path) {
			return path+"."+KEY_RESERVED_PROPERTY;
		}

	}

	private static final class PredicateProbe extends RelatorProbe<Predicate<Object>> {

		private final String path;
		private final List<String> inequalities;


		private PredicateProbe(final String path, final List<String> inequalities) {
			this.path=path;
			this.inequalities=inequalities;
		}


		@Override public Predicate<Object> probe(final Datatype datatype) {

			final String name=datatype.getName();

			return values -> stream(values).allMatch(value ->
					name.equals(GAE.type(value))
			);
		}

		@Override public Predicate<Object> probe(final Clazz clazz) { // top level handled by Query

			final String name=clazz.getName();

			return path.isEmpty() ? null : values -> stream(values).allMatch(value ->
					value instanceof PropertyContainer && name.equals(GAE.kind((PropertyContainer)value))
			);
		}


		@Override public Predicate<Object> probe(final MinExclusive minExclusive) {

			final Object limit=minExclusive.getValue();

			return inequalities.contains(path)
					? values -> stream(values).allMatch(value -> GAE.compare(value, limit) > 0)
					: null;
		}

		@Override public Predicate<Object> probe(final MaxExclusive maxExclusive) {

			final Object limit=maxExclusive.getValue();

			return inequalities.contains(path)
					? values -> stream(values).allMatch(value -> GAE.compare(value, limit) < 0)
					: null;
		}

		@Override public Predicate<Object> probe(final MinInclusive minInclusive) {

			final Object limit=minInclusive.getValue();

			return inequalities.contains(path)
					? values -> stream(values).allMatch(value -> GAE.compare(value, limit) >= 0)
					: null;
		}

		@Override public Predicate<Object> probe(final MaxInclusive maxInclusive) {

			final Object limit=maxInclusive.getValue();

			return inequalities.contains(path)
					? values -> stream(values).allMatch(value -> GAE.compare(value, limit) <= 0)
					: null;
		}


		@Override public Predicate<Object> probe(final MinLength minLength) {

			final int limit=minLength.getLimit();

			return values -> stream(values).allMatch(value ->
					(value == null ? 0 : value.toString().length()) >= limit
			);
		}

		@Override public Predicate<Object> probe(final MaxLength maxLength) {

			final int limit=maxLength.getLimit();

			return values -> stream(values).allMatch(value ->
					(value == null ? 0 : value.toString().length()) <= limit
			);
		}

		@Override public Predicate<Object> probe(final Pattern pattern) {

			final java.util.regex.Pattern regex=java.util.regex.Pattern.compile(
					String.format("(?%s:%s)", pattern.getFlags(), pattern.getText())
			);

			return values -> stream(values).allMatch(value ->
					value != null && regex.matcher(value.toString()).matches()
			);
		}

		@Override public Predicate<Object> probe(final Like like) {

			final java.util.regex.Pattern regex=java.util.regex.Pattern.compile(like.toExpression());

			return values -> stream(values).allMatch(value ->
					value != null && regex.matcher(value.toString()).find()
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

			final Set<Object> range=in.getValues();

			return range.isEmpty() ? values -> true : values -> stream(values).allMatch(value ->
					value != null && range.contains(value)
			);
		}


		@Override public Predicate<Object> probe(final Field field) {

			final String name=field.getName();
			final Predicate<Object> predicate=field.getShape().map(new PredicateProbe(property(path, field.getName()), inequalities));

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
