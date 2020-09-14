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

import org.eclipse.rdf4j.model.IRI;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.json.Values.direct;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;


/**
 * Non-validating annotation constraint.
 *
 * <p>States that the enclosing shape has a given value for an annotation property.</p>
 */
public final class Meta extends Shape {

	private static final java.util.regex.Pattern NamedIRIPattern
			=Pattern.compile("([/#:])(?<name>[^/#:]+)(/|#|#_|#id|#this)?$");


	/**
	 * Creates an alias annotation.
	 *
	 * @param value an alternate property name for reporting values for the enclosing shape (e.g. in the context of
	 *                 JSON-based serialization results)
	 *
	 * @return a new alias annotation
	 *
	 * @throws NullPointerException if {@code value} is null
	 */
	public static Shape alias(final String value) {
		return meta("alias", value);
	}

	public static Optional<String> alias(final Shape shape) {
		return meta("alias", shape, String.class);
	}

	public static Map<IRI, String> aliases(final Shape shape) {
		if ( shape == null ) { return emptyMap(); } else {

			final Map<IRI, String> aliases=new LinkedHashMap<>();

			aliases.putAll(shape.map(new AliasesProbe(field -> { // system-guessed aliases

				final IRI name=field.label();

				return Optional
						.of(NamedIRIPattern.matcher(name.stringValue()))
						.filter(Matcher::find)
						.map(matcher -> matcher.group("name"))
						.filter(alias -> !alias.startsWith("@"))
						.map(alias -> singletonMap(name, direct(name) ? alias : alias+"Of")) // !!! inverse?
						.orElse(emptyMap());
			})));

			aliases.putAll(shape.map(new AliasesProbe(field -> { // user-provided aliases (higher precedence)

				final IRI name=field.label();

				return alias(field.value())
						.filter(alias -> !alias.startsWith("@"))
						.map(alias -> singletonMap(name, alias))
						.orElse(emptyMap());
			})));

			return aliases;
		}
	}


	/**
	 * Creates a label annotation.
	 *
	 * @param value a human-readable textual label for the enclosing shape
	 *
	 * @return a new label annotation
	 *
	 * @throws NullPointerException if {@code value} is null
	 */
	public static Shape label(final String value) {
		return new Meta("label", value);
	}

	public static Optional<String> label(final Shape shape) {
		return meta("label", shape, String.class);
	}


	/**
	 * Creates a notes annotation.
	 *
	 * @param value a human-readable textual description for the enclosing shape
	 *
	 * @return a new notes annotation
	 *
	 * @throws NullPointerException if {@code value} is null
	 */
	public static Shape notes(final String value) {
		return new Meta("notes", value);
	}

	public static Optional<String> notes(final Shape shape) {
		return meta("notes", shape, String.class);
	}


	/**
	 * Creates a placeholder annotation.
	 *
	 * @param value a human-readable textual placeholder for the expected values of the enclosing shape
	 *
	 * @return a new placeholder annotation
	 *
	 * @throws NullPointerException if {@code value} is null
	 */
	public static Shape placeholder(final String value) {
		return new Meta("placeholder", value);
	}

	public static Optional<String> placeholder(final Shape shape) {
		return meta("placeholder", shape, String.class);
	}


	/**
	 * Creates a default annotation.
	 *
	 * @param value a default value for the enclosing shape
	 *
	 * @return a new default annotation
	 *
	 * @throws NullPointerException if {@code value} is null
	 */
	public static Shape dflt(final Object value) {
		return new Meta("default", value);
	}

	public static Optional<String> dflt(final Shape shape) {
		return meta("default", shape, String.class);
	}


	/**
	 * Creates a hint annotation.
	 *
	 * @param value the identifier of a resource hinting at possible values for the enclosing shape
	 *
	 * @return a new hint annotation
	 *
	 * @throws NullPointerException if {@code value} is null
	 */
	public static Shape hint(final String value) {
		return new Meta("hint", value);
	}

	public static Optional<String> hint(final Shape shape) {
		return meta("hint", shape, String.class);
	}


	/**
	 * Creates a group annotation.
	 *
	 * @param value a client-dependent suggested group representation mode (list, form, tabbed panes, …) for the enclosing shape
	 *
	 * @return a new group annotation
	 *
	 * @throws NullPointerException if {@code value} is null
	 */
	public static Shape group(final String value) {
		return new Meta("group", value);
	}

