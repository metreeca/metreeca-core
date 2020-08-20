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
import java.util.Map.Entry;
import java.util.function.*;
import java.util.stream.*;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;


/**
 * Extended stream.
 *
 * @param <T> the type of the extended stream elements
 */
public final class Xtream<T> implements Stream<T> {

	/**
	 * Creates an empty sequential extended stream.
	 *
	 * @param <T> the type of stream elements
	 *
	 * @return an empty sequential extended stream
	 */
	public static <T> Xtream<T> empty() {
		return from(Stream.empty());
	}


	/**
	 * Creates a sequential extended stream containing a single element.
	 *
	 * @param element the single element to be included in the new extended stream
	 * @param <T>     the type of stream elements
	 *
	 * @return a singleton sequential extended stream
	 */
	public static <T> Xtream<T> of(final T element) {
		return from(Stream.of(element));
	}

	/**
	 * Creates a sequential ordered extended stream containing the specified elements.
	 *
	 * @param <T>      the type of stream elements
	 * @param elements the elements to be included in the new extended stream
	 *
	 * @return the new extended stream
	 *
	 * @throws NullPointerException if {@code elements} is null
	 */
	@SafeVarargs public static <T> Xtream<T> of(final T... elements) {

		if ( elements == null ) {
			throw new NullPointerException("null elements");
		}

		return from(Stream.of(elements));
	}


	/**
	 * Creates a sequential ordered extended stream containing the elements of the specified collection.
	 *
	 * @param <T>        the type of stream elements
	 * @param collection the collection whose elements are to be included in the extended stream
	 *
	 * @return the new extended stream
	 *
	 * @throws NullPointerException if {@code collection} is null
	 */
	public static <T> Xtream<T> from(final Collection<T> collection) {

		if ( collection == null ) {
			throw new NullPointerException("null collection");
		}

		return from(collection.stream());
	}

	/**
	 * Creates a sequential ordered extended stream containing the elements of the specified collections.
	 *
	 * @param <T>         the type of stream elements
	 * @param collections the collections whose elements are to be included in the extended stream
	 *
	 * @return the new extended stream
	 *
	 * @throws NullPointerException if {@code collections} is null or contains null elements
	 */
	@SafeVarargs public static <T> Xtream<T> from(final Collection<T>... collections) {

		if ( collections == null || Arrays.stream(collections).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null collections");
		}

		return from(Arrays.stream(collections).flatMap(Collection::stream));
	}


	/**
	 * Creates a extended stream containing the elements of the specified stream.
	 *
	 * @param <T>    the type of stream elements
	 * @param stream the stream whose elements are to be included in the extended stream
	 *
	 * @return the new extended stream
	 *
	 * @throws NullPointerException if {@code stream} is null
	 */
	public static <T> Xtream<T> from(final Stream<T> stream) {

		if ( stream == null ) {
			throw new NullPointerException("null stream");
		}

		return stream instanceof Xtream ? (Xtream<T>)stream : new Xtream<>(stream);
	}

