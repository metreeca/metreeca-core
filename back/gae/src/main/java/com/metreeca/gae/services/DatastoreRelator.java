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

import com.google.cloud.datastore.*;
import com.google.cloud.datastore.StructuredQuery.Filter;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.metreeca.gae.formats.EntityFormat.entity;
import static com.metreeca.gae.services.Datastore.datastore;
import static com.metreeca.gae.services.Datastore.value;
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

import static com.google.cloud.datastore.StructuredQuery.CompositeFilter.and;
import static com.google.cloud.datastore.StructuredQuery.OrderBy.Direction.ASCENDING;
import static com.google.cloud.datastore.StructuredQuery.OrderBy.Direction.DESCENDING;
import static com.google.cloud.datastore.StructuredQuery.OrderBy.asc;
import static com.google.cloud.datastore.StructuredQuery.PropertyFilter.eq;
import static com.google.cloud.datastore.StructuredQuery.PropertyFilter.hasAncestor;
import static com.google.cloud.datastore.ValueType.LONG;
import static com.google.cloud.datastore.ValueType.STRING;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingLong;
import static java.util.Map.Entry.comparingByKey;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;
import static java.util.stream.StreamSupport.stream;


final class DatastoreRelator extends DatastoreProcessor {

	private static final String KeyProperty="__key__";


	private static final Shape TermShape=and(
			field(GAE.label, and(optional(), datatype(STRING)))
	);

	private static final Shape TermsShape=and(
			field(GAE.terms, and(multiple(),
					field(GAE.value, and(required(), TermShape)),
					field(GAE.count, and(required(), datatype(LONG)))
			))
	);

	private static final Shape StatsShape=and(

			field(GAE.count, and(required(), datatype(LONG))),
			field(GAE.min, and(optional(), TermShape)),
			field(GAE.max, and(optional(), TermShape)),

			field(GAE.stats, and(multiple(),
					field(GAE.type, and(required(), datatype(STRING))),
					field(GAE.count, and(required(), datatype(LONG))),
					field(GAE.min, and(required(), TermShape)),
					field(GAE.max, and(required(), TermShape))
			))

	);


	private static String key(final String path) {
		return path+"."+KeyProperty;
	}


	private static String property(final Iterable<String> path) {
		return String.join(".", path);  // !!! handle/reject path steps containing dots
	}

	private static String property(final String head, final String tail) {
		return head.isEmpty() ? tail : head+"."+tail;  // !!! handle/reject path steps containing dots
	}


	private static String label(final Value<?> value) {
		return Optional.of(value)
				.filter(v -> v.getType() == ValueType.ENTITY)
				.map(v -> ((EntityValue)v).get())
				.filter(e -> e.contains(GAE.label))
				.map(e -> e.getString(GAE.label))
				.orElse("");
	}

	private static Value<?> get(final Value<?> entity, final Iterable<String> steps) {

		Value<?> value=entity;

		for (final String step : steps) {
			if ( value.getType() == ValueType.ENTITY ) {
				value=((EntityValue)value).get().contains(step) ?
						((EntityValue)value).get().getValue(step) : NullValue.of();
			}
		}

		return value;
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

		final Entity container=Entity.newBuilder(datastore.key(GAE.Resource, path))

				.set(GAE.contains, this.<List<EntityValue>>entities(path, shape, orders, offset, limit, entities -> entities

						.collect(toList())

				))

				.build();

		return response -> response
				.status(OK) // containers are virtual and respond always with 200 OK
				.shape(field(GAE.contains, convey(shape)))
				.body(entity(), container);

	}

	private Function<Response, Response> terms(final String path, final Terms terms) {

		final Comparator<Map.Entry<? extends Value<?>, Long>> byCount=comparingLong(Map.Entry::getValue);
		final Comparator<Map.Entry<? extends Value<?>, Long>> byLabel=comparing(x -> label(x.getKey()));
		final Comparator<Map.Entry<? extends Value<?>, Long>> byValue=comparing(Map.Entry::getKey, Datastore::compare);

		final Entity container=Entity.newBuilder(datastore.key(GAE.Resource, path))

				.set(GAE.terms, this.<List<EntityValue>>values(path, terms.getShape(), terms.getPath(), values -> values

						.collect(groupingBy(v -> v, counting()))
						.entrySet()
						.stream()

						.sorted(byCount.reversed().thenComparing(byLabel.thenComparing(byValue)))

						.map(entry -> FullEntity.newBuilder()

								.set(GAE.value, entry.getKey())
								.set(GAE.count, entry.getValue())

								.build())

						.map(EntityValue::of)
						.collect(toList())
				))

				.build();

		return response -> response
				.status(OK)
				.shape(TermsShape)
				.body(entity(), container);

	}

