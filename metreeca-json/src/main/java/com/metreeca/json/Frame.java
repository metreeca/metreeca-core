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

package com.metreeca.json;

import com.metreeca.json.shifts.Path;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.metreeca.json.Values.*;
import static com.metreeca.json.shifts.Alt.alt;
import static com.metreeca.json.shifts.Seq.seq;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;

/**
 * Linked data frame.
 *
 * <p>Describes a linked data graph centered on a focus value.</p>
 */
public final class Frame {

	public static Frame frame(final Value focus, final Collection<Statement> model) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( model == null || model.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null model or model statement");
		}

		return frame(focus, model, value -> false);
	}

	private static Frame frame(final Value focus, final Collection<Statement> model, final Predicate<Value> visited) {
		return visited.test(focus) ? frame(focus) : new Frame(focus, Stream.<Map.Entry<IRI, Value>>concat(

				model.stream()
						.filter(pattern(focus, null, null))
						.map(s -> new SimpleImmutableEntry<>(s.getPredicate(), s.getObject())),

				model.stream()
						.filter(pattern(null, null, focus))
						.filter(s -> !visited.test(s.getSubject()))
						.map(s -> new SimpleImmutableEntry<>(inverse(s.getPredicate()), s.getSubject()))

		).collect(groupingBy(Map.Entry::getKey, collectingAndThen(

				mapping(entry -> frame(entry.getValue(), model, visited.or(focus::equals)), toSet()),
				Collections::unmodifiableSet

		))));
	}


	public static Stream<Statement> model(final Frame frame) {

		if ( frame == null ) {
			throw new NullPointerException("null frame");
		}

		return frame.traits().entrySet().stream().flatMap(trait -> {

			final IRI predicate=trait.getKey();
			final Collection<Frame> frames=trait.getValue();

			return frames.stream().flatMap(_frame -> {

				final Statement statement=traverse(predicate,
						direct -> statement((Resource)frame.focus, direct, _frame.focus),
						inverse -> statement((Resource)_frame.focus, inverse, frame.focus)
				);

				return Stream.concat(Stream.of(statement), model(_frame));

			});

		});
	}

	public Frame objects(final IRI predicate, final Stream<Object> objects) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( objects == null ) {
			throw new NullPointerException("null objects");
		}

		return frames(predicate, objects.map(object -> object instanceof Frame ? (Frame)object : frame((Value)object)));
	}


	//// !!! ///////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final Path Labels=alt(
			RDFS.LABEL, DC.TITLE, iri("http://schema.org/", "name")
	);

	private static final Path Notes=alt(
			RDFS.COMMENT, DC.DESCRIPTION, iri("http://schema.org/", "description")
	);


	public static Frame frame(final Value focus) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		return new Frame(focus, emptyMap());
	}

	public static Frame frame(final Value focus, final Map<IRI, ? extends Collection<Frame>> traits) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		return new Frame(focus, traits.entrySet().stream().collect(toMap(
				Map.Entry::getKey, e -> unmodifiableSet(new LinkedHashSet<>(e.getValue()))
		)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Value focus;
	private final Map<IRI, Collection<Frame>> traits;

	private Frame(final Value focus, final Map<IRI, ? extends Collection<Frame>> traits) {
		this.focus=focus;
		this.traits=unmodifiableMap(traits);
	}


	public boolean empty() {
		return traits.isEmpty();
	}

	public int size() {
		return traits.size()+traits.values().stream()
				.flatMap(Collection::stream)
				.mapToInt(Frame::size)
				.sum();
	}


	public Optional<String> label() {
		return string(Labels);
	}

	public Optional<String> notes() {
		return string(Notes);
	}


	/**
	 * Retrieves the frame focus.
	 *
	 * @return the frame focus value.
	 */
	public Value focus() {
		return focus;
	}

	public Map<IRI, Collection<Frame>> traits() {
		return traits;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Optional<Boolean> bool(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return bool(seq(predicate));
	}

	public Optional<Boolean> bool(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return value(shift).flatMap(Values::bool);
	}


	public Frame bool(final IRI predicate, final Boolean bool) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return bool == null ? this : value(predicate, Values.literal(bool));
	}

	public Frame bool(final IRI predicate, final Optional<Boolean> bool) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( bool == null ) {
			throw new NullPointerException("null bool");
		}

		return bool.map(object -> value(predicate, Values.literal(object))).orElse(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Optional<BigInteger> integer(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return integer(seq(predicate));
	}

	public Optional<BigInteger> integer(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return value(shift).flatMap(Values::integer);
	}


	public Stream<BigInteger> integers(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return integers(seq(predicate));
	}

	public Stream<BigInteger> integers(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return values(shift).map(Values::integer).filter(Optional::isPresent).map(Optional::get);
	}


	public Frame integer(final IRI predicate, final Number integer) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return integer == null ? this : integers(predicate, Stream.of(integer));
	}

	public Frame integer(final IRI predicate, final Optional<Number> integer) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( integer == null ) {
			throw new NullPointerException("null integer");
		}

		return integer.map(object -> integers(predicate, Stream.of(object))).orElse(this);
	}


	public Frame integers(final IRI predicate, final Number... integers) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( integers == null ) {
			throw new NullPointerException("null integers");
		}

		return integers.length == 0 ? this : integers(predicate, Arrays.stream(integers));
	}

	public Frame integers(final IRI predicate, final Collection<Number> integers) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( integers == null ) {
			throw new NullPointerException("null integers");
		}

		return integers.isEmpty() ? this : integers(predicate, integers.stream());
	}

	public Frame integers(final IRI predicate, final Stream<Number> integers) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( integers == null ) {
			throw new NullPointerException("null integers");
		}

		return values(predicate, integers.map(value

				-> value instanceof BigInteger ? (BigInteger)value
				: value instanceof BigDecimal ? ((BigDecimal)value).toBigInteger()
				: BigInteger.valueOf(value.longValue())

		).map(Values::literal));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Optional<BigDecimal> decimal(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return decimal(seq(predicate));
	}

	public Optional<BigDecimal> decimal(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return value(shift).flatMap(Values::decimal);
	}


	public Stream<BigDecimal> decimals(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return decimals(seq(predicate));
	}

	public Stream<BigDecimal> decimals(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return values(shift).map(Values::decimal).filter(Optional::isPresent).map(Optional::get);
	}


	public Frame decimal(final IRI predicate, final Number decimal) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return decimal == null ? this : decimals(predicate, Stream.of(decimal));
	}

	public Frame decimal(final IRI predicate, final Optional<Number> decimal) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( decimal == null ) {
			throw new NullPointerException("null decimal");
		}

		return decimal.map(object -> decimals(predicate, Stream.of(object))).orElse(this);
	}


	public Frame decimals(final IRI predicate, final Number... decimals) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( decimals == null ) {
			throw new NullPointerException("null decimals");
		}

		return decimals.length == 0 ? this : decimals(predicate, Arrays.stream(decimals));
	}

	public Frame decimals(final IRI predicate, final Collection<Number> decimals) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( decimals == null ) {
			throw new NullPointerException("null decimals");
		}

		return decimals.isEmpty() ? this : decimals(predicate, decimals.stream());
	}

	public Frame decimals(final IRI predicate, final Stream<Number> decimals) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( decimals == null ) {
			throw new NullPointerException("null decimals");
		}


		return values(predicate, decimals.map(value

				-> value instanceof BigDecimal ? (BigDecimal)value
				: value instanceof BigInteger ? new BigDecimal((BigInteger)value)
				: BigDecimal.valueOf(value.doubleValue())

		).map(Values::literal));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Optional<String> string(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return string(seq(predicate));
	}

	public Optional<String> string(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return value(shift).map(Value::stringValue);
	}


	public Stream<String> strings(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return strings(seq(predicate));
	}

	public Stream<String> strings(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return values(shift).map(Value::stringValue);
	}


	public Frame string(final IRI predicate, final String string) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return string == null ? this : strings(predicate, Stream.of(string));
	}

	public Frame string(final IRI predicate, final Optional<String> string) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( string == null ) {
			throw new NullPointerException("null string");
		}

		return string.map(object -> strings(predicate, Stream.of(object))).orElse(this);
	}


	public Frame strings(final IRI predicate, final String... strings) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( strings == null ) {
			throw new NullPointerException("null strings");
		}

		return strings.length == 0 ? this : strings(predicate, Arrays.stream(strings));
	}

	public Frame strings(final IRI predicate, final Collection<String> strings) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( strings == null ) {
			throw new NullPointerException("null strings");
		}

		return strings.isEmpty() ? this : strings(predicate, strings.stream());
	}

	public Frame strings(final IRI predicate, final Stream<String> strings) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( strings == null ) {
			throw new NullPointerException("null strings");
		}

		return values(predicate, strings.map(Values::literal));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Optional<Literal> literal(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return literal(seq(predicate));
	}

	public Optional<Literal> literal(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return literals(shift).findFirst();
	}


	public Stream<Literal> literals(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return literals(seq(predicate));
	}

	public Stream<Literal> literals(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return values(shift)
				.filter(Value::isLiteral)
				.map(Literal.class::cast);
	}


	public Frame literal(final IRI predicate, final Literal literal) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return literal == null ? this : literals(predicate, Stream.of(literal));
	}

	public Frame literal(final IRI predicate, final Optional<? extends Literal> literal) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( literal == null ) {
			throw new NullPointerException("null literal");
		}

		return literal.map(object -> literals(predicate, Stream.of(object))).orElse(this);
	}


	public Frame literals(final IRI predicate, final Literal... literals) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( literals == null ) {
			throw new NullPointerException("null literals");
		}

		return literals.length == 0 ? this : literals(predicate, Arrays.stream(literals));
	}

	public Frame literals(final IRI predicate, final Collection<? extends Literal> literals) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( literals == null ) {
			throw new NullPointerException("null literals");
		}

		return literals.isEmpty() ? this : literals(predicate, literals.stream());
	}

	public Frame literals(final IRI predicate, final Stream<? extends Literal> literals) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( literals == null ) {
			throw new NullPointerException("null literals");
		}

		return values(predicate, literals);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Optional<Value> value(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return values(predicate).findFirst();
	}

	public Optional<Value> value(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return values(shift).findFirst();
	}


	public Stream<Value> values(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return frames(predicate).map(frame -> frame.focus);
	}

	public Stream<Value> values(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return frames(shift).map(frame -> frame.focus);
	}


	public Frame value(final IRI predicate, final Value value) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return value == null ? this : values(predicate, Stream.of(value));
	}

	public Frame value(final IRI predicate, final Optional<? extends Value> value) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return value.map(object -> values(predicate, Stream.of(object))).orElse(this);
	}


	public Frame values(final IRI predicate, final Value... values) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		return values.length == 0 ? this : values(predicate, Arrays.stream(values));
	}

	public Frame values(final IRI predicate, final Collection<? extends Value> values) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		return values.isEmpty() ? this : values(predicate, values.stream());
	}

	public Frame values(final IRI predicate, final Stream<? extends Value> values) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		return frames(predicate, values.map(value -> new Frame(value, emptyMap())));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Optional<Frame> frame(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return frame(seq(predicate));
	}

	public Optional<Frame> frame(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return frames(shift).findFirst();
	}


	public Stream<Frame> frames(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return traits.getOrDefault(predicate, emptySet()).stream();
	}

	public Stream<Frame> frames(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return shift.map(new ShiftEvaluator(this));
	}


	public Frame frame(final IRI predicate, final Frame frame) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return frame == null ? this : frames(predicate, Stream.of(frame));
	}

	public Frame frame(final IRI predicate, final Optional<Frame> frame) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( frame == null ) {
			throw new NullPointerException("null frame");
		}

		return frame.map(object -> frames(predicate, Stream.of(object))).orElse(this);
	}


	public Frame frames(final IRI predicate, final Frame... frames) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( frames == null ) {
			throw new NullPointerException("null frames");
		}

		return frames.length == 0 ? this : frames(predicate, Arrays.stream(frames));
	}

	public Frame frames(final IRI predicate, final Collection<Frame> frames) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( frames == null ) {
			throw new NullPointerException("null frames");
		}

		return frames.isEmpty() ? this : frames(predicate, frames.stream());
	}

	public Frame frames(final IRI predicate, final Stream<Frame> frames) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( frames == null ) {
			throw new NullPointerException("null frames");
		}

		if ( !focus.isResource() && direct(predicate) ) {
			throw new IllegalArgumentException(String.format(
					"direct predicate %s with focus %s", Values.format(predicate), Values.format(focus)
			));
		}

		final Collection<Frame> merged=unmodifiableSet(new LinkedHashSet<>(index(Stream.concat(

				traits.getOrDefault(predicate, emptySet()).stream(),

				frames.peek(frame -> {

					if ( !frame.focus.isResource() && !direct(predicate) ) {
						throw new IllegalArgumentException(String.format(
								"inverse predicate %s with value %s", Values.format(predicate), Values.format(focus)
						));
					}

				})

		)).values()));

		if ( merged.isEmpty() ) { return this; } else {

			final Map<IRI, Collection<Frame>> extended=new LinkedHashMap<>(traits);

			extended.put(predicate, merged);

			return new Frame(focus, extended);
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Map<Value, Frame> index(final Stream<Frame> frames) {
		return frames.collect(groupingBy(Frame::focus, LinkedHashMap::new, reducing(null, (x, y) ->
				x == null ? y : y == null ? x : new Frame(x.focus, merge(x.traits, y.traits))
		)));
	}

	private Map<IRI, Collection<Frame>> merge(
			final Map<IRI, Collection<Frame>> x, final Map<IRI, Collection<Frame>> y
	) {

		final Map<IRI, Collection<Frame>> merged=new LinkedHashMap<>(x);

		y.forEach((predicate, frames) -> merged.compute(predicate, (key, value) ->
				value == null ? frames : merge(frames, value)
		));

		return merged;
	}

	private Collection<Frame> merge(final Collection<Frame> x, final Collection<Frame> y) {

		final Map<Value, Frame> merged=index(x.stream());

		y.forEach(frame -> merged.compute(focus, (key, value) ->
				value == null ? frame : new Frame(frame.focus, merge(value.traits, frame.traits))
		));

		return unmodifiableSet(new LinkedHashSet<>(merged.values()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String format() { // !!! test/review

		final StringBuilder builder=new StringBuilder(Values.format(focus));

		label().ifPresent(label -> builder.append(" : ").append(label));
		notes().ifPresent(notes -> builder.append(" / ").append(notes));

		if ( !traits.isEmpty() ) {
			builder.append(traits.entrySet().stream()
					.map(this::format)
					.map(Values::indent)
					.collect(joining(",\n\t", "{\n\t", "\n}"))
			);
		}

		return builder.toString();
	}

	private String format(final Map.Entry<IRI, Collection<Frame>> trait) {
		return Values.format(trait.getKey())+": "+format(trait.getValue());
	}

	private String format(final Collection<Frame> values) {
		return values.stream()
				.map(Frame::format)
				.map(Values::indent)
				.collect(joining(",\n\t", "[\n\t", "\n]"));
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Frame
				&& focus.equals(((Frame)object).focus)
				&& traits.equals(((Frame)object).traits);
	}

	@Override public int hashCode() {
		return focus.hashCode()
				^traits.hashCode();
	}

	@Override public String toString() {
		return format();
		//return Values.format(focus)
		//		+label().map(l -> " : "+l).orElse("")
		//		+notes().map(l -> " / "+l).orElse("")
		//		+(traits.isEmpty() ? "" : String.format(" { [%d] }", size()));
	}

}
