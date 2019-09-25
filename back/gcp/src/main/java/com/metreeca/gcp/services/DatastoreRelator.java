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

package com.metreeca.gcp.services;

import com.metreeca.gcp.GCP;
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

import static com.metreeca.gcp.formats.EntityFormat.entity;
import static com.metreeca.gcp.services.Datastore.datastore;
import static com.metreeca.gcp.services.Datastore.value;
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
			field(GCP.label, and(optional(), datatype(STRING)))
	);

	private static final Shape TermsShape=and(
			field(GCP.terms, and(multiple(),
					field(GCP.value, and(required(), TermShape)),
					field(GCP.count, and(required(), datatype(LONG)))
			))
	);

	private static final Shape StatsShape=and(

			field(GCP.count, and(required(), datatype(LONG))),
			field(GCP.min, and(optional(), TermShape)),
			field(GCP.max, and(optional(), TermShape)),

			field(GCP.stats, and(multiple(),
					field(GCP.type, and(required(), datatype(STRING))),
					field(GCP.count, and(required(), datatype(LONG))),
					field(GCP.min, and(required(), TermShape)),
					field(GCP.max, and(required(), TermShape))
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
				.filter(e -> e.contains(GCP.label))
				.map(e -> e.getString(GCP.label))
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


	private static <V> V unsupported(final String message) {
		throw new UnsupportedOperationException(message);
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

		final Entity container=Entity.newBuilder(datastore.key(GCP.Resource, path))

				.set(GCP.contains, this.<List<EntityValue>>entities(path, shape, orders, offset, limit, entities -> entities

						.collect(toList())

				))

				.build();

		return response -> response
				.status(OK) // containers are virtual and respond always with 200 OK
				.shape(field(GCP.contains, convey(shape)))
				.body(entity(), container);

	}

	private Function<Response, Response> terms(final String path, final Terms terms) {

		final Comparator<Map.Entry<? extends Value<?>, Long>> byCount=comparingLong(Map.Entry::getValue);
		final Comparator<Map.Entry<? extends Value<?>, Long>> byLabel=comparing(x -> label(x.getKey()));
		final Comparator<Map.Entry<? extends Value<?>, Long>> byValue=comparing(Map.Entry::getKey, Datastore::compare);

		final Entity container=Entity.newBuilder(datastore.key(GCP.Resource, path))

				.set(GCP.terms, this.<List<EntityValue>>values(path, terms.getShape(), terms.getPath(), values -> values

						.collect(groupingBy(v -> v, counting()))
						.entrySet()
						.stream()

						.sorted(byCount.reversed().thenComparing(byLabel.thenComparing(byValue)))

						.map(entry -> FullEntity.newBuilder()

								.set(GCP.value, entry.getKey())
								.set(GCP.count, entry.getValue())

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

				container.set(GCP.count, count);
				container.set(GCP.min, min);
				container.set(GCP.max, max);

			}

		}


		final Map<String, Range> ranges=values(path, stats.getShape(), stats.getPath(), values -> values

				.collect(groupingBy(v -> v.getType().toString(), reducing(null, v -> new Range(1, v, v), (x, y) ->
						x == null ? y : y == null ? x : x.merge(y)
				)))

		);

		final Entity.Builder container=Entity.newBuilder(datastore.key(GCP.Resource, path));

		if ( ranges.isEmpty() ) {

			container.set(GCP.count, 0L);

		} else {

			ranges.values().stream() // global stats
					.reduce(Range::merge)
					.orElse(new Range(0, null, null)) // unexpected
					.set(container);

			container.set(GCP.stats, ranges.entrySet().stream() // type-specific stats

					.sorted(Map.Entry
							.<String, Range>comparingByValue(Range::order) // decreasing count
							.thenComparing(comparingByKey()) // increasing datatype
					)

					.map(entry -> {

						final FullEntity.Builder<?> item=FullEntity.newBuilder();

						item.set(GCP.type, entry.getKey());
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

		final EntityQuery query=query(path, filter, orders, inequalities);

		final Optional<Predicate<Value<?>>> predicate=predicate(filter, inequalities); // handle residual constraints
		final Optional<Comparator<EntityValue>> sorter=sorter(orders, inequalities); // handle inconsistent sorting/inequalities

		logger.info(this, String.format("executing query %s", query));

		return datastore.exec(service -> Optional.of(stream(spliteratorUnknownSize(service.run(query), ORDERED), false))

				.map(entities -> entities.map(EntityValue::of))
				.map(entities -> predicate.map(entities::filter).orElse(entities))

				// handle offset/limit after postprocessing filter/sort

				.map(entities -> sorter.map(entities::sorted).orElse(entities))
				.map(entities -> offset > 0 ? entities.skip(offset) : entities)
				.map(entities -> limit > 0 ? entities.limit(limit) : entities)

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
			final List<Order> orders,
			final List<String> inequalities
	) {


		final EntityQuery.Builder builder=Query.newEntityQueryBuilder();

		clazz(shape).ifPresent(clazz -> builder.setKind(clazz.toString()));

		final Filter ancestor=hasAncestor(datastore.key(GCP.Resource, path));

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
			return unsupported(guard.toString());
		}

		@Override public T probe(final When when) {
			return unsupported(when.toString());
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
			return unsupported("disjunctive inequality operators {"+or+"}");
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
			return filter(value(minExclusive.getValue()), PropertyFilter::gt, inequalities);
		}

		@Override public Filter probe(final MaxExclusive maxExclusive) {
			return filter(value(maxExclusive.getValue()), PropertyFilter::lt, inequalities);

		}

		@Override public Filter probe(final MinInclusive minInclusive) {
			return filter(value(minInclusive.getValue()), PropertyFilter::ge, inequalities);
		}

		@Override public Filter probe(final MaxInclusive maxInclusive) {
			return filter(value(maxInclusive.getValue()), PropertyFilter::le, inequalities);
		}


		@Override public Filter probe(final All all) {
			return and(all.getValues().stream()
					.map(Datastore::value)
					.map(value -> filter(value, PropertyFilter::eq, null))
					.collect(toList())
			);
		}

		@Override public Filter probe(final Any any) {

			final Set<Object> values=any.getValues();

			return values.isEmpty() ? null
					: values.size() == 1 ? filter(value(values.iterator().next()), PropertyFilter::eq, null)
					: null;
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
					: StructuredQuery.CompositeFilter.and( // ;( no collection constructor
							filters.get(0), filters.subList(1, filters.size()).toArray(new Filter[filters.size()-1])
			);
		}

		private Filter or(final List<Filter> filters) {
			return filters.isEmpty() ? null
					: filters.size() == 1 ? filters.get(0)
					: unsupported("disjunctive queries");
		}


		private Filter filter(final Value<?> value, final BiFunction<String, Value<?>, Filter> op, final Collection<String> inequalities) {
			if ( value .getType() == ValueType.ENTITY) {

				final String property=key(path);
				final IncompleteKey key=((EntityValue)value).get().getKey();

				return (inequalities == null || inequalities.contains(property)) && (key instanceof Key)
						? op.apply(property, KeyValue.of((Key)key))
						: null;

			} else {

				return (inequalities == null || inequalities.contains(path)) ? op.apply(path, value) : null;

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

			final Set<Value<?>> range=in.getValues().stream()
					.map(Datastore::value)
					.map(this::target)
					.collect(toSet());

			return range.isEmpty() ? null : values -> stream(values).allMatch(value ->
					range.contains(target(value))
			);
		}

		@Override public Predicate<Value<?>> probe(final Any any) {

			final Set<Value<?>> range=any.getValues().stream()
					.map(Datastore::value)
					.map(this::target)
					.collect(toSet());

			return range.size() <= 1 ? null : values -> stream(values).anyMatch(value -> // singleton handled by query
					range.contains(target(value))
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
			return Optional.of(value)
					.filter(v -> v.getType() == ValueType.ENTITY)
					.map(v -> ((EntityValue)v).get().getKey())
					.filter(k -> k instanceof Key)
					.map(k -> ((Key)k).getName())
					.orElse(value.get().toString());
		}

		private Value<?> target(final Value<?> value) {
			return Optional.of(value)
					.filter(v -> v.getType() == ValueType.ENTITY)
					.map(v -> ((EntityValue)v).get().getKey())
					.filter(k -> k instanceof Key)
					.map(k -> (Value)KeyValue.of((Key)k))
					.orElse(value);
		}

	}

}