	private Function<Response, Response> stats(final String path, final Stats stats) {

		final class Range {

			private final long count;

			private final Value<?> min;
			private final Value<?> max;

			private Range(final long count, final Value<?> min, final Value<?> max) {

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
						Datastore.compare(min, range.min) <= 0 ? min : range.min,
						Datastore.compare(max, range.max) >= 0 ? max : range.max
				);
			}

			private void set(final BaseEntity.Builder<?, ?> container) {

				container.set(GAE.count, count);
				container.set(GAE.min, min);
				container.set(GAE.max, max);

			}

		}


		final Map<String, Range> ranges=values(path, stats.getShape(), stats.getPath(), values -> values

				.collect(groupingBy(v -> v.getType().toString(), reducing(null, v -> new Range(1, v, v), (x, y) ->
						x == null ? y : y == null ? x : x.merge(y)
				)))

		);

		final Entity.Builder container=Entity.newBuilder(datastore.key(GAE.Resource, path));

		if ( ranges.isEmpty() ) {

			container.set(GAE.count, 0L);

		} else {

			ranges.values().stream() // global stats
					.reduce(Range::merge)
					.orElse(new Range(0, null, null)) // unexpected
					.set(container);

			container.set(GAE.stats, ranges.entrySet().stream() // type-specific stats

					.sorted(Map.Entry
							.<String, Range>comparingByValue(Range::order) // decreasing count
							.thenComparing(comparingByKey()) // increasing datatype
					)

					.map(entry -> {

						final FullEntity.Builder<?> item=FullEntity.newBuilder();

						item.set(GAE.type, entry.getKey());
						entry.getValue().set(item);

						return item.build();

					})

					.map(EntityValue::of)
					.collect(toList()));

		}

