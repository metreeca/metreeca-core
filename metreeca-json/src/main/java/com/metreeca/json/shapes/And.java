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

import org.eclipse.rdf4j.model.Value;

import java.util.*;
import java.util.stream.Stream;

import static com.metreeca.json.Values.derives;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.Range.range;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.*;


/**
 * Conjunctive logical constraint.
 *
 * <p>States that the focus set is consistent with all shapes in a given target set.</p>
 */
public final class And extends Shape {

	private static final Shape empty=new And(emptySet());


	public static Shape and() { return empty; }

	public static Shape and(final Shape... shapes) {
		return and(asList(shapes));
	}

	public static Shape and(final Collection<? extends Shape> shapes) {
		return and(shapes.stream());
	}

	public static Shape and(final Stream<? extends Shape> shapes) {
		return pack(shapes

				// flatten nested shaped shapes of the same type

				.flatMap(new Probe<Stream<Shape>>() {

					@Override public Stream<Shape> probe(final Shape shape) { return Stream.of(shape); }

					@Override public Stream<Shape> probe(final And and) { return and.shapes().stream(); }

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
		return shapes.contains(or()) ? or() // always fail
				: shapes.size() == 1 ? shapes.iterator().next()
				: new And(shapes);
	}

	private static Stream<? extends Shape> merge(final Class<? extends Shape> clazz, final Stream<Shape> shapes) {
		return clazz.equals(Meta.class) ? Meta.metas(shapes.map(Meta.class::cast))
				: clazz.equals(Datatype.class) ? datatypes(shapes.map(Datatype.class::cast))
				: clazz.equals(Range.class) ? ranges(shapes.map(Range.class::cast))
				: clazz.equals(MinCount.class) ? minCounts(shapes.map(MinCount.class::cast))
				: clazz.equals(MaxCount.class) ? maxCounts(shapes.map(MaxCount.class::cast))
				: clazz.equals(All.class) ? alls(shapes.map(All.class::cast))
				: clazz.equals(Field.class) ? fields(shapes.map(Field.class::cast))
				: shapes;
	}


	private static Stream<? extends Shape> datatypes(final Stream<Datatype> datatypes) {

		final Collection<Datatype> space=datatypes.collect(toList());

		return space.stream().filter(upper -> space.stream()
				.filter(lower -> !upper.equals(lower))
				.noneMatch(lower -> derives(upper.id(), lower.id()))
		);
	}

	private static Stream<? extends Shape> ranges(final Stream<Range> ranges) {

		final List<Set<Value>> sets=ranges.map(Range::values).collect(toList());

		return Stream.of(range(sets // value sets intersection
				.stream()
				.flatMap(Collection::stream)
				.filter(value -> sets.stream().allMatch(set -> set.contains(value)))
				.collect(toSet())
		));
	}

	private static Stream<? extends Shape> minCounts(final Stream<MinCount> minCounts) {
		return Stream.of(minCount(minCounts.mapToInt(MinCount::limit).max().orElse(Integer.MIN_VALUE)));
	}

	private static Stream<? extends Shape> maxCounts(final Stream<MaxCount> maxCounts) {
		return Stream.of(maxCount(maxCounts.mapToInt(MaxCount::limit).min().orElse(Integer.MAX_VALUE)));
	}

	private static Stream<? extends Shape> alls(final Stream<All> alls) {
		return Stream.of(all(alls // value sets union
				.map(All::values)
				.flatMap(Collection::stream)
				.collect(toSet())
		));
	}

	private static Stream<? extends Shape> fields(final Stream<Field> fields) {
		return fields // group by name preserving order

				.collect(toMap(Field::label, f -> Stream.of(f.value()), Stream::concat, LinkedHashMap::new))

				.entrySet()
				.stream()

				.map(entry -> field(entry.getKey(), and(entry.getValue())));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Collection<Shape> shapes;


	private And(final Collection<? extends Shape> shapes) {

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
		return this == object || object instanceof And
				&& shapes.equals(((And)object).shapes);
	}

	@Override public int hashCode() {
		return shapes.hashCode();
	}

	@Override public String toString() {
		return "and("+(shapes.isEmpty() ? "" : shapes.stream()
				.map(shape -> shape.toString().replace("\n", "\n\t"))
				.collect(joining(",\n\t", "\n\t", "\n"))
		)+")";
	}

}
