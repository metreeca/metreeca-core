/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.spec;


import com.metreeca.spec.shifts.Step;

import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.metreeca.jeep.Maps.entry;
import static com.metreeca.jeep.Maps.map;
import static com.metreeca.jeep.Strings.indent;
import static com.metreeca.spec.Values.format;

import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.*;


/**
 * Value property map.
 *
 * @param <T>
 */
public final class Frame<T> {

	@SafeVarargs public static <T> Frame<T> frame(final Value value, final Map.Entry<Step, T>... slots) {
		return frame(value, map(slots));
	}

	public static <T> Frame<T> frame(final Value value, final Map<Step, T> slots) {
		return new Frame<>(value, slots);
	}

	public static <T> Map.Entry<Step, T> slot(final Step step, final T value) {
		return entry(step, value);
	}


	/**
	 * Merges a collection of frames.
	 *
	 * @param frames    the frames to be merged
	 * @param collector a collector transforming a stream of values into a merged value (usually a {@linkplain
	 *                  Collectors#reducing} collector)
	 * @param <T>       the  type  the frame properties
	 *
	 * @return a merged collection of frames where each frame value appears only once
	 */
	public static <T> Collection<Frame<T>> frames(final Collection<Frame<T>> frames, final Collector<T, ?, T> collector) {

		if ( frames == null ) {
			throw new NullPointerException("null frames");
		}

		if ( collector == null ) {
			throw new NullPointerException("null collector");
		}

		// slot maps merge operator

		final BinaryOperator<Map<Step, T>> operator=(x, y) -> Stream.of(x, y)
				.flatMap(slot -> slot.entrySet().stream())
				.collect(groupingBy(Map.Entry::getKey, LinkedHashMap::new,
						mapping(Map.Entry::getValue, collector)));

		// group slot maps by frame value and merge

		final Map<Value, Map<Step, T>> map=frames.stream().collect(
				groupingBy(Frame::getValue, LinkedHashMap::new,
						mapping(Frame::getSlots, reducing(map(), operator))));

		// convert back to frames

		return map.entrySet().stream()
				.map(e -> frame(e.getKey(), e.getValue()))
				.collect(toList());
	}


	private final Value value;
	private final Map<Step, T> slots;


	public Frame(final Value value, final Map<Step, T> slots) {

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		if ( slots == null ) {
			throw new NullPointerException("null slots");
		}

		if ( slots.containsKey(null) ) {
			throw new NullPointerException("null slot key");
		}

		if ( slots.containsValue(null) ) {
			throw new NullPointerException("null slot value");
		}

		this.value=value;
		this.slots=new LinkedHashMap<>(slots);
	}


	public Value getValue() {
		return value;
	}

	public Map<Step, T> getSlots() {
		return unmodifiableMap(slots);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Frame
				&& value.equals(((Frame<?>)object).value)
				&& slots.equals(((Frame<?>)object).slots);
	}

	@Override public int hashCode() {
		return value.hashCode()^slots.hashCode();
	}

	@Override public String toString() {
		return format(value)+" {"+(slots.isEmpty() ? "" : indent(slots.entrySet().stream().map(e -> {

			final String edge=e.getKey().toString();
			final String value=e.getValue().toString();

			return edge+" :\n\n"+indent(value);

		}).collect(joining("\n\n", "\n\n", "\n\n"))))+"}";
	}

}