	public static Optional<String> group(final Shape shape) {
		return meta("group", shape, String.class);
	}


	/**
	 * Creates an index annotation.
	 *
	 * @param value a  a storage indexing hint for the enclosing shape
	 *
	 * @return a new index annotation
	 *
	 * @throws NullPointerException if {@code value} is null
	 */
	public static Shape index(final boolean value) {
		return new Meta("index", value);
	}

	public static Optional<Boolean> index(final Shape shape) {
		return meta("index", shape, Boolean.class);
	}


	public static Meta meta(final Object label, final Object value) {
		return new Meta(label, value);
	}

	public static Optional<Object> meta(final Object label, final Shape shape) {
		return Optional.ofNullable(shape.map(new MetaProbe(label)));
	}

	public static <T> Optional<T> meta(final Object label, final Shape shape, final Class<T> clazz) {
		return meta(label, shape)
				.filter(clazz::isInstance)
				.map(clazz::cast);
	}


	static Stream<Meta> metas(final Stream<Meta> metas) { // make sure meta annotatios are unique

		final Map<Object, Object> mappings=new HashMap<>();

		return metas.filter(meta -> {

			final Object current=mappings.put(meta.label(), meta.value());

			if ( current != null && !current.equals(meta.value()) ) {
				throw new IllegalArgumentException(format("clashing <%s> annotations <%s> / <%s>",
						meta.label(), meta.value(), current
				));
			}

			return true;

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Object label;
	private final Object value;


	private Meta(final Object label, final Object value) {

		if ( label == null ) {
			throw new NullPointerException("null label");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		this.label=label;
		this.value=value;
	}


	public Object label() {
		return label;
	}

	public Object value() {
		return value;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <V> V map(final Probe<V> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Meta
				&& label.equals(((Meta)object).label)
				&& value.equals(((Meta)object).value);
	}

	@Override public int hashCode() {
		return label.hashCode()^value.hashCode();
	}

	@Override public String toString() {
		return "meta("+label+"="+value+")";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class MetaProbe extends Probe<Object> {

		private final Object label;


		private MetaProbe(final Object label) {
			this.label=label;
		}


		@Override public Object probe(final Meta meta) {
			return meta.label().equals(label) ? meta.value() : null;
		}


		@Override public Object probe(final And and) {
			return probe(and.shapes().stream());
		}

		@Override public Object probe(final Or or) {
			return probe(or.shapes().stream());
		}

		@Override public Object probe(final When when) {
			return probe(Stream.of(when.pass(), when.fail()));
		}


		private Object probe(final Stream<Shape> shapes) {
			return shapes.map(this).filter(Objects::nonNull).findFirst().orElse(null);
		}

	}

	private static final class AliasesProbe extends Probe<Map<IRI, String>> {

		private final Function<Field, Map<IRI, String>> aliaser;


		private AliasesProbe(final Function<Field, Map<IRI, String>> aliaser) {
			this.aliaser=aliaser;
		}


		@Override public Map<IRI, String> probe(final Shape shape) { return emptyMap(); }


		@Override public Map<IRI, String> probe(final Field field) {
			return aliaser.apply(field);
		}


		@Override public Map<IRI, String> probe(final And and) {
			return aliases(and.shapes());
		}

		@Override public Map<IRI, String> probe(final Or or) {
			return aliases(or.shapes());
		}

		@Override public Map<IRI, String> probe(final When when) {
			return aliases(asList(when.pass(), when.fail()));
		}


		private Map<IRI, String> aliases(final Collection<Shape> shapes) {
			return shapes.stream()

					// collect field-to-alias mappings from nested shapes

					.flatMap(shape -> shape.map(this).entrySet().stream())

					// remove duplicate mappings

					.distinct()

					// group by field and remove edges mapped to multiple aliases

					.collect(groupingBy(Map.Entry::getKey)).values().stream()
					.filter(group -> group.size() == 1)
					.map(group -> group.get(0))

					// group by alias and remove aliases mapped from multiple fields

					.collect(groupingBy(Map.Entry::getValue)).values().stream()
					.filter(group -> group.size() == 1)
					.map(group -> group.get(0))

					// collect non-clashing mappings

					.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
		}

	}

}
