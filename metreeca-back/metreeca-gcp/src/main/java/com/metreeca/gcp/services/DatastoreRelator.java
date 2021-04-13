/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.gcp.services;

final class DatastoreRelator {

	//private static final String KeyProperty="__key__";
	//
	//
	//private static String key(final String path) {
	//	return path.endsWith("."+KeyProperty) ? path : path+"."+KeyProperty;
	//}
	//
	//
	//private static String property(final List<Object> path) {  // !!! handle/reject path steps containing dots
	//	return path.isEmpty() ? KeyProperty : path.stream().map(Object::toString).collect(joining("."));
	//}
	//
	//private static String property(final String head, final String tail) { // !!! handle/reject path steps containing
	//	// dots
	//	return head.isEmpty() ? tail : head+"."+tail;
	//}
	//
	//
	//private static String label(final Value<?> value) {
	//	return Optional.of(value)
	//			.filter(v -> v.getType() == ValueType.ENTITY)
	//			.map(v -> ((EntityValue)v).get())
	//			.filter(e -> e.contains(GCP.label))
	//			.map(e -> e.getString(GCP.label))
	//			.orElse("");
	//}
	//
	//private static Value<?> get(final Value<?> entity, final Iterable<Object> steps) {
	//
	//	Value<?> value=entity;
	//
	//	for (final Object step : steps) {
	//		if ( value.getType() == ValueType.ENTITY ) {
	//
	//			final FullEntity<?> nested=((EntityValue)value).get();
	//			final String property=step.toString();
	//
	//			value=nested.contains(property) ?
	//					nested.getValue(property)
	//					: NullValue.of();
	//		}
	//	}
	//
	//	return value;
	//}
	//
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//private final Datastore datastore=service(datastore());
	//private final Logger logger=service(logger());
	//
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//private <R> R values(
	//		final Shape shape,
	//		final Iterable<Object> steps,
	//		final Function<Stream<? extends Value<?>>, R> task
	//) {
	//
	//	return entities(shape, null, 0, 0, entities -> task.apply(entities
	//
	//			// !!! (€) if property is known to be of a scalar supported type retrieve using a projection
	//			// https://cloud.google.com/appengine/docs/standard/java/javadoc/com/google/appengine/api/datastore
	//			// /RawValue.html#getValue--
	//
	//			.map(entity -> get(entity, steps))
	//
	//			.filter(v -> v.getType() != ValueType.NULL)
	//
	//			.map(v -> { // retain only entity core properties
	//
	//				if ( v instanceof EntityValue ) {
	//
	//					final FullEntity<?> entity=((EntityValue)v).get();
	//					final FullEntity.Builder<?> builder=FullEntity.newBuilder(entity.getKey());
	//
	//					Stream.of(JSONFormat.id, JSONFormat.type, GCP.label, GCP.comment).forEach(property -> {
	//
	//						if ( entity.contains(property) ) {
	//							builder.set(property, (Value<?>)entity.getValue(property));
	//						}
	//
	//					});
	//
	//					return EntityValue.of(builder.build());
	//
	//				} else {
	//
	//					return v;
	//
	//				}
	//
	//			})
	//
	//	));
	//}
	//
	//private <R> R entities(
	//		final Shape shape,
	//		final List<Order> orders, final int offset, final int limit,
	//		final Function<Stream<EntityValue>, R> task
	//) {
	//
	//	// !!! support cursors
	//	// !!! hard sampling limits?
	//	// !!! strict index-based query generation
	//
	//	final Shape constraints=filter(shape);
	//
	//	final List<String> equalities=equalities(constraints);
	//	final List<String> inequalities=inequalities(constraints);
	//
	//	// ;( native query handling requires perfect indexes
	//	// delegate the query subset supported by defaut indexes and handle other constraints with a post-processor
	//
	//	if ( !equalities.isEmpty() || inequalities.isEmpty() ) { // prefer equality filters // !!! (€) prefer most
	//		// selective option
	//
	//		return query(
	//				constraints, equalities, emptyList(),
	//				constraints.map(new FilterProbe("", equalities)),
	//				constraints.map(new PredicateProbe("", inequalities)),
	//				orders, offset, limit,
	//				criteria -> criteria.isEmpty() || equalities.isEmpty() && criteria.size() == 1,
	//				task
	//		);
	//
	//	} else {
	//
	//		// ;( inequality filters are limited to at most one property
	//		// see https://cloud.google.com/datastore/docs/concepts/queries#restrictions_on_queries
	//
	//		return query(
	//				constraints, equalities, inequalities,
	//				constraints.map(new FilterProbe("", singleton(inequalities.get(0)))),
	//				constraints.map(new PredicateProbe("",
	//						Stream.concat(inequalities.stream().skip(1), equalities.stream()).collect(toList())
	//				)),
	//				orders, offset, limit,
	//				criteria -> criteria.size() == 1 && property(criteria.get(0).getPath()).equals(inequalities.get(0)),
	//				task
	//		);
	//
	//	}
	//
	//}
	//
	//private <R> R query(
	//		final Shape shape,
	//		final Collection<String> equalities, final Collection<String> inequalities,
	//		final Filter filter, final Predicate<Value<?>> predicate,
	//		final List<Order> orders, final int offset, final int limit,
	//		final Function<List<Order>, Boolean> compatible,
	//		final Function<Stream<EntityValue>, R> task
	//) {
	//
	//	final EntityQuery.Builder builder=Query.newEntityQueryBuilder()
	//			.setKind(clazz(shape).map(Object::toString).orElse(GCP.Resource));
	//
	//	if ( filter != null ) {
	//		builder.setFilter(filter);
	//	}
	//
	//	final List<Order> criteria=criteria(orders, equalities);
	//
	//	final Boolean sortable=(criteria == null) ? null : predicate == null && compatible.apply(criteria);
	//
	//	if ( TRUE.equals(sortable) ) {
	//
	//		criteria.forEach(order -> builder.addOrderBy(new OrderBy(
	//				property(order.getPath()), order.isInverse() ? DESCENDING : ASCENDING
	//		)));
	//
	//		builder.addOrderBy(asc(KeyProperty)); // force a consistent default/residual ordering
	//
	//		if ( offset > 0 ) { builder.setOffset(offset); }
	//		if ( limit > 0 ) { builder.setLimit(limit); }
	//
	//	} else if ( !inequalities.isEmpty() ) {
	//
	//		builder.addOrderBy(asc(inequalities.iterator().next()));
	//
	//	}
	//
	//	final EntityQuery query=builder.build();
	//
	//	logger.info(this, String.format("executing query %s", query));
	//
	//	return datastore.exec(service -> Optional.of(stream(spliteratorUnknownSize(service.run(query), ORDERED), false))
	//
	//			.map(entities -> entities.map(EntityValue::of))
	//			.map(entities -> predicate != null ? entities.filter(predicate) : entities)
	//
	//			// handle offset/limit after postprocessing filter/sort
	//
	//			.map(entities -> FALSE.equals(sortable) ? entities.sorted(comparator(criteria)) : entities)
	//			.map(entities -> FALSE.equals(sortable) && offset > 0 ? entities.skip(offset) : entities)
	//			.map(entities -> FALSE.equals(sortable) && limit > 0 ? entities.limit(limit) : entities)
	//
	//			.map(task)
	//
	//			.get()
	//	);
	//}
	//
	//
	//private List<String> equalities(final Shape shape) { // !!! (€) prefer most selective
	//	return shape
	//
	//			.map(new EqualityProbe(""))
	//
	//			.collect(toList());
	//}
	//
	//private List<String> inequalities(final Shape shape) { // !!! (€) prefer most selective
	//	return shape
	//
	//			.map(new InequalityProbe(""))
	//
	//			.collect(groupingBy(identity(), counting()))
	//			.entrySet()
	//			.stream()
	//
	//			.sorted(Map.Entry.<String, Long>comparingByValue().reversed()) // prefer multiple constrains
	//
	//			.map(Map.Entry::getKey)
	//			.collect(toList());
	//}
	//
	//
	//private List<Order> criteria(final List<Order> orders, final Collection<String> equalities) {
	//	return (orders == null) ? null : orders.stream()
	//
	//			// sort orders are ignored on properties with equality filters
	//			// sort on key property sensible only as last criterion and handled by default in this case
	//			// see https://cloud.google.com/datastore/docs/concepts/queries#restrictions_on_queries
	//
	//			.filter(order -> !order.getPath().isEmpty() && !equalities.contains(property(order.getPath())))
	//
	//			.collect(toList());
	//}
	//
	//private Comparator<EntityValue> comparator(final Collection<Order> orders) {
	//	return orders.isEmpty() ? Datastore::compare : orders.stream()
	//
	//			.map(order -> {
	//
	//				final List<Object> path=order.getPath();
	//				final boolean inverse=order.isInverse();
	//
	//				final Comparator<EntityValue> comparator=comparing(e -> get(e, path), Datastore::compare);
	//
	//				return inverse ? comparator.reversed() : comparator;
	//
	//			})
	//
	//			.reduce(Comparator::thenComparing)
	//			.orElse(null) // unexpected
	//
	//			.thenComparing(Datastore::compare); // force a consistent default/residual ordering
	//}
	//
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//private abstract static class RelatorProbe<T> extends Traverser<T> {
	//
	//	@Override public T probe(final Guard guard) {
	//		throw new UnsupportedOperationException(guard.toString());
	//	}
	//
	//	@Override public T probe(final When when) {
	//		throw new UnsupportedOperationException(when.toString());
	//	}
	//
	//
	//	boolean isEntity(final Object value) {
	//		return value instanceof FullEntity || value instanceof EntityValue;
	//	}
	//
	//}
	//
	//
	///**
	// * Identifies paths possibly subject to query equality constraints.
	// */
	//private static final class EqualityProbe extends RelatorProbe<Stream<String>> {
	//
	//	private final String path;
	//
	//
	//	private EqualityProbe(final String path) {
	//		this.path=path;
	//	}
	//
	//
	//	@Override public Stream<String> probe(final Shape shape) {
	//		return Stream.empty();
	//	}
	//
	//
	//	@Override public Stream<String> probe(final All all) {
	//		return Stream.of(all.values().stream().allMatch(this::isEntity) ? key(path) : path);
	//	}
	//
	//	@Override public Stream<String> probe(final Any any) {
	//		return any.values().size() == 1
	//				? Stream.of(any.values().stream().allMatch(this::isEntity) ? key(path) : path)
	//				: Stream.empty();
	//	}
	//
	//
	//	@Override public Stream<String> probe(final Field field) {
	//		return field.shape().map(new EqualityProbe(property(path, field.iri().toString())));
	//	}
	//
	//	@Override public Stream<String> probe(final And and) {
	//		return and.shapes().stream().flatMap(shape -> shape.map(this));
	//	}
	//
	//	@Override public Stream<String> probe(final Or or) {
	//		return Stream.empty(); // managed by predicate
	//	}
	//
	//}
	//
	///**
	// * Identifies paths possibly subject to query inequality constraints.
	// */
	//private static final class InequalityProbe extends RelatorProbe<Stream<String>> {
	//
	//	private final String path;
	//
	//
	//	private InequalityProbe(final String path) {
	//		this.path=path;
	//	}
	//
	//
	//	@Override public Stream<String> probe(final Shape shape) {
	//		return Stream.empty();
	//	}
	//
	//
	//	@Override public Stream<String> probe(final MinExclusive minExclusive) {
	//		return Stream.of(isEntity(minExclusive.limit()) ? key(path) : path);
	//	}
	//
	//	@Override public Stream<String> probe(final MaxExclusive maxExclusive) {
	//		return Stream.of(isEntity(maxExclusive.limit()) ? key(path) : path);
	//	}
	//
	//	@Override public Stream<String> probe(final MinInclusive minInclusive) {
	//		return Stream.of(isEntity(minInclusive.limit()) ? key(path) : path);
	//	}
	//
	//	@Override public Stream<String> probe(final MaxInclusive maxInclusive) {
	//		return Stream.of(isEntity(maxInclusive.limit()) ? key(path) : path);
	//	}
	//
	//
	//	@Override public Stream<String> probe(final Field field) {
	//		return field.shape().map(new InequalityProbe(property(path, field.iri().toString())));
	//	}
	//
	//	@Override public Stream<String> probe(final And and) {
	//		return and.shapes().stream().flatMap(shape -> shape.map(this));
	//	}
	//
	//	@Override public Stream<String> probe(final Or or) {
	//		return Stream.empty(); // managed by predicate
	//	}
	//
	//}
	//
	//
	//private static final class FilterProbe extends RelatorProbe<Filter> {
	//
	//	private final String path;
	//	private final boolean target;
	//
	//	private final Collection<String> targets;
	//
	//
	//	private FilterProbe(final String path, final Collection<String> targets) {
	//
	//		this.path=path;
	//		this.target=targets == null || targets.contains(path) || targets.contains(key(path));
	//
	//		this.targets=targets;
	//	}
	//
	//
	//	@Override public Filter probe(final MinExclusive minExclusive) {
	//		return target ? filter(value(minExclusive.limit()), PropertyFilter::gt) : null;
	//	}
	//
	//	@Override public Filter probe(final MaxExclusive maxExclusive) {
	//		return target ? filter(value(maxExclusive.limit()), PropertyFilter::lt) : null;
	//
	//	}
	//
	//	@Override public Filter probe(final MinInclusive minInclusive) {
	//		return target ? filter(value(minInclusive.limit()), PropertyFilter::ge) : null;
	//	}
	//
	//	@Override public Filter probe(final MaxInclusive maxInclusive) {
	//		return target ? filter(value(maxInclusive.limit()), PropertyFilter::le) : null;
	//	}
	//
	//
	//	@Override public Filter probe(final All all) {
	//		return target ? and(all.values().stream()
	//				.map(EntityFormat::value)
	//				.map(value -> filter(value, PropertyFilter::eq))
	//				.filter(Objects::nonNull)
	//				.collect(toList())
	//		) : null;
	//	}
	//
	//	@Override public Filter probe(final Any any) {
	//
	//		final Set<Object> values=any.values();
	//
	//		return target && values.size() == 1
	//				? filter(value(values.iterator().next()), PropertyFilter::eq)
	//				: null;
	//	}
	//
	//
	//	@Override public Filter probe(final Field field) {
	//		return field.shape().map(new FilterProbe(property(path, field.iri().toString()), targets));
	//	}
	//
	//
	//	@Override public Filter probe(final And and) {
	//		return and(and.shapes().stream()
	//				.map(shape -> shape.map(this))
	//				.filter(Objects::nonNull)
	//				.collect(toList())
	//		);
	//	}
	//
	//	@Override public Filter probe(final Or or) {
	//		return or(or.shapes().stream()
	//				.map(shape -> shape.map(this))
	//				.filter(Objects::nonNull)
	//				.collect(toList())
	//		);
	//	}
	//
	//
	//	private Filter and(final List<Filter> filters) {
	//		return filters.isEmpty() ? null
	//				: filters.size() == 1 ? filters.get(0)
	//				: StructuredQuery.CompositeFilter.and( // ;( no collection constructor
	//				filters.get(0), filters.subList(1, filters.size()).toArray(new Filter[filters.size()-1])
	//		);
	//	}
	//
	//	private Filter or(final List<Filter> filters) {
	//		return filters.isEmpty() ? null
	//				: filters.size() == 1 ? filters.get(0)
	//				: null; // disjunctive constraints handled by predicate
	//	}
	//
	//
	//	private Filter filter(final Value<?> value, final BiFunction<String, Value<?>, Filter> op) {
	//		if ( value.getType() == ValueType.ENTITY ) {
	//
	//			final String property=key(path);
	//			final IncompleteKey key=((EntityValue)value).get().getKey();
	//
	//			return (key instanceof Key)
	//					? op.apply(property, KeyValue.of((Key)key))
	//					: null;
	//
	//		} else {
	//
	//			return op.apply(path, value);
	//
	//		}
	//	}
	//
	//}
	//
	//private static final class PredicateProbe extends RelatorProbe<Predicate<Value<?>>> {
	//
	//	private final String path;
	//	private final boolean target;
	//
	//	private final Collection<String> targets;
	//
	//
	//	private PredicateProbe(final String path, final Collection<String> targets) {
	//
	//		this.path=path;
	//		this.target=targets == null || targets.contains(path) || targets.contains(key(path));
	//
	//		this.targets=targets;
	//	}
	//
	//
	//	@Override public Predicate<Value<?>> probe(final Datatype datatype) {
	//
	//		final String name=datatype.iri().toString();
	//
	//		return values -> stream(values).allMatch(value ->
	//				name.equals(value.getType().toString())
	//		);
	//	}
	//
	//	@Override public Predicate<Value<?>> probe(final Clazz clazz) { // top level handled by Query
	//
	//		final String name=clazz.iri().toString();
	//
	//		return path.isEmpty() ? null : values -> stream(values).allMatch(value ->
	//				Optional.of(value)
	//						.filter(v -> v.getType() == ValueType.ENTITY)
	//						.map(v -> (FullEntity<?>)v.get())
	//						.map(BaseEntity::getKey)
	//						.filter(Objects::nonNull)
	//						.map(BaseKey::getKind)
	//						.filter(kind -> kind.equals(name))
	//						.isPresent()
	//
	//		);
	//	}
	//
	//	@Override public Predicate<Value<?>> probe(final Range in) {
	//
	//		final Set<Value<?>> range=in.values().stream()
	//				.map(EntityFormat::value)
	//				.map(this::value)
	//				.collect(toSet());
	//
	//		return range.isEmpty() ? null : values -> stream(values).allMatch(value -> range.contains(value(value)));
	//	}
	//
	//
	//	@Override public Predicate<Value<?>> probe(final MinExclusive minExclusive) {
	//
	//		final Value<?> limit=EntityFormat.value(minExclusive.limit());
	//
	//		return target ? values -> stream(values).allMatch(value -> compare(value, limit) > 0) : null;
	//	}
	//
	//	@Override public Predicate<Value<?>> probe(final MaxExclusive maxExclusive) {
	//
	//		final Value<?> limit=EntityFormat.value(maxExclusive.limit());
	//
	//		return target ? values -> stream(values).allMatch(value -> compare(value, limit) < 0) : null;
	//	}
	//
	//	@Override public Predicate<Value<?>> probe(final MinInclusive minInclusive) {
	//
	//		final Value<?> limit=EntityFormat.value(minInclusive.limit());
	//
	//		return target ? values -> stream(values).allMatch(value -> compare(value, limit) >= 0) : null;
	//	}
	//
	//	@Override public Predicate<Value<?>> probe(final MaxInclusive maxInclusive) {
	//
	//		final Value<?> limit=EntityFormat.value(maxInclusive.limit());
	//
	//		return target ? values -> stream(values).allMatch(value -> compare(value, limit) <= 0) : null;
	//	}
	//
	//
	//	@Override public Predicate<Value<?>> probe(final MinLength minLength) {
	//
	//		final int limit=minLength.limit();
	//
	//		return values -> stream(values).allMatch(value -> string(value).length() >= limit);
	//	}
	//
	//	@Override public Predicate<Value<?>> probe(final MaxLength maxLength) {
	//
	//		final int limit=maxLength.limit();
	//
	//		return values -> stream(values).allMatch(value -> string(value).length() <= limit);
	//	}
	//
	//	@Override public Predicate<Value<?>> probe(final Pattern pattern) {
	//
	//		final java.util.regex.Pattern regex=java.util.regex.Pattern.compile(
	//				String.format("(?%s:%s)", pattern.flags(), pattern.expression())
	//		);
	//
	//		return values -> stream(values).allMatch(value -> regex.matcher(string(value)).matches());
	//	}
	//
	//	@Override public Predicate<Value<?>> probe(final Like like) {
	//
	//		final java.util.regex.Pattern regex=java.util.regex.Pattern.compile(like.toExpression());
	//
	//		return values -> stream(values).allMatch(value -> regex.matcher(string(value)).find());
	//	}
	//
	//
	//	@Override public Predicate<Value<?>> probe(final MinCount minCount) {
	//
	//		final int limit=minCount.limit();
	//
	//		return values -> count(values) >= limit;
	//	}
	//
	//	@Override public Predicate<Value<?>> probe(final MaxCount maxCount) {
	//
	//		final int limit=maxCount.limit();
	//
	//		return values -> count(values) <= limit;
	//	}
	//
	//	@Override public Predicate<Value<?>> probe(final All all) {
	//
	//		final Set<Value<?>> range=all.values().stream()
	//				.map(EntityFormat::value)
	//				.map(this::value)
	//				.collect(toSet());
	//
	//		return target ? values -> stream(values).allMatch(value -> range.contains(value(value))) : null;
	//	}
	//
	//	@Override public Predicate<Value<?>> probe(final Any any) {
	//
	//		final Set<Value<?>> range=any.values().stream()
	//				.map(EntityFormat::value)
	//				.map(this::value)
	//				.collect(toSet());
	//
	//		return target || range.size() > 1  // singleton handled by query
	//				? values -> stream(values).anyMatch(value -> range.contains(value(value)))
	//				: null;
	//	}
	//
	//
	//	@Override public Predicate<Value<?>> probe(final Field field) {
	//
	//		final String name=field.iri().toString();
	//		final Predicate<Value<?>> predicate=field.shape().map(new PredicateProbe(property(path, name), targets));
	//
	//		return predicate == null ? null : values -> stream(values).allMatch(value ->
	//				value.getType() == ValueType.ENTITY
	//						&& ((EntityValue)value).get().contains(name)
	//						&& predicate.test(((EntityValue)value).get().getValue(name))
	//		);
	//	}
	//
	//
	//	@Override public Predicate<Value<?>> probe(final And and) {
	//
	//		final List<Predicate<Value<?>>> predicates=and.shapes().stream()
	//				.map(shape -> shape.map(this))
	//				.filter(Objects::nonNull)
	//				.collect(toList());
	//
	//		return predicates.isEmpty() ? null
	//				: predicates.size() == 1 ? predicates.get(0)
	//				: values -> predicates.stream().allMatch(predicate -> predicate.test(values));
	//	}
	//
	//	@Override public Predicate<Value<?>> probe(final Or or) {
	//
	//		final List<Predicate<Value<?>>> predicates=or.shapes().stream()
	//				.map(shape -> shape.map(new PredicateProbe(path, null))) // handle all disjunctive constraints
	//				.filter(Objects::nonNull)
	//				.collect(toList());
	//
	//		return predicates.isEmpty() ? null
	//				: predicates.size() == 1 ? predicates.get(0)
	//				: values -> predicates.stream().anyMatch(predicate -> predicate.test(values));
	//	}
	//
	//
	//	private int count(final Value<?> value) {
	//		return value.getType() == ValueType.NULL ? 0
	//				: value.getType() == ValueType.LIST ? ((ListValue)value).get().size()
	//				: 1;
	//	}
	//
	//	private Stream<? extends Value<?>> stream(final Value<?> value) {
	//		return value.getType() == ValueType.NULL ? Stream.empty()
	//				: value.getType() == ValueType.LIST ? ((ListValue)value).get().stream()
	//				: Stream.of(value);
	//	}
	//
	//
	//	private String string(final Value<?> value) {
	//		return Optional.of(value)
	//				.filter(v -> v.getType() == ValueType.ENTITY)
	//				.map(v -> ((EntityValue)v).get().getKey())
	//				.filter(k -> k instanceof Key)
	//				.map(k -> ((Key)k).getName())
	//				.orElse(value.get().toString());
	//	}
	//
	//	private Value<?> value(final Value<?> value) {
	//		return Optional.of(value)
	//				.filter(v -> v.getType() == ValueType.ENTITY)
	//				.map(v -> ((EntityValue)v).get().getKey())
	//				.filter(k -> k instanceof Key)
	//				.map(k -> (Value)KeyValue.of((Key)k))
	//				.orElse(value);
	//	}
	//
	//}

}
