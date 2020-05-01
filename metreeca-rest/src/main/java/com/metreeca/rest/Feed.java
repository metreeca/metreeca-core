/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.rest;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;


/**
 * Data processing feed.
 *
 * <p>Handles a sequence of data items to be processed by a pipeline.</p>
 *
 * @param <T> the type of the item included in the feed
 */
public final class Feed<T> implements Stream<T> {

	public static <T> Feed<T> of(final T item) {
		return from(Stream.of(item));
	}

	@SafeVarargs public static <T> Feed<T> of(final T... items) {

		if ( items == null ) {
			throw new NullPointerException("null items");
		}

		return from(Stream.of(items));
	}


	public static <T> Feed<T> from(final Collection<T> collection) {

		if ( collection == null ) {
			throw new NullPointerException("null collection");
		}

		return from(collection.stream());
	}

	@SafeVarargs public static <T> Feed<T> from(final Collection<T>... collections) {

		if ( collections == null ) {
			throw new NullPointerException("null collections");
		}

		return from(Arrays.stream(collections).flatMap(Collection::stream));
	}


	public static <T> Feed<T> from(final Stream<T> stream) {

		if ( stream == null ) {
			throw new NullPointerException("null stream");
		}

		return stream instanceof Feed ? (Feed<T>)stream : new Feed<>(stream);
	}

	@SafeVarargs public static <T> Feed<T> from(final Stream<T>... streams) {

		if ( streams == null ) {
			throw new NullPointerException("null streams");
		}

		return from(Arrays.stream(streams).flatMap(identity()));
	}


