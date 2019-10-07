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
import com.metreeca.gcp.formats.EntityFormat;
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
import static com.metreeca.gcp.services.Datastore.compare;
import static com.metreeca.gcp.services.Datastore.datastore;
import static com.metreeca.gcp.services.Datastore.value;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.services.Logger.logger;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Field.field;

import static com.google.cloud.datastore.StructuredQuery.OrderBy.Direction.ASCENDING;
import static com.google.cloud.datastore.StructuredQuery.OrderBy.Direction.DESCENDING;
import static com.google.cloud.datastore.StructuredQuery.OrderBy.asc;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
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


	private static String key(final String path) {
		return path.endsWith("."+KeyProperty) ? path : path+"."+KeyProperty;
	}


	private static String property(final List<Object> path) {  // !!! handle/reject path steps containing dots
		return path.isEmpty() ? KeyProperty : path.stream().map(Object::toString).collect(joining("."));
	}

	private static String property(final String head, final String tail) { // !!! handle/reject path steps containing dots
		return head.isEmpty() ? tail : head+"."+tail;
	}


	private static String label(final Value<?> value) {
		return Optional.of(value)
				.filter(v -> v.getType() == ValueType.ENTITY)
				.map(v -> ((EntityValue)v).get())
				.filter(e -> e.contains(GCP.label))
				.map(e -> e.getString(GCP.label))
				.orElse("");
	}

	private static Value<?> get(final Value<?> entity, final Iterable<Object> steps) {

		Value<?> value=entity;

		for (final Object step : steps) {
			if ( value.getType() == ValueType.ENTITY ) {

				final FullEntity<?> nested=((EntityValue)value).get();
				final String property=step.toString();

				value=nested.contains(property) ?
						nested.getValue(property)
						: NullValue.of();
			}
		}

		return value;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Datastore datastore=service(datastore());
	private final Logger logger=service(logger());

	private final EntityFormat format=entity(datastore);


	Future<Response> handle(final Request request) {
		return request.collection() ? holder(request) : member(request);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Future<Response> holder(final Request request) {

		return request.query(expand(digest(request.shape())), (path, shape) -> format.path(path), format::value)

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

		final Entity container=Entity.newBuilder(datastore.newKeyFactory().setKind(GCP.Resource).newKey(path))

				.set(GCP.contains, this.<List<EntityValue>>entities(shape, orders, offset, limit, entities -> entities

						.collect(toList())

				))

				.build();

		return response -> response
				.status(OK) // containers are virtual and respond always with 200 OK
				.shape(field(GCP.contains, convey(shape)))
				.body(format, container);

	}

	private Function<Response, Response> terms(final String path, final Terms terms) {

		final Comparator<Map.Entry<? extends Value<?>, Long>> byCount=comparingLong(Map.Entry::getValue);
		final Comparator<Map.Entry<? extends Value<?>, Long>> byLabel=comparing(x -> label(x.getKey()));
		final Comparator<Map.Entry<? extends Value<?>, Long>> byValue=comparing(Map.Entry::getKey, Datastore::compare);

		final Entity container=Entity.newBuilder(datastore.newKeyFactory().setKind(GCP.Resource).newKey(path))

				.set(DatastoreEngine.terms, this.<List<EntityValue>>values(terms.getShape(), terms.getPath(), values -> values

						.collect(groupingBy(v -> v, counting()))
						.entrySet()
						.stream()

						.sorted(byCount.reversed().thenComparing(byLabel.thenComparing(byValue)))

						.map(entry -> FullEntity.newBuilder()

								.set(DatastoreEngine.value, entry.getKey())
								.set(DatastoreEngine.count, entry.getValue())

								.build())

						.map(EntityValue::of)
						.collect(toList())
				))

				.build();

		return response -> response
				.status(OK)
				.shape(DatastoreEngine.TermsShape)
				.body(format, container);

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
						compare(min, range.min) <= 0 ? min : range.min,
						compare(max, range.max) >= 0 ? max : range.max
				);
			}

			private void set(final BaseEntity.Builder<?, ?> container) {

				container.set(DatastoreEngine.count, count);
				container.set(DatastoreEngine.min, min);
				container.set(DatastoreEngine.max, max);

			}

		}


		final Map<String, Range> ranges=values(stats.getShape(), stats.getPath(), values -> values

				.collect(groupingBy(v -> v.getType().toString(), reducing(null, v -> new Range(1, v, v), (x, y) ->
						x == null ? y : y == null ? x : x.merge(y)
				)))

		);

		final Entity.Builder container=Entity.newBuilder(datastore.newKeyFactory().setKind(GCP.Resource).newKey(path));

		if ( ranges.isEmpty() ) {

			container.set(DatastoreEngine.count, 0L);

		} else {

			ranges.values().stream() // global stats
					.reduce(Range::merge)
					.orElse(new Range(0, null, null)) // unexpected
					.set(container);

			container.set(DatastoreEngine.stats, ranges.entrySet().stream() // type-specific stats

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
				.shape(DatastoreEngine.StatsShape)
				.body(format, container.build());

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Future<Response> member(final Request request) {
		return request.reply(response -> datastore.exec(service -> {

			final Shape shape=convey(detail(request.shape()));

			final Key key=service.newKeyFactory()
					.setKind(clazz(shape).map(Object::toString).orElse(GCP.Resource))
					.newKey(request.path());

			// ;( projecting only properties actually included in the shape would lower costs, as projection queries
			// are counted as small operations: unfortunately, a number of limitations apply:
			// https://cloud.google.com/appengine/docs/standard/java/datastore/projectionqueries#Java_Limitations_on_projections
			// https://cloud.google.com/appengine/docs/standard/java/datastore/projectionqueries#Java_Projections_and_multiple_valued_properties

			return Optional.ofNullable(service.get(key))

					.map(entity -> response
							.status(OK)
							.shape(shape)
							.body(format, entity)
					)

					.orElseGet(() -> response
							.status(NotFound)
					);

		}));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private <R> R values(
			final Shape shape,
			final Iterable<Object> steps,
			final Function<Stream<? extends Value<?>>, R> task
	) {

		return entities(shape, null, 0, 0, entities -> task.apply(entities

				// !!! (€) if property is known to be of a scalar supported type retrieve using a projection
				// https://cloud.google.com/appengine/docs/standard/java/javadoc/com/google/appengine/api/datastore/RawValue.html#getValue--

				.map(entity -> get(entity, steps))

				.filter(v -> v.getType() != ValueType.NULL)

				.map(v -> { // retain only entity core properties

					if ( v instanceof EntityValue ) {

						final FullEntity<?> entity=((EntityValue)v).get();
						final FullEntity.Builder<?> builder=FullEntity.newBuilder(entity.getKey());

						Stream.of(GCP.id, GCP.type, GCP.label, GCP.comment).forEach(property -> {

							if (entity.contains(property)) {
								builder.set(property, (Value<?>)entity.getValue(property));
							}

						});

						return EntityValue.of(builder.build());

					} else {

						return v;

					}

				})

		));
	}

	private <R> R entities(
			final Shape shape,
			final List<Order> orders, final int offset, final int limit,
			final Function<Stream<EntityValue>, R> task
	) {

		// !!! support cursors
		// !!! hard sampling limits?
		// !!! strict index-based query generation

		final Shape constraints=filter(shape);

		final List<String> equalities=equalities(constraints);
		final List<String> inequalities=inequalities(constraints);

		// ;( native query handling requires perfect indexes
		// delegate the query subset supported by defaut indexes and handle other constraints with a post-processor

		if ( !equalities.isEmpty() || inequalities.isEmpty() ) { // prefer equality filters // !!! (€) prefer most selective option

			return query(
					constraints, equalities, emptyList(),
					constraints.map(new FilterProbe("", equalities)),
					constraints.map(new PredicateProbe("", inequalities)),
					orders, offset, limit,
					criteria -> criteria.isEmpty() || equalities.isEmpty() && criteria.size() == 1,
					task
			);

		} else {

			// ;( inequality filters are limited to at most one property
			// see https://cloud.google.com/datastore/docs/concepts/queries#restrictions_on_queries

			return query(
					constraints, equalities, inequalities,
					constraints.map(new FilterProbe("", singleton(inequalities.get(0)))),
					constraints.map(new PredicateProbe("",
							Stream.concat(inequalities.stream().skip(1), equalities.stream()).collect(toList())
					)),
					orders, offset, limit,
					criteria -> criteria.size() == 1 && property(criteria.get(0).getPath()).equals(inequalities.get(0)),
					task
			);

		}

	}

	private <R> R query(
			final Shape shape,
			final Collection<String> equalities, final Collection<String> inequalities,
			final Filter filter, final Predicate<Value<?>> predicate,
			final List<Order> orders, final int offset, final int limit,
			final Function<List<Order>, Boolean> compatible,
			final Function<Stream<EntityValue>, R> task
	) {

		final EntityQuery.Builder builder=Query.newEntityQueryBuilder()
				.setKind(clazz(shape).map(Object::toString).orElse(GCP.Resource));

		if ( filter != null ) {
			builder.setFilter(filter);
		}

		final List<Order> criteria=criteria(orders, equalities);

		final Boolean sortable=(criteria == null) ? null : predicate == null && compatible.apply(criteria);

		if ( TRUE.equals(sortable) ) {

			criteria.forEach(order -> builder.addOrderBy(new OrderBy(
					property(order.getPath()), order.isInverse() ? DESCENDING : ASCENDING
			)));

			builder.addOrderBy(asc(KeyProperty)); // force a consistent default/residual ordering

			if ( offset > 0 ) { builder.setOffset(offset); }
			if ( limit > 0 ) { builder.setLimit(limit); }

		} else if ( !inequalities.isEmpty() ) {

			builder.addOrderBy(asc(inequalities.iterator().next()));

		}

		final EntityQuery query=builder.build();

		logger.info(this, String.format("executing query %s", query));

		return datastore.exec(service -> Optional.of(stream(spliteratorUnknownSize(service.run(query), ORDERED), false))

				.map(entities -> entities.map(EntityValue::of))
				.map(entities -> predicate != null ? entities.filter(predicate) : entities)

				// handle offset/limit after postprocessing filter/sort

				.map(entities -> FALSE.equals(sortable) ? entities.sorted(comparator(criteria)) : entities)
				.map(entities -> FALSE.equals(sortable) && offset > 0 ? entities.skip(offset) : entities)
				.map(entities -> FALSE.equals(sortable) && limit > 0 ? entities.limit(limit) : entities)

				.map(task)

				.get()
		);
	}


	private List<String> equalities(final Shape shape) { // !!! (€) prefer most selective
		return shape

				.map(new EqualityProbe(""))

				.collect(toList());
	}

	private List<String> inequalities(final Shape shape) { // !!! (€) prefer most selective
		return shape

				.map(new InequalityProbe(""))

				.collect(groupingBy(identity(), counting()))
				.entrySet()
				.stream()

				.sorted(Map.Entry.<String, Long>comparingByValue().reversed()) // prefer multiple constrains

				.map(Map.Entry::getKey)
				.collect(toList());
	}


	private List<Order> criteria(final List<Order> orders, final Collection<String> equalities) {
		return (orders == null) ? null : orders.stream()

				// sort orders are ignored on properties with equality filters
				// sort on key property sensible only as last criterion and handled by default in this case
				// see https://cloud.google.com/datastore/docs/concepts/queries#restrictions_on_queries

				.filter(order -> !order.getPath().isEmpty() && !equalities.contains(property(order.getPath())))

				.collect(toList());
	}

	private Comparator<EntityValue> comparator(final Collection<Order> orders) {
		return orders.isEmpty() ? Datastore::compare : orders.stream()

				.map(order -> {

					final List<Object> path=order.getPath();
					final boolean inverse=order.isInverse();

					final Comparator<EntityValue> comparator=comparing(e -> get(e, path), Datastore::compare);

					return inverse ? comparator.reversed() : comparator;

				})

				.reduce(Comparator::thenComparing)
				.orElse(null) // unexpected

				.thenComparing(Datastore::compare); // force a consistent default/residual ordering
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private abstract static class RelatorProbe<T> extends Traverser<T> {

		@Override public T probe(final Guard guard) {
			throw new UnsupportedOperationException(guard.toString());
		}

		@Override public T probe(final When when) {
			throw new UnsupportedOperationException(when.toString());
		}


		boolean isEntity(final Object value) {
			return value instanceof FullEntity || value instanceof EntityValue;
		}

	}


	/**
	 * Identifies paths possibly subject to query equality constraints.
	 */
	private static final class EqualityProbe extends RelatorProbe<Stream<String>> {

		private final String path;


		private EqualityProbe(final String path) {
			this.path=path;
		}


		@Override public Stream<String> probe(final Shape shape) {
			return Stream.empty();
		}


		@Override public Stream<String> probe(final All all) {
			return Stream.of(all.getValues().stream().allMatch(this::isEntity) ? key(path) : path);
		}

		@Override public Stream<String> probe(final Any any) {
			return any.getValues().size() == 1
					? Stream.of(any.getValues().stream().allMatch(this::isEntity) ? key(path) : path)
					: Stream.empty();
		}


		@Override public Stream<String> probe(final Field field) {
			return field.getShape().map(new EqualityProbe(property(path, field.getName().toString())));
		}

		@Override public Stream<String> probe(final And and) {
			return and.getShapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<String> probe(final Or or) {
			return Stream.empty(); // managed by predicate
		}

	}

	/**
	 * Identifies paths possibly subject to query inequality constraints.
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
			return Stream.of(isEntity(minExclusive.getValue()) ? key(path) : path);
		}

		@Override public Stream<String> probe(final MaxExclusive maxExclusive) {
			return Stream.of(isEntity(maxExclusive.getValue()) ? key(path) : path);
		}

		@Override public Stream<String> probe(final MinInclusive minInclusive) {
			return Stream.of(isEntity(minInclusive.getValue()) ? key(path) : path);
		}

		@Override public Stream<String> probe(final MaxInclusive maxInclusive) {
			return Stream.of(isEntity(maxInclusive.getValue()) ? key(path) : path);
		}


		@Override public Stream<String> probe(final Field field) {
			return field.getShape().map(new InequalityProbe(property(path, field.getName().toString())));
		}

		@Override public Stream<String> probe(final And and) {
			return and.getShapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<String> probe(final Or or) {
			return Stream.empty(); // managed by predicate
		}

	}


	private static final class FilterProbe extends RelatorProbe<Filter> {

		private final String path;
		private final boolean target;

		private final Collection<String> targets;


		private FilterProbe(final String path, final Collection<String> targets) {

			this.path=path;
			this.target=targets == null || targets.contains(path) || targets.contains(key(path));

			this.targets=targets;
		}


		@Override public Filter probe(final MinExclusive minExclusive) {
			return target ? filter(value(minExclusive.getValue()), PropertyFilter::gt) : null;
		}

		@Override public Filter probe(final MaxExclusive maxExclusive) {
			return target ? filter(value(maxExclusive.getValue()), PropertyFilter::lt) : null;

		}

		@Override public Filter probe(final MinInclusive minInclusive) {
			return target ? filter(value(minInclusive.getValue()), PropertyFilter::ge) : null;
		}

		@Override public Filter probe(final MaxInclusive maxInclusive) {
			return target ? filter(value(maxInclusive.getValue()), PropertyFilter::le) : null;
		}


		@Override public Filter probe(final All all) {
			return target ? and(all.getValues().stream()
					.map(Datastore::value)
					.map(value -> filter(value, PropertyFilter::eq))
					.filter(Objects::nonNull)
					.collect(toList())
			) : null;
		}

		@Override public Filter probe(final Any any) {

			final Set<Object> values=any.getValues();

			return target && values.size() == 1
					? filter(value(values.iterator().next()), PropertyFilter::eq)
					: null;
		}


		@Override public Filter probe(final Field field) {
			return field.getShape().map(new FilterProbe(property(path, field.getName().toString()), targets));
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
					: null; // disjunctive constraints handled by predicate
		}


		private Filter filter(final Value<?> value, final BiFunction<String, Value<?>, Filter> op) {
			if ( value.getType() == ValueType.ENTITY ) {

				final String property=key(path);
				final IncompleteKey key=((EntityValue)value).get().getKey();

				return (key instanceof Key)
						? op.apply(property, KeyValue.of((Key)key))
						: null;

			} else {

				return op.apply(path, value);

			}
		}

	}

	private static final class PredicateProbe extends RelatorProbe<Predicate<Value<?>>> {

		private final String path;
		private final boolean target;

		private final Collection<String> targets;


		private PredicateProbe(final String path, final Collection<String> targets) {

			this.path=path;
			this.target=targets == null || targets.contains(path) || targets.contains(key(path));

			this.targets=targets;
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

			final Value<?> limit=Datastore.value(minExclusive.getValue());

			return target ? values -> stream(values).allMatch(value -> compare(value, limit) > 0) : null;
		}

		@Override public Predicate<Value<?>> probe(final MaxExclusive maxExclusive) {

			final Value<?> limit=Datastore.value(maxExclusive.getValue());

			return target ? values -> stream(values).allMatch(value -> compare(value, limit) < 0) : null;
		}

		@Override public Predicate<Value<?>> probe(final MinInclusive minInclusive) {

			final Value<?> limit=Datastore.value(minInclusive.getValue());

			return target ? values -> stream(values).allMatch(value -> compare(value, limit) >= 0) : null;
		}

		@Override public Predicate<Value<?>> probe(final MaxInclusive maxInclusive) {

			final Value<?> limit=Datastore.value(maxInclusive.getValue());

			return target ? values -> stream(values).allMatch(value -> compare(value, limit) <= 0) : null;
		}


		@Override public Predicate<Value<?>> probe(final MinLength minLength) {

			final int limit=minLength.getLimit();

			return values -> stream(values).allMatch(value -> string(value).length() >= limit);
		}

		@Override public Predicate<Value<?>> probe(final MaxLength maxLength) {

			final int limit=maxLength.getLimit();

			return values -> stream(values).allMatch(value -> string(value).length() <= limit);
		}

		@Override public Predicate<Value<?>> probe(final Pattern pattern) {

			final java.util.regex.Pattern regex=java.util.regex.Pattern.compile(
					String.format("(?%s:%s)", pattern.getFlags(), pattern.getText())
			);

			return values -> stream(values).allMatch(value -> regex.matcher(string(value)).matches());
		}

		@Override public Predicate<Value<?>> probe(final Like like) {

			final java.util.regex.Pattern regex=java.util.regex.Pattern.compile(like.toExpression());

			return values -> stream(values).allMatch(value -> regex.matcher(string(value)).find());
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
					.map(this::value)
					.collect(toSet());

			return range.isEmpty() ? null : values -> stream(values).allMatch(value -> range.contains(value(value)));
		}

		@Override public Predicate<Value<?>> probe(final All all) {

			final Set<Value<?>> range=all.getValues().stream()
					.map(Datastore::value)
					.map(this::value)
					.collect(toSet());

			return target ? values -> stream(values).allMatch(value -> range.contains(value(value))) : null;
		}

		@Override public Predicate<Value<?>> probe(final Any any) {

			final Set<Value<?>> range=any.getValues().stream()
					.map(Datastore::value)
					.map(this::value)
					.collect(toSet());

			return target || range.size() > 1  // singleton handled by query
					? values -> stream(values).anyMatch(value -> range.contains(value(value)))
					: null;
		}


		@Override public Predicate<Value<?>> probe(final Field field) {

			final String name=field.getName().toString();
			final Predicate<Value<?>> predicate=field.getShape().map(new PredicateProbe(property(path, name), targets));

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
					.map(shape -> shape.map(new PredicateProbe(path, null))) // handle all disjunctive constraints
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

		private Value<?> value(final Value<?> value) {
			return Optional.of(value)
					.filter(v -> v.getType() == ValueType.ENTITY)
					.map(v -> ((EntityValue)v).get().getKey())
					.filter(k -> k instanceof Key)
					.map(k -> (Value)KeyValue.of((Key)k))
					.orElse(value);
		}

	}

}
