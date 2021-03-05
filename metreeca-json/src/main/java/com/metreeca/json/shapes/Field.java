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

package com.metreeca.json.shapes;

import com.metreeca.json.Frame;
import com.metreeca.json.Shape;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.json.Values.indent;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Or.or;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;


/**
 * Field structural constraint.
 *
 * <p>States that the derived focus set generated by following a single step path is consistent with a given {@link
 * Shape shape}.</p>
 */
public final class Field extends Shape {

	private static final java.util.regex.Pattern AliasPattern=Pattern.compile(
			"\\w+"
	);

	private static final Pattern NamedIRIPattern=Pattern.compile(
			"([/#:])(?<name>"+AliasPattern+"[^/#:]+)(/|#|#_|#id|#this)?$"
	);


	public static Shape field(final IRI iri, final Shape... shapes) {

		if ( iri == null ) {
			throw new NullPointerException("null iri");
		}

		if ( shapes == null || Arrays.stream(shapes).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null shapes");
		}

		return field("", iri, and(shapes));
	}

	public static Shape field(final IRI iri, final Object... values) {

		if ( iri == null ) {
			throw new NullPointerException("null iri");
		}

		if ( values == null || Arrays.stream(values).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null values");
		}

		return field("", iri, all(values));
	}


	public static Shape field(final String alias, final IRI iri, final Shape... shapes) {

		if ( alias == null ) {
			throw new NullPointerException("null alias");
		}

		if ( !(alias.isEmpty() || AliasPattern.matcher(alias).matches()) ) {
			throw new IllegalArgumentException(format("malformed alias <%s>", alias));
		}

		if ( iri == null ) {
			throw new NullPointerException("null iri");
		}

		if ( shapes == null || Arrays.stream(shapes).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null shapes");
		}

		return field(alias, iri, and(shapes));
	}

	public static Shape field(final String alias, final IRI iri, final Object... values) {

		if ( alias == null ) {
			throw new NullPointerException("null alias");
		}

		if ( !(alias.isEmpty() || AliasPattern.matcher(alias).matches()) ) {
			throw new IllegalArgumentException(format("malformed alias <%s>", alias));
		}

		if ( iri == null ) {
			throw new NullPointerException("null iri");
		}

		if ( values == null || Arrays.stream(values).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null values");
		}

		return field(alias, iri, all(values));
	}


	private static Shape field(final String alias, final IRI iri, final Shape shape) {
		return shape.equals(or()) ? and() : new Field(alias, iri, shape);
	}


	public static Optional<Field> field(final Shape shape, final IRI iri) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( iri == null ) {
			throw new NullPointerException("null iri");
		}

		return fields(shape)

				.filter(field -> field.iri().equals(iri))

				.findFirst();
	}

	public static Stream<Field> fields(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return shape.map(new FieldsProbe());
	}


	public static Map<String, Field> aliases(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return aliases(shape, emptyMap());
	}

	public static Map<String, Field> aliases(final Shape shape, final Map<String, String> keywords) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( keywords == null ) {
			throw new NullPointerException("null keywords");
		}


		return fields(shape).collect(toMap(

				field -> {

					final IRI iri=field.iri();
					final boolean direct=Frame.direct(iri);

					if ( direct && iri.equals(RDF.TYPE) ) {

						return keywords.getOrDefault("@type", "@type");

					} else {

						final String alias=Optional.of(field.alias()).filter(s -> !s.isEmpty()).orElseGet(() -> Optional

								.of(NamedIRIPattern.matcher(iri.stringValue()))
								.filter(Matcher::find)
								.map(matcher -> matcher.group("name"))
								.map(label -> direct ? label : label+"Of")

								.orElseThrow(() ->
										new IllegalArgumentException(format("undefined alias for %s", iri))
								)

						);

						if ( keywords.containsValue(alias) ) {
							throw new IllegalArgumentException(format("reserved alias <%s> for %s", alias, iri));
						}

						return alias;

					}

				},

				identity(),

				(x, y) -> {

					throw new IllegalArgumentException(format(
							"clashing aliases for fields <%s>=%s / <%s>=%s", x.alias(), x.iri(), y.alias(), y.iri()
					));

				},

				LinkedHashMap::new
		));
	}


	static String alias(final Field field, final String x, final String y) {
		if ( y.isEmpty() || x.equals(y) ) {

			return x;

		} else if ( x.isEmpty() ) {

			return y;

		} else {

			throw new IllegalArgumentException(format(
					"clashing aliases <%s> / <%s> for field %s", x, y, field
			));

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String alias;

	private final IRI iri;
	private final Shape shape;


	Field(final String alias, final IRI iri, final Shape shape) {

		this.alias=alias;

		this.iri=iri;
		this.shape=shape;
	}


	public String alias() {
		return alias;
	}


	public IRI iri() {
		return iri;
	}

	public Shape shape() {
		return shape;
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
		return this == object || object instanceof Field
				&& alias.equals(((Field)object).alias)
				&& iri.equals(((Field)object).iri)
				&& shape.equals(((Field)object).shape);
	}

	@Override public int hashCode() {
		return alias.hashCode()
				^iri.hashCode()
				^shape.hashCode();
	}

	@Override public String toString() {

		final StringBuilder builder=new StringBuilder(25);

		builder.append("field(");

		if ( !alias.isEmpty() ) {
			builder.append('<').append(alias).append(">=");
		}

		builder.append(iri);

		if ( !shape.equals(and()) ) {
			builder.append(", ").append(indent(shape.toString()));
		}

		builder.append(")");

		return builder.toString();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class FieldsProbe extends Probe<Stream<Field>> {

		@Override public Stream<Field> probe(final Field field) {
			return Stream.of(field);
		}

		@Override public Stream<Field> probe(final And and) {
			return and.shapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<Field> probe(final Or or) {
			return or.shapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<Field> probe(final When when) {
			return Stream.of(when.pass(), when.fail()).flatMap(this);
		}

		@Override public Stream<Field> probe(final Shape shape) {
			return Stream.empty();
		}

	}

}
