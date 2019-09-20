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
import com.metreeca.gae.formats.EntityFormat;
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
import com.google.cloud.datastore.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.metreeca.gae.GAE.isEntity;
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

import static com.google.appengine.api.datastore.Query.FilterOperator.*;
import static com.google.cloud.datastore.StructuredQuery.PropertyFilter.Operator.GREATER_THAN;
import static com.google.cloud.datastore.StructuredQuery.PropertyFilter.Operator.LESS_THAN;
import static com.google.cloud.datastore.StructuredQuery.PropertyFilter.eq;
import static com.google.cloud.datastore.ValueType.LONG;
import static com.google.cloud.datastore.ValueType.STRING;

import static java.util.Comparator.comparing;
import static java.util.Map.Entry.comparingByKey;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;
import static java.util.stream.StreamSupport.stream;


final class DatastoreRelator extends DatastoreProcessor {

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


	private static boolean entity(final Object limit) {
		return Datastore.value(limit).getType() == ValueType.ENTITY;
	}

	private static String key(final String path) {
		return path+".__key__";
	}


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
		return request.query(splitter.resource(expand(request.shape())), EntityFormat.entity()::value)

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

		return response -> response
				.status(OK) // containers are virtual and respond always with 200 OK
				.shape(field(GAE.contains, convey(shape)))
				.body(EntityFormat.entity(), Entity.newBuilder(datastore.key(GAE.Resource, path))

						.set(GAE.contains, this.<List<EntityValue>>entities(path, shape, orders, offset, limit, entities -> entities
								.map(EntityValue::of)
								.collect(toList())
						))

						.build()

				);

	}

	private Function<Response, Response> terms(final String path, final Terms terms) {

		final Comparator<Map.Entry<Value<?>, Long>> xxx=Map.Entry.<Value<?>, Long>

				comparingByValue().reversed();

		final Comparator<Map.Entry<Value<?>, Long>> yyy=xxx

				.thenComparing(comparingByKey(

						comparing(v -> GAE.get(v, "", GAE.label)) // increasing label

								.thenComparing(GAE::compare) // increasing value

				));

		final List<EntityValue> values1=values(path, terms.getShape(), terms.getPath(), values -> values

				.collect(groupingBy(v -> v, counting()))
				.entrySet()
				.stream()

				.sorted(yyy // decreasing count


				)

				.map(entry -> FullEntity.newBuilder()

						.set(GAE.value, entry.getKey())
						.set(GAE.count, entry.getValue())

						.build()

				)

				.collect(toList())
		);

		return response -> response
				.status(OK)
				.shape(TermsShape)
				.body(EntityFormat.entity(), Entity.newBuilder(datastore.key(GAE.Resource, path))

						.set(GAE.terms, values1)

						.build()

				);

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

		final Map<ValueType, Range> ranges=values(path, stats.getShape(), stats.getPath(), values -> values

				.collect(groupingBy(Value::getType, reducing(null, v -> new Range(1, v, v), (x, y) ->
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
							.<ValueType, Range>comparingByValue(Range::order) // decreasing count
							.thenComparing(comparingByKey()) // increasing type
					)

					.map(entry -> {

						final FullEntity.Builder<?> item=FullEntity.newBuilder()

								.set(GAE.type, entry.getKey().toString());

						entry.getValue().set(item);

						return EntityValue.of(item.build());

					})

					.collect(toList())

			);

		}

		return response -> response
				.status(OK)
				.shape(StatsShape)
				.body(EntityFormat.entity(), container.build());

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
							.body(EntityFormat.entity(), entity)
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

				.map(entity -> {

					Value<?> value=EntityValue.of(entity);

					for (final String step : steps) {
						if ( value != null && value.getType() == ValueType.ENTITY ) {
							value=((EntityValue)value).get().contains(step) ? ((EntityValue)value).get().getValue(step) : null;
						}
					}

					return value;
				})

				.filter(Objects::nonNull)

		));
	}

	private <R> R entities(
			final String path, final Shape shape,
			final List<Order> orders, final int offset, final int limit,
			final Function<Stream<Entity>, R> task
	) { // !!! support cursors // !!! hard sampling limits?

		// ;( inequality filters are limited to at most one property: identify them and assign to query/predicate
		// see https://cloud.google.com/appengine/docs/standard/java/datastore/query-restrictions#inequality_filters_are_limited_to_at_most_one_property

		final Shape filter=filter(shape);
		final List<String> inequalities=inequalities(filter);

		final EntityQuery query=query(path, filter, orders, inequalities);

		final Optional<Predicate<Value<?>>> predicate=predicate(filter, inequalities); // handle residual constraints
		final Optional<Comparator<Entity>> sorter=sorter(orders, inequalities); // handle inconsistent sorting/inequalities

		logger.info(this, String.format("executing query %s", query));

		return datastore.exec(service -> Optional.of(stream(spliteratorUnknownSize(service.run(query), ORDERED), false))
				.map(entities -> predicate.map(entities::filter).orElse(entities))
				.map(entities -> sorter.map(entities::sorted).orElse(entities))
				.map(entities -> offset > 0 ? entities.skip(offset) : entities)
				.map(entities -> limit > 0 ? entities.limit(offset) : entities)
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


	private EntityQuery query(final String path, final Shape shape, final List<Order> orders, final List<String> inequalities) {

		final Key ancestor=GAE.key(path);

		final EntityQuery.Builder query=clazz(shape)
				.map(clazz -> new Query(clazz, ancestor))
				.orElseGet(() -> new Query(ancestor));

		query.setFilter()

		filter(shape, inequalities).ifPresent(query::setFilter);

		// ;( properties used in inequality filters must be sorted first
		// see https://cloud.google.com/appengine/docs/standard/java/datastore/query-restrictions#properties_used_in_inequality_filters_must_be_sorted_first

		inequalities.forEach(query::addSort);

		if ( orders != null && consistent(inequalities, orders) ) {

			orders.stream().skip(inequalities.size()).forEach(order -> query.addSort(
					order.getPath().isEmpty() ? "__key__" : property(order.getPath()),
					order.isInverse() ? SortDirection.DESCENDING : SortDirection.ASCENDING
			));

			if ( orders.stream().noneMatch(o -> o.getPath().isEmpty()) ) { // omit if already included
				query.addSort("__key__"); // force a consistent default/residual ordering
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

	private Optional<StructuredQuery.Filter> filter(final Shape shape, final List<String> inequalities) {
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
			return Stream.of(entity(minExclusive.getValue()) ? key(path) : path);
		}

		@Override public Stream<String> probe(final MaxExclusive maxExclusive) {
			return Stream.of(entity(maxExclusive.getValue()) ? key(path) : path);
		}

		@Override public Stream<String> probe(final MinInclusive minInclusive) {
			return Stream.of(entity(minInclusive.getValue()) ? key(path) : path);
		}

		@Override public Stream<String> probe(final MaxInclusive maxInclusive) {
			return Stream.of(entity(maxInclusive.getValue()) ? key(path) : path);
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

	private static final class FilterProbe extends RelatorProbe<StructuredQuery.Filter> {

		private final String path;
		private final List<String> inequalities;


		private FilterProbe(final String path, final List<String> inequalities) {
			this.path=path;
			this.inequalities=inequalities;
		}


		@Override public StructuredQuery.Filter probe(final MinExclusive minExclusive) {

			final Object limit=minExclusive.getValue();

			return inequalities.contains(entity(limit) ? key(path) : path)
					? op(path, GREATER_THAN, limit)
					: null;
		}

		@Override public StructuredQuery.Filter probe(final MaxExclusive maxExclusive) {

			final Object limit=maxExclusive.getValue();

			return inequalities.contains(entity(limit) ? key(path) : path)
					? op(path, LESS_THAN, limit)
					: null;
		}

		@Override public StructuredQuery.Filter probe(final MinInclusive minInclusive) {

			final Object limit=minInclusive.getValue();

			return inequalities.contains(entity(limit) ? key(path) : path)
					? op(path, GREATER_THAN_OR_EQUAL, limit)
					: null;
		}

		@Override public StructuredQuery.Filter probe(final MaxInclusive maxInclusive) {

			final Object limit=maxInclusive.getValue();

			return inequalities.contains(entity(limit) ? key(path) : path)
					? op(path, StructuredQuery.LESS_THAN_OR_EQUAL, limit)
					: null;
		}


		@Override public StructuredQuery.Filter probe(final All all) {
			return and(all.getValues().stream()
					.map(value -> op(path, EQUAL, value))
					.collect(toList())
			);
		}

		@Override public StructuredQuery.Filter probe(final Any any) {

			final Set<Object> values=any.getValues();

			return values.isEmpty() ? null
					: values.size() == 1 ? op(path, EQUAL, values.iterator().next())
					: values.stream().noneMatch(v -> entity(v)) ? new FilterPredicate(path, IN, values)
					: values.stream().allMatch(v -> entity(v)) ? new FilterPredicate(key(path), IN,
					values.stream().map(GAE::key).collect(toList())
			)
					: or(values.stream().map(value -> op(path, EQUAL, value)).collect(toList()));
		}


		@Override public StructuredQuery.Filter probe(final Field field) {
			return field.getShape().map(new FilterProbe(property(path, field.getName()), inequalities));
		}


		@Override public StructuredQuery.Filter probe(final And and) {
			return and(and.getShapes().stream()
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.collect(toList())
			);
		}

		@Override public StructuredQuery.Filter probe(final Or or) {
			return or(or.getShapes().stream()
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.collect(toList())
			);
		}


		private StructuredQuery.Filter and(final List<Query.Filter> filters) {
			return filters.isEmpty() ? null
					: filters.size() == 1 ? filters.get(0)
					: new Query.CompositeFilter(Query.CompositeFilterOperator.AND, filters);
		}

		private StructuredQuery.Filter or(final List<Query.Filter> filters) {
			return filters.isEmpty() ? null
					: filters.size() == 1 ? filters.get(0)
					: new Query.CompositeFilter(Query.CompositeFilterOperator.OR, filters);
		}


		private StructuredQuery.Filter op(final String path, final Query.FilterOperator op, final Object value) {
			return entity(value)
					? new FilterPredicate(key(path), op, ((EntityValue)value).get().getKey())
					: new FilterPredicate(path, op, value);
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

			final Object name=datatype.getName();

			return values -> stream(values).allMatch(value ->
					name.equals(Datastore.value(value).getType())
			);
		}

		@Override public Predicate<Value<?>> probe(final Clazz clazz) { // top level handled by Query

			final String name=clazz.getName().toString();

			return path.isEmpty() ? null : values -> stream(values).allMatch(value -> Optional.of(value)

					.filter(v -> v.getType() == ValueType.ENTITY)
					.map(v -> ((EntityValue)v).get().getKey())
					.filter(Objects::nonNull)
					.map(BaseKey::getKind)
					.filter(kind -> kind.equals(name))
					.isPresent()

			);
		}


		@Override public Predicate<Value<?>> probe(final MinExclusive minExclusive) {

			final Value<?> limit=Datastore.value(minExclusive.getValue());

			return inequalities.contains(limit.getType() == ValueType.ENTITY ? key(path) : path)
					? values -> stream(values).allMatch(value -> Datastore.compare(value, limit) > 0)
					: null;
		}

		@Override public Predicate<Value<?>> probe(final MaxExclusive maxExclusive) {

			final Value<?> limit=Datastore.value(maxExclusive.getValue());

			return inequalities.contains(limit.getType() == ValueType.ENTITY ? key(path) : path)
					? values -> stream(values).allMatch(value -> Datastore.compare(value, limit) < 0)
					: null;
		}

		@Override public Predicate<Value<?>> probe(final MinInclusive minInclusive) {

			final Value<?> limit=Datastore.value(minInclusive.getValue());

			return inequalities.contains(limit.getType() == ValueType.ENTITY ? key(path) : path)
					? values -> stream(values).allMatch(value -> Datastore.compare(value, limit) >= 0)
					: null;
		}

		@Override public Predicate<Value<?>> probe(final MaxInclusive maxInclusive) {

			final Value<?> limit=Datastore.value(maxInclusive.getValue());

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

			final Function<Value<?>, Value<?>> mapper=value -> Optional.of(value)
					.filter(v -> v.getType() == ValueType.ENTITY)
					.map(v -> ((EntityValue)v).get())
					.map(BaseEntity::getKey)
					.filter(k -> k instanceof Key)
					.map(k -> (Value<?>)KeyValue.of((Key)k))
					.orElse((Value<?>)value);

			final Set<Value<?>> range=in.getValues().stream()
					.map(Datastore::value)
					.map(mapper)
					.collect(toSet());

			return range.isEmpty() ? values -> true : values -> stream(values).allMatch(value ->
					range.contains(mapper.apply(value))
			);
		}


		@Override public Predicate<Value<?>> probe(final Field field) {

			final String name=field.getName().toString();

			final Predicate<Value<?>> predicate=field.getShape()
					.map(new PredicateProbe(property(path, name), inequalities));

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


		private int count(final Value<?> value) {
			return value.getType() == ValueType.NULL ? 0
					: value.getType() == ValueType.LIST ? ((ListValue)value).get().size()
					: 1;
		}

		private Stream<? extends Value<?>> stream(final Value<?> value) {
			return value.getType() == ValueType.NULL ? Stream.empty()
					: value.getType() == ValueType.LIST ? ((ListValue)value).get().stream()
					: Stream.of(value);
		}


		private String string(final Value<?> value) {
			return value.getType() == ValueType.NULL ? ""

					: value.getType() == ValueType.ENTITY ? Optional.of(value)
					.map(v -> ((EntityValue)v).get().getKey())
					.filter(key -> key instanceof Key)
					.map(key -> ((Key)key).getName())
					.orElse("")

					: value.get().toString();
		}

	}

}
