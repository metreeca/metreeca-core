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

package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

import java.util.*;
import java.util.stream.Stream;

import static com.metreeca.json.Values.derives;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Any.any;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.Meta.metas;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.Range.range;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.*;


/**
 * Disjunctive logical constraint.
 *
 * <p>States that the focus set is consistent with at least one shape in a given target set.</p>
 */
public final class Or extends Shape {

	private static final Shape empty=new Or(Collections.emptySet());


	public static Shape or() {
		return empty;
	}

	public static Shape or(final Shape... shapes) {
		return or(asList(shapes));
	}

	public static Shape or(final Collection<? extends Shape> shapes) {
		return or(shapes.stream());
	}

	public static Shape or(final Stream<? extends Shape> shapes) {
		return pack(shapes

				// flatten nested shaped shapes of the same type

				.flatMap(new Shape.Probe<Stream<Shape>>() {

					@Override public Stream<Shape> probe(final Shape shape) { return Stream.of(shape); }

					@Override public Stream<Shape> probe(final Or or) { return or.shapes().stream(); }

				})

				// group by shape type preserving order

				.collect(groupingBy(Shape::getClass, LinkedHashMap::new, toCollection(LinkedHashSet::new)))

				.entrySet()
				.stream()

				// merge shapes sets

				.flatMap(entry -> merge(entry.getKey(), entry.getValue().stream()))

				.collect(toList())
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static Shape pack(final List<? extends Shape> shapes) {
		return shapes.contains(and()) ? and() // always pass
				: shapes.size() == 1 ? shapes.iterator().next()
				: new Or(shapes);
	}

	private static Stream<? extends Shape> merge(final Class<? extends Shape> clazz, final Stream<Shape> shapes) {
		return clazz.equals(Meta.class) ? metas(shapes.map(Meta.class::cast))
				: clazz.equals(Datatype.class) ? datatypes(shapes.map(Datatype.class::cast))
				: clazz.equals(Range.class) ? ranges(shapes.map(Range.class::cast))
				: clazz.equals(MinCount.class) ? minCounts(shapes.map(MinCount.class::cast))
				: clazz.equals(MaxCount.class) ? maxCounts(shapes.map(MaxCount.class::cast))
				: clazz.equals(Any.class) ? anys(shapes.map(Any.class::cast))
				: clazz.equals(Field.class) ? fields(shapes.map(Field.class::cast))
				: shapes;
	}


	private static Stream<? extends Shape> datatypes(final Stream<Datatype> datatypes) {

		final Collection<Datatype> space=datatypes.collect(toList());

		return space.stream().filter(lower -> space.stream()
				.filter(upper -> !upper.equals(lower))
				.noneMatch(upper -> derives(upper.id(), lower.id()))
		);
	}

	private static Stream<? extends Shape> ranges(final Stream<Range> ranges) {
		return Stream.of(range(ranges // value sets union
				.map(Range::values)
				.flatMap(Collection::stream)
				.collect(toSet())
		));
	}

	private static Stream<? extends Shape> minCounts(final Stream<MinCount> minCounts) {
		return Stream.of(minCount(minCounts.mapToInt(MinCount::limit).min().orElse(Integer.MIN_VALUE)));
	}

	private static Stream<? extends Shape> maxCounts(final Stream<MaxCount> maxCounts) {
		return Stream.of(maxCount(maxCounts.mapToInt(MaxCount::limit).max().orElse(Integer.MAX_VALUE)));
	}

	private static Stream<? extends Shape> anys(final Stream<Any> anys) {
		return Stream.of(any(anys // value sets union
				.map(Any::values)
				.flatMap(Collection::stream)
				.collect(toSet())
		));
	}

	private static Stream<? extends Shape> fields(final Stream<Field> fields) {
		return fields // group by name preserving order

				.collect(toMap(Field::label, f -> Stream.of(f.value()), Stream::concat, LinkedHashMap::new))

				.entrySet()
				.stream()

				.map(entry -> field(entry.getKey(), or(entry.getValue())));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Collection<Shape> shapes;


	private Or(final Collection<? extends Shape> shapes) {

		if ( shapes == null ) {
			throw new NullPointerException("null shapes");
		}

		if ( shapes.contains(null) ) {
			throw new NullPointerException("null shape");
		}

		this.shapes=new LinkedHashSet<>(shapes);
	}


	public Collection<Shape> shapes() {
		return Collections.unmodifiableCollection(shapes);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Or
				&& shapes.equals(((Or)object).shapes);
	}

	@Override public int hashCode() {
		return shapes.hashCode();
	}

	@Override public String toString() {
		return "or("+(shapes.isEmpty() ? "" : shapes.stream()
				.map(shape -> shape.toString().replace("\n", "\n\t"))
				.collect(joining(",\n\t", "\n\t", "\n"))
		)+")";
	}

}