	/**
	 * Creates a extended stream containing the elements of the specified streams.
	 *
	 * @param <T>     the type of stream elements
	 * @param streams the streams whose elements are to be included in the extended stream
	 *
	 * @return the new extended stream
	 *
	 * @throws NullPointerException if {@code stream} is null or contains null elements
	 */
	@SafeVarargs public static <T> Xtream<T> from(final Stream<T>... streams) {

		if ( streams == null || Arrays.stream(streams).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null streams");
		}

		return from(Arrays.stream(streams).flatMap(identity()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Stream<T> stream; // the delegate plain stream


	private Xtream(final Stream<T> stream) {
		this.stream=stream;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Maps elements to optional values.
	 *
	 * @param mapper a function mapping elements to optional values
	 * @param <R>    the type of the optional value returned by {@code mapper}
	 *
	 * @return an extended stream  produced by applying {@code mapper} to each element of this extended stream and
	 * replacing it with the value of non-null and non-empty optionals
	 *
	 * @throws NullPointerException if {@code mapper} is null
	 */
	public <R> Xtream<R> optMap(final Function<? super T, Optional<R>> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return from(stream.map(mapper)
				.filter(Objects::nonNull)
				.filter(Optional::isPresent)
				.map(Optional::get)
		);
	}

	/**
	 * Maps elements to collection of values.
	 *
	 * @param mapper a function mapping elements to collection of values
	 * @param <R>    the type of the value in the collection returned by {@code mapper}
	 *
	 * @return an extended stream produced by applying {@code mapper} to each element of this extended stream and
	 * replacing it with the values of non-null collections
	 *
	 * @throws NullPointerException if {@code mapper} is null
	 */
	public <R> Xtream<R> bagMap(final Function<? super T, ? extends Collection<R>> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return from(stream.map(mapper)
				.filter(Objects::nonNull)
				.flatMap(Collection::stream)
		);
	}


	@SafeVarargs public final <R> Xtream<R> bagMap(final Function<? super T, ? extends Collection<R>>... mappers) {

		if ( mappers == null || Arrays.stream(mappers).anyMatch(Objects::isNull)) {
			throw new NullPointerException("null mapper");
		}

		return flatMap(t -> Arrays.stream(mappers)
				.map(mapper -> mapper.apply(t))
				.filter(Objects::nonNull)
				.flatMap(Collection::stream)
		);

	}

	@SafeVarargs public final <R> Xtream<R> flatMap(final Function<? super T, ? extends Stream<? extends R>>... mappers) {

		if ( mappers == null || Arrays.stream(mappers).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null mappers");
		}

		return flatMap(t -> Arrays.stream(mappers)
				.flatMap(mapper -> mapper.apply(t))
		);
	}


	/**
	 * Removes incompatible elements.
	 *
	 * @param clash a binary predicate returning {@code true} if its arguments are mutually incompatible or {@code
	 *              false} otherwise
	 *
	 * @return an extended stream produced by removing from this extended stream all elements not compatible with
	 * previously processed elements according to {@code clash}
	 *
	 * @throws NullPointerException if {@code clash} is null
	 */
	public Xtream<T> prune(final BiPredicate<T, T> clash) {

		if ( clash == null ) {
			throw new NullPointerException("null clash");
		}

		return filter(new Predicate<T>() {

			private final Collection<T> matches=new ArrayList<>();

			@Override public boolean test(final T x) {
				synchronized ( matches ) {
					return matches.stream().noneMatch(y -> clash.test(x, y)) && matches.add(x);
				}
			}

		});
	}


	/**
	 * Groups elements.
	 *
	 * @param classifier a function mapping elements to a grouping key
	 * @param <K>        the type of the key returned by {@code classifier}
	 *
	 * @return an extended stream produced by applying {@code classifier} to each element of this extended stream and
	 * returning a stream of entries mapping each grouping key returned by {@code classifier} to the list of the
	 * elements matching the grouping key
	 *
	 * @throws NullPointerException if {@code classifier} is null
	 */
	public <K> Xtream<Entry<K, List<T>>> groupBy(final Function<T, K> classifier) {

		if ( classifier == null ) {
			throw new NullPointerException("null classifier");
		}

		return from(collect(groupingBy(classifier)).entrySet().stream());
	}

	/**
	 * Groups and collects elements.
	 *
	 * @param classifier a function mapping elements to their grouping key
	 * @param downstream a collector for sub-streams of this extended stream
	 * @param <K>        the type of the key returned by {@code classifier}
	 * @param <V>        the type of the value returned by the {@code downstream} collector
	 *
	 * @return an extended stream produced by applying {@code classifier} to each element of this extended stream and
	 * returning a stream of entries mapping each grouping key returned by {@code classifier} to the value produced
	 * by collection the stream of the elements matching the grouping key using the {@code downstream} collector
	 *
	 * @throws NullPointerException if either {@code classifier} or {@code downstream} is null
	 */
	public <K, V> Xtream<Entry<K, V>> groupBy(final Function<T, K> classifier, final Collector<T, ?, V> downstream) {

		if ( classifier == null ) {
			throw new NullPointerException("null classifier");
		}

		if ( downstream == null ) {
			throw new NullPointerException("null downstream collector");
		}

		return from(collect(groupingBy(classifier, downstream)).entrySet().stream());
	}


	/**
	 * Batches elements.
	 *
	 * @param size the batch size limit (0 for no limits)
	 *
	 * @return an extended stream produced by collecting the elements of this extended stream in batches of at most
	 * {@code size} elements if {@code size} is greater than 0 or in a single batch otherwise
	 *
	 * @throws IllegalArgumentException if {@code size} is negative
	 */
	public Xtream<Collection<T>> batch(final int size) {

		if ( size < 0 ) {
			throw new IllegalArgumentException("negative batch size");
		}

		return size == 0 ? of(stream.collect(toList()))
				: from(StreamSupport.stream(new BatchSpliterator<>(size, stream.spliterator()), stream.isParallel()));
	}

	/**
	 * Batches elements.
	 *
	 * @param collector a stream collector
	 * @param <C>       the type of the collected element
	 *
	 * @return an extended stream containing a single element produced by collecting the elements of this extended
	 * stream using {@code collector}
	 *
	 * @throws NullPointerException if {@code collector} is null
	 */
	public <C> Xtream<C> batch(final Collector<T, ?, C> collector) {

		if ( collector == null ) {
			throw new NullPointerException("null collector");
		}

		return of(collect(collector));
	}


	/**
	 * Recursively expands this extended stream.
	 *
	 * @param mapper a function mapping elements to streams of elements of the same type
	 *
	 * @return an extended stream produced by recursively applying {@code mapper} to this extended stream and
	 * expanding it with the elements of the returned streams until no new elements are generated; null returned
	 * streams are considered to be empty
	 *
	 * @throws NullPointerException if {@code mapper} is {@code null}
	 */
	public Xtream<T> loop(final Function<? super T, ? extends Stream<T>> mapper) { // !!! lazy

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		final Collection<T> visited=new LinkedHashSet<>(); // preserve order

		for (

				Set<T> pending=stream
						.collect(toCollection(LinkedHashSet::new));

				!pending.isEmpty();

				pending=pending
						.stream()
						.flatMap(mapper)
						.filter(value -> !visited.contains(value))
						.collect(toCollection(LinkedHashSet::new))

		) {

			visited.addAll(pending);

		}

		return from(visited.stream());
	}

	/**
	 * Iteratively expands this extended stream.
	 *
	 * @param steps  the number of expansion steps to be performed
	 * @param mapper a function mapping elements to streams of elements of the same type
	 *
	 * @return an extended stream produced by iteratively applying {@code mapper} this this extended stream and
	 * replacing it with with the elements of the returned streams until {@code steps} cycles are performed; null
	 * returned streams are considered to be empty
	 *
	 * @throws IllegalArgumentException if {@code steps} is negative
	 * @throws NullPointerException     if {@code mapper} is {@code null}
	 */
	public Xtream<T> iter(final int steps, final Function<? super T, ? extends Stream<T>> mapper) { // !!! lazy

		if ( steps < 0 ) {
			throw new IllegalArgumentException("negative steps count");
		}

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		Xtream<T> iter=this;

		for (int n=0; n < steps; ++n) {
			iter=iter.flatMap(mapper);
		}

		return iter;
	}


	/**
	 * Processes this extended stream.
	 *
	 * @param mapper a function mapping from/to extended streams
	 * @param <R>    the type of the elements of the extended stream returned by {@code mapper}
	 *
	 * @return an extended stream produced by applying {@code mapper} to this extended stream or an empty extended
	 * stream if {@code mapper} returns a null value
	 *
	 * @throws NullPointerException if {@code mapper} is {@code null}
	 */
	public <R> Xtream<R> pipe(final Function<? super Xtream<T>, ? extends Stream<R>> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return Optional
				.of(this)
				.map(mapper)
				.map(Xtream::from)
				.orElseGet(Xtream::empty);
	}

	/**
	 * Processes this extended stream.
	 *
	 * @param consumer a consumer of extended streams of this type
	 *
	 * @throws NullPointerException if {@code consumer} is {@code null}
	 */
	public void sink(final Consumer<? super Xtream<T>> consumer) {

		if ( consumer == null ) {
			throw new NullPointerException("null consumer");
		}

		consumer.accept(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Xtream<T> filter(final Predicate<? super T> predicate) { return from(stream.filter(predicate)); }

	@Override public <R> Xtream<R> map(final Function<? super T, ? extends R> mapper) { return from(stream.map(mapper)); }

	@Override public IntStream mapToInt(final ToIntFunction<? super T> mapper) { return stream.mapToInt(mapper); }

	@Override public LongStream mapToLong(final ToLongFunction<? super T> mapper) { return stream.mapToLong(mapper); }

	@Override public DoubleStream mapToDouble(final ToDoubleFunction<? super T> mapper) { return stream.mapToDouble(mapper); }

	@Override public <R> Xtream<R> flatMap(final Function<? super T, ? extends Stream<? extends R>> mapper) { return from(stream.flatMap(mapper)); }

	@Override public IntStream flatMapToInt(final Function<? super T, ? extends IntStream> mapper) { return stream.flatMapToInt(mapper); }

	@Override public LongStream flatMapToLong(final Function<? super T, ? extends LongStream> mapper) { return stream.flatMapToLong(mapper); }

	@Override public DoubleStream flatMapToDouble(final Function<? super T, ? extends DoubleStream> mapper) { return stream.flatMapToDouble(mapper); }

	@Override public Xtream<T> distinct() { return from(stream.distinct()); }

	@Override public Xtream<T> sorted() { return from(stream.sorted()); }

	@Override public Xtream<T> sorted(final Comparator<? super T> comparator) { return from(stream.sorted(comparator)); }

	@Override public Xtream<T> peek(final Consumer<? super T> action) { return from(stream.peek(action)); }

	@Override public Xtream<T> limit(final long maxSize) { return from(stream.limit(maxSize)); }

	@Override public Xtream<T> skip(final long n) { return from(stream.skip(n)); }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void forEach(final Consumer<? super T> action) { stream.forEach(action); }

	@Override public void forEachOrdered(final Consumer<? super T> action) { stream.forEachOrdered(action); }

	@Override public Object[] toArray() { return stream.toArray(); }

	@Override public <A> A[] toArray(final IntFunction<A[]> generator) { return stream.toArray(generator); }

	@Override public T reduce(final T identity, final BinaryOperator<T> accumulator) {
		return stream.reduce(identity, accumulator);
	}

	@Override public Optional<T> reduce(final BinaryOperator<T> accumulator) { return stream.reduce(accumulator); }

	@Override public <U> U reduce(
			final U identity, final BiFunction<U, ? super T, U> accumulator, final BinaryOperator<U> combiner) { return stream.reduce(identity, accumulator, combiner); }

	@Override public <R> R collect(
			final Supplier<R> supplier, final BiConsumer<R, ? super T> accumulator, final BiConsumer<R, R> combiner) { return stream.collect(supplier, accumulator, combiner); }

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

	@Override public Xtream<T> sequential() { return from(stream.sequential()); }

	@Override public Xtream<T> parallel() { return from(stream.parallel()); }

	@Override public Xtream<T> unordered() { return from(stream.unordered()); }

	@Override public Xtream<T> onClose(final Runnable closeHandler) { return from(stream.onClose(closeHandler)); }

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
			return base.estimateSize() <= size ? null : Optional
					.ofNullable(base.trySplit())
					.map(spliterator -> new BatchSpliterator<>(size, spliterator))
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