		return response -> response
				.status(OK)
				.shape(StatsShape)
				.body(entity(), container.build());

	}


	//// Resource //////////////////////////////////////////////////////////////////////////////////////////////////////

	private Future<Response> resource(final Request request) {
		return request.reply(response -> datastore.exec(service -> {

			final Shape shape=convey(request.shape());
			final Key key=datastore.key(shape, request.path());

			// ;( projecting only properties actually included in the shape would lower costs, as projection queries
			// are counted as small operations: unfortunately, a number of limitations apply:
			// https://cloud.google.com/appengine/docs/standard/java/datastore/projectionqueries#Java_Limitations_on_projections
			// https://cloud.google.com/appengine/docs/standard/java/datastore/projectionqueries#Java_Projections_and_multiple_valued_properties

			return Optional.ofNullable(service.get(key))

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
			final Function<Stream<? extends Value<?>>, R> task
	) {

		return entities(path, shape, null, 0, 0, entities -> task.apply(entities

				// !!! (€) if property is known to be of a scalar supported type retrieve using a projection
				// https://cloud.google.com/appengine/docs/standard/java/javadoc/com/google/appengine/api/datastore/RawValue.html#getValue--

				.map(entity -> get(entity, steps))

				.filter(v -> v.getType() != ValueType.NULL)

		));
	}

	private <R> R entities(
			final String path, final Shape shape,
			final List<Order> orders, final int offset, final int limit,
			final Function<Stream<EntityValue>, R> task
	) { // !!! support cursors // !!! hard sampling limits?

		// ;( inequality filters are limited to at most one property: identify them and assign to query/predicate
		// see https://cloud.google.com/appengine/docs/standard/java/datastore/query-restrictions#inequality_filters_are_limited_to_at_most_one_property

		final Shape filter=filter(shape);
		final List<String> inequalities=inequalities(filter);

		final EntityQuery query=query(path, filter, orders, offset, limit, inequalities);

		final Optional<Predicate<Value<?>>> predicate=predicate(filter, inequalities); // handle residual constraints
		final Optional<Comparator<EntityValue>> sorter=sorter(orders, inequalities); // handle inconsistent sorting/inequalities

		logger.info(this, String.format("executing query %s", query));

		return datastore.exec(service -> Optional.of(stream(spliteratorUnknownSize(service.run(query), ORDERED), false))
				.map(entities -> entities.map(EntityValue::of))
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


	private EntityQuery query(final String path, final Shape shape,
			final List<Order> orders, final int offset, final int limit,
			final List<String> inequalities
	) {


		final EntityQuery.Builder builder=Query.newEntityQueryBuilder();

		clazz(shape).ifPresent(clazz -> builder.setKind(clazz.toString()));

		final Filter ancestor=hasAncestor(datastore.key(GAE.Resource, path));

		builder.setFilter(filter(shape, inequalities)
				.map(filter -> (Filter)and(ancestor, filter))
				.orElse(ancestor)
		);

		// ;( properties used in inequality filters must be sorted first
		// see https://cloud.google.com/appengine/docs/standard/java/datastore/query-restrictions#properties_used_in_inequality_filters_must_be_sorted_first

		inequalities.forEach(inequality -> builder.addOrderBy(asc(inequality)));

		if ( orders != null && consistent(inequalities, orders) ) {

			orders.stream().skip(inequalities.size()).forEach(order -> builder.addOrderBy(new OrderBy(
					order.getPath().isEmpty() ? KeyProperty : property(order.getPath()),
					order.isInverse() ? DESCENDING : ASCENDING
			)));

			if ( orders.stream().noneMatch(o -> o.getPath().isEmpty()) ) { // omit if already included
				builder.addOrderBy(asc(KeyProperty)); // force a consistent default/residual ordering
			}

		}

		if ( offset > 0 ) { builder.setOffset(offset); }
		if ( limit > 0 ) { builder.setLimit(offset); }

		return builder.build();
	}

	private Optional<Comparator<EntityValue>> sorter(final List<Order> orders, final List<String> inequalities) {
		return Optional.ofNullable((orders == null || consistent(inequalities, orders)) ? null : orders.stream()

				.map(order -> {

					final List<String> path=order.getPath();
					final boolean inverse=order.isInverse();

					final Comparator<EntityValue> comparator=comparing(e -> get(e, path), Datastore::compare);

					return inverse ? comparator.reversed() : comparator;

				})

				.reduce(Comparator::thenComparing)
				.orElse(Datastore::compare)
		);
	}

	private Optional<Filter> filter(final Shape shape, final List<String> inequalities) {
		return Optional.ofNullable(shape.map(new FilterProbe("",
				inequalities.isEmpty() ? inequalities : inequalities.subList(0, 1)
		)));
	}

	private Optional<Predicate<Value<?>>> predicate(final Shape shape, final List<String> inequalities) {
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
			return Stream.of(minExclusive.getValue() instanceof FullEntity ? key(path) : path);
		}

		@Override public Stream<String> probe(final MaxExclusive maxExclusive) {
			return Stream.of(maxExclusive.getValue() instanceof FullEntity ? key(path) : path);
		}

		@Override public Stream<String> probe(final MinInclusive minInclusive) {
			return Stream.of(minInclusive.getValue() instanceof FullEntity ? key(path) : path);
		}

		@Override public Stream<String> probe(final MaxInclusive maxInclusive) {
			return Stream.of(maxInclusive.getValue() instanceof FullEntity ? key(path) : path);
		}


		@Override public Stream<String> probe(final Field field) {
			return field.getShape().map(new InequalityProbe(property(path, field.getName().toString())));
		}

		@Override public Stream<String> probe(final And and) {
			return and.getShapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<String> probe(final Or or) {
			throw new UnsupportedOperationException("disjunctive inequality operators {"+or+"}");
		}

	}

	private static final class FilterProbe extends RelatorProbe<Filter> {

		private final String path;
		private final List<String> inequalities;


		private FilterProbe(final String path, final List<String> inequalities) {
			this.path=path;
			this.inequalities=inequalities;
		}


		@Override public Filter probe(final MinExclusive minExclusive) {
			return inequality(value(minExclusive.getValue()), PropertyFilter::gt);
		}

		@Override public Filter probe(final MaxExclusive maxExclusive) {
			return inequality(value(maxExclusive.getValue()), PropertyFilter::lt);

		}

		@Override public Filter probe(final MinInclusive minInclusive) {
			return inequality(value(minInclusive.getValue()), PropertyFilter::ge);
		}

		@Override public Filter probe(final MaxInclusive maxInclusive) {
			return inequality(value(maxInclusive.getValue()), PropertyFilter::le);
		}


		@Override public Filter probe(final All all) {
			return and(all.getValues().stream()
					.map(Datastore::value)
					.map(value -> eq(path, value))
					.collect(toList())
			);
		}

		@Override public Filter probe(final Any any) {

			final Set<Object> values=any.getValues();

			return values.isEmpty() ? null
					: values.size() == 1 ? eq(path, value(values.iterator().next()))
					: values.stream().noneMatch(v -> isEntity(v)) ? in(path, values)
					: values.stream().allMatch(v -> isEntity(v)) ? in(key(path),
					values.stream().map(GAE::key).collect(toList())
			)
					: or(values.stream().map(value -> eq(path, value)).collect(toList()));
		}


		@Override public Filter probe(final Field field) {
			return field.getShape().map(new FilterProbe(property(path, field.getName().toString()), inequalities));
		}


		@Override public Filter probe(final And and) {
			return and(and.getShapes().stream()
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.collect(toList())
			);
		}

		@Override public Filter probe(final Or or) {
			return or(or.getShapes().stream()
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.collect(toList())
			);
		}


		private Filter and(final List<Filter> filters) {
			return filters.isEmpty() ? null
					: filters.size() == 1 ? filters.get(0)
					: and(filters);
		}

		private Filter or(final List<Filter> filters) {
			return filters.isEmpty() ? null
					: filters.size() == 1 ? filters.get(0)
					: StructuredQuery.CompositeFilter.or(filters);
		}


		private Filter inequality(final Object limit, final BiFunction<String, Value<?>, Filter> op) {
			if ( limit instanceof BaseEntity ) {

				final String key=key(path);
				final IncompleteKey key1=((BaseEntity<?>)limit).getKey();

				return inequalities.contains(key) && (key1 instanceof Key)
						? op.apply(key, KeyValue.of((Key)key1))
						: null;

			} else {

				return inequalities.contains(path) ? op.apply(path, value(limit)) : null;

			}
		}

	}

	private static final class PredicateProbe extends RelatorProbe<Predicate<Value<?>>> {

		private final String path;
		private final List<String> inequalities;


		private PredicateProbe(final String path, final List<String> inequalities) {
			this.path=path;
			this.inequalities=inequalities;
		}


		@Override public Predicate<Value<?>> probe(final Datatype datatype) {

			final String name=datatype.getName().toString();

			return values -> stream(values).allMatch(value ->
					name.equals(value.getType().toString())
			);
		}

		@Override public Predicate<Value<?>> probe(final Clazz clazz) { // top level handled by Query

			final String name=clazz.getName().toString();

			return path.isEmpty() ? null : values -> stream(values).allMatch(value ->
					Optional.of(value)
							.filter(v -> v.getType() == ValueType.ENTITY)
							.map(v -> (FullEntity<?>)v.get())
							.map(BaseEntity::getKey)
							.filter(Objects::nonNull)
							.map(BaseKey::getKind)
							.filter(kind -> kind.equals(name))
							.isPresent()

			);
		}


		@Override public Predicate<Value<?>> probe(final MinExclusive minExclusive) {

			final Value<?> limit=value(minExclusive.getValue());

			return inequalities.contains(limit.getType() == ValueType.ENTITY ? key(path) : path)
					? values -> stream(values).allMatch(value -> Datastore.compare(value, limit) > 0)
					: null;
		}

		@Override public Predicate<Value<?>> probe(final MaxExclusive maxExclusive) {

			final Value<?> limit=value(maxExclusive.getValue());

			return inequalities.contains(limit.getType() == ValueType.ENTITY ? key(path) : path)
					? values -> stream(values).allMatch(value -> Datastore.compare(value, limit) < 0)
					: null;
		}

		@Override public Predicate<Value<?>> probe(final MinInclusive minInclusive) {

			final Value<?> limit=value(minInclusive.getValue());

			return inequalities.contains(limit.getType() == ValueType.ENTITY ? key(path) : path)
					? values -> stream(values).allMatch(value -> Datastore.compare(value, limit) >= 0)
					: null;
		}

		@Override public Predicate<Value<?>> probe(final MaxInclusive maxInclusive) {

			final Value<?> limit=value(maxInclusive.getValue());

			return inequalities.contains(limit.getType() == ValueType.ENTITY ? key(path) : path)
					? values -> stream(values).allMatch(value -> Datastore.compare(value, limit) <= 0)
					: null;
		}


		@Override public Predicate<Value<?>> probe(final MinLength minLength) {

			final int limit=minLength.getLimit();

			return values -> stream(values).allMatch(value ->
					string(value).length() >= limit
			);
		}

		@Override public Predicate<Value<?>> probe(final MaxLength maxLength) {

			final int limit=maxLength.getLimit();

			return values -> stream(values).allMatch(value ->
					string(value).length() <= limit
			);
		}

		@Override public Predicate<Value<?>> probe(final Pattern pattern) {

			final java.util.regex.Pattern regex=java.util.regex.Pattern.compile(
					String.format("(?%s:%s)", pattern.getFlags(), pattern.getText())
			);

			return values -> stream(values).allMatch(value ->
					regex.matcher(string(value)).matches()
			);
		}

		@Override public Predicate<Value<?>> probe(final Like like) {

			final java.util.regex.Pattern regex=java.util.regex.Pattern.compile(like.toExpression());

			return values -> stream(values).allMatch(value ->
					regex.matcher(string(value)).find()
			);
		}


		@Override public Predicate<Value<?>> probe(final MinCount minCount) {

			final int limit=minCount.getLimit();

			return values -> count(values) >= limit;
		}

		@Override public Predicate<Value<?>> probe(final MaxCount maxCount) {

			final int limit=maxCount.getLimit();

			return values -> count(values) <= limit;
		}

		@Override public Predicate<Value<?>> probe(final In in) {

			final Function<Object, Object> mapper=value ->
					isEntity(value) ? GAE.key(value) : value;

			final Set<Object> range=in.getValues().stream()
					.map(mapper)
					.collect(toSet());

			return range.isEmpty() ? values -> true : values -> stream(values).allMatch(value ->
					value != null && range.contains(mapper.apply(value))
			);
		}


		@Override public Predicate<Value<?>> probe(final Field field) {

			final String name=field.getName().toString();
			final Predicate<Value<?>> predicate=field.getShape().map(new PredicateProbe(property(path, name), inequalities));

			return predicate == null ? null : values -> stream(values).allMatch(value ->
					value.getType() == ValueType.ENTITY
							&& ((EntityValue)value).get().contains(name)
							&& predicate.test(((EntityValue)value).get().getValue(name))
			);
		}


		@Override public Predicate<Value<?>> probe(final And and) {

			final List<Predicate<Value<?>>> predicates=and.getShapes().stream()
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.collect(toList());

			return predicates.isEmpty() ? null
					: predicates.size() == 1 ? predicates.get(0)
					: values -> predicates.stream().allMatch(predicate -> predicate.test(values));
		}

		@Override public Predicate<Value<?>> probe(final Or or) {

			final List<Predicate<Value<?>>> predicates=or.getShapes().stream()
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

		private Stream<Value<?>> stream(final Object value) {
			return value == null ? Stream.empty()
					: value instanceof Collection ? ((Collection<?>)value).stream().map(Datastore::value)
					: Stream.of(value(value));
		}


		private String string(final Value<?> value) {
			return value.getType() == ValueType.ENTITY

					? Optional.ofNullable(((EntityValue)value).get().getKey())
					.filter(k -> k instanceof Key)
					.map(k -> ((Key)k).getName())
					.orElse("")

					: value.get().toString();
		}

	}

}