	public static <V, R> Function<V, R> guard(final Function<V, R> function) {

		if ( function == null ) {
			throw new NullPointerException("null function");
		}

		return v -> {
			try {

				return v == null ? null : function.apply(v);

			} catch ( final RuntimeException e ) {

				return null;

			}
		};

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Stream<T> stream;


	private Feed(final Stream<T> stream) {
		this.stream=stream;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	public <R> Feed<R> optMap(final Function<? super T, Optional<R>> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return from(stream.map(mapper).filter(Optional::isPresent).map(Optional::get));
	}

	public <R> Feed<R> bagMap(final Function<? super T, ? extends Collection<R>> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return from(stream.map(mapper).flatMap(Collection::stream));
	}


	public <K> Feed<Map.Entry<K, List<T>>> groupBy(final Function<T, K> classifier) {

		if ( classifier == null ) {
			throw new NullPointerException("null classifier");
		}

		return from(collect(groupingBy(classifier)).entrySet().stream());
	}

	public <K, V> Feed<Map.Entry<K, V>> groupBy(final Function<T, K> classifier, final Collector<T, ?, V> downstream) {

		if ( classifier == null ) {
			throw new NullPointerException("null classifier");
		}

		if ( downstream == null ) {
			throw new NullPointerException("null downstream collector");
		}

		return from(collect(groupingBy(classifier, downstream)).entrySet().stream());
	}


	public Feed<Collection<T>> batch(final int size) {

		if ( size < 0 ) {
			throw new IllegalArgumentException("negative batch size");
		}

		return size == 0 ? of(stream.collect(toList()))
				: from(StreamSupport.stream(new BatchSpliterator<>(size, stream.spliterator()), stream.isParallel()));
	}


	public Feed<T> loop(final Function<? super Feed<T>, ? extends Stream<T>> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		final Collection<T> loop=new LinkedHashSet<>(); // preserve order

		for (

				Set<T> pending=stream.parallel()
						.collect(toCollection(LinkedHashSet::new));

				!pending.isEmpty();

				pending=mapper.apply(from(pending.stream()))
						.filter(value -> !loop.contains(value))
						.collect(toCollection(LinkedHashSet::new))

		) {

			loop.addAll(pending);

		}

		return from(loop.stream());
	}

	public Feed<T> iter(final int steps, final Function<? super Feed<T>, ? extends Stream<T>> mapper) {

		if ( steps < 0 ) {
			throw new IllegalArgumentException("negative steps count");
		}

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		Feed<T> iter=this;

		for (int n=0; n < steps; ++n) {
			iter=from(mapper.apply(iter.parallel()));
		}

		return iter;
	}


	public <R> Feed<R> pipe(final Function<? super Feed<T>, ? extends Stream<R>> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return from(mapper.apply(this));
	}

	public void sink(final Consumer<? super Feed<T>> consumer) {

		if ( consumer == null ) {
			throw new NullPointerException("null consumer");
		}

		consumer.accept(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Feed<T> filter(final Predicate<? super T> predicate) { return from(stream.filter(predicate)); }

	@Override public <R> Feed<R> map(final Function<? super T, ? extends R> mapper) { return from(stream.map(mapper)); }

	@Override public IntStream mapToInt(final ToIntFunction<? super T> mapper) { return stream.mapToInt(mapper); }

	@Override public LongStream mapToLong(final ToLongFunction<? super T> mapper) { return stream.mapToLong(mapper); }

	@Override public DoubleStream mapToDouble(final ToDoubleFunction<? super T> mapper) { return stream.mapToDouble(mapper); }

	@Override public <R> Feed<R> flatMap(final Function<? super T, ? extends Stream<? extends R>> mapper) { return from(stream.flatMap(mapper)); }

	@Override public IntStream flatMapToInt(final Function<? super T, ? extends IntStream> mapper) { return stream.flatMapToInt(mapper); }

	@Override public LongStream flatMapToLong(final Function<? super T, ? extends LongStream> mapper) { return stream.flatMapToLong(mapper); }

	@Override public DoubleStream flatMapToDouble(final Function<? super T, ? extends DoubleStream> mapper) { return stream.flatMapToDouble(mapper); }

	@Override public Feed<T> distinct() { return from(stream.distinct()); }

	@Override public Feed<T> sorted() { return from(stream.sorted()); }

	@Override public Feed<T> sorted(final Comparator<? super T> comparator) { return from(stream.sorted(comparator)); }

	@Override public Feed<T> peek(final Consumer<? super T> action) { return from(stream.peek(action)); }

	@Override public Feed<T> limit(final long maxSize) { return from(stream.limit(maxSize)); }

	@Override public Feed<T> skip(final long n) { return from(stream.skip(n)); }

	@Override public void forEach(final Consumer<? super T> action) { stream.forEach(action); }

	@Override public void forEachOrdered(final Consumer<? super T> action) { stream.forEachOrdered(action); }

	@Override public Object[] toArray() { return stream.toArray(); }

	@Override public <A> A[] toArray(final IntFunction<A[]> generator) { return stream.toArray(generator); }

	@Override public T reduce(final T identity, final BinaryOperator<T> accumulator) {
		return stream.reduce(identity,
				accumulator);
	}

	@Override public Optional<T> reduce(final BinaryOperator<T> accumulator) { return stream.reduce(accumulator); }

	@Override public <U> U reduce(final U identity, final BiFunction<U, ? super T, U> accumulator,
			final BinaryOperator<U> combiner) { return stream.reduce(identity, accumulator, combiner); }

	@Override public <R> R collect(final Supplier<R> supplier, final BiConsumer<R, ? super T> accumulator,
			final BiConsumer<R, R> combiner) { return stream.collect(supplier, accumulator, combiner); }

	@Override public <R, A> R collect(final Collector<? super T, A, R> collector) { return stream.collect(collector); }

	@Override public Optional<T> min(final Comparator<? super T> comparator) { return stream.min(comparator); }

	@Override public Optional<T> max(final Comparator<? super T> comparator) { return stream.max(comparator); }

	@Override public long count() { return stream.count(); }

	@Override public boolean anyMatch(final Predicate<? super T> predicate) { return stream.anyMatch(predicate); }

	@Override public boolean allMatch(final Predicate<? super T> predicate) { return stream.allMatch(predicate); }

	@Override public boolean noneMatch(final Predicate<? super T> predicate) { return stream.noneMatch(predicate); }

	@Override public Optional<T> findFirst() { return stream.findFirst(); }

	@Override public Optional<T> findAny() { return stream.findAny(); }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Iterator<T> iterator() { return stream.iterator(); }

	@Override public Spliterator<T> spliterator() { return stream.spliterator(); }

	@Override public boolean isParallel() { return stream.isParallel(); }

	@Override public Feed<T> sequential() { return from(stream.sequential()); }

	@Override public Feed<T> parallel() { return from(stream.parallel()); }

	@Override public Feed<T> unordered() { return from(stream.unordered()); }

	@Override public Feed<T> onClose(final Runnable closeHandler) { return from(stream.onClose(closeHandler)); }

	@Override public void close() { stream.close(); }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class BatchSpliterator<T> implements Spliterator<Collection<T>> {

		private final int size;
		private final Spliterator<T> base;


		private BatchSpliterator(final int size, final Spliterator<T> base) {
			this.size=size;
			this.base=base;
		}


		@Override public boolean tryAdvance(final Consumer<? super Collection<T>> action) {

			final Collection<T> batch=new ArrayList<>(size);

			for (int n=0; n < size && base.tryAdvance(batch::add); ++n) {}

			if ( batch.isEmpty() ) {

				return false;

			} else {

				action.accept(batch);

				return true;

			}

		}

		@Override public Spliterator<Collection<T>> trySplit() {
			return base.estimateSize() <= size ? null : Optional.ofNullable(base.trySplit())
					.map(spliterator -> new BatchSpliterator<>(size, base.trySplit()))
					.orElse(null);
		}

		@Override public long estimateSize() {
			return (base.estimateSize()+size-1)/size;
		}

		@Override public int characteristics() {
			return base.characteristics();
		}

	}

}
