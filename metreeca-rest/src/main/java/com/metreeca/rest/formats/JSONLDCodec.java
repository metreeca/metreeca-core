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

package com.metreeca.rest.formats;

import com.metreeca.json.Shape;
import com.metreeca.json.shapes.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.json.Values.direct;
import static com.metreeca.json.shapes.Guard.*;
import static com.metreeca.rest.Xtream.entry;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.*;

final class JSONLDCodec {

	static Shape driver(final Shape shape) { // !!! caching
		return shape.redact(

				retain(Role, true),
				retain(Task, true),
				retain(Area, true),
				retain(Mode, Convey) // remove internal filtering shapes

		).expand(); // add inferred constraints to drive json shorthands
	}

	static <V> V error(final String message, final Object... args) {
		throw new IllegalArgumentException(format(message, args));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	static boolean tagged(final Shape shape) {
		return datatype(shape).filter(RDF.LANGSTRING::equals).isPresent();
	}

	static boolean localized(final Shape shape) {
		return (shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new ValueProbe<Object>() {

			@Override public Object probe(final Localized localized) { return localized; }

		}))).isPresent();
	}


	static Optional<IRI> datatype(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new ValueProbe<IRI>() {

			@Override public IRI probe(final Datatype datatype) { return datatype.iri(); }

		}));
	}

	static Optional<IRI> _clazz(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new ValueProbe<IRI>() {

			@Override public IRI probe(final Clazz clazz) { return clazz.iri(); }

		}));
	}

	static Optional<Collection<Value>> range(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new ValueProbe<Collection<Value>>() {

			@Override public Collection<Value> probe(final Range range) { return range.values(); }

		}));
	}

	static Optional<Set<String>> langs(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new ValueProbe<Set<String>>() {

			@Override public Set<String> probe(final Lang lang) { return lang.tags(); }

		}));
	}


	private abstract static class ValueProbe<V> extends Probe<V> {

		@Override public V probe(final And and) {
			return value(and.shapes().stream());
		}

		@Override public V probe(final Or or) {
			return value(or.shapes().stream());
		}

		@Override public V probe(final When when) {
			return value(Stream.of(when.pass(), when.fail()));
		}


		private V value(final Stream<Shape> shapes) {

			final Set<V> values=shapes
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.collect(toSet());

			if ( values.size() > 1 ) {
				error("conflicting values {%s}",
						values.stream().map(Object::toString).collect(joining(", "))
				);
			}

			return values.isEmpty() ? null : values.iterator().next();

		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final BinaryOperator<Integer> min=(x, y) ->
			x == null ? y : y == null ? x : x.compareTo(y) <= 0 ? x : y;

	private static final BinaryOperator<Integer> max=(x, y) ->
			x == null ? y : y == null ? x : x.compareTo(y) >= 0 ? x : y;

	// ;(jdk-1.8) replacing compareTo() with Math.min/max() causes a NullPointerException during Integer unboxing


	static Optional<Integer> minCount(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new MinCountProbe()));
	}

	static Optional<Integer> maxCount(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new MaxCountProbe()));
	}


	private static final class MinCountProbe extends Probe<Integer> {

		@Override public Integer probe(final MinCount minCount) {
			return minCount.limit();
		}

		@Override public Integer probe(final And and) {
			return and.shapes().stream()
					.map(shape -> shape.map(this))
					.reduce(null, max);
		}

		@Override public Integer probe(final Or or) {
			return or.shapes().stream()
					.map(shape -> shape.map(this))
					.reduce(null, min);
		}

		@Override public Integer probe(final When when) {
			return min.apply(
					when.pass().map(this),
					when.fail().map(this)
			);
		}

	}

	private static final class MaxCountProbe extends Probe<Integer> {

		@Override public Integer probe(final MaxCount maxCount) {
			return maxCount.limit();
		}

		@Override public Integer probe(final And and) {
			return and.shapes().stream()
					.map(shape -> shape.map(this))
					.reduce(null, min);
		}

		@Override public Integer probe(final Or or) {
			return or.shapes().stream()
					.map(shape -> shape.map(this))
					.reduce(null, max);
		}

		@Override public Integer probe(final When when) {
			return max.apply(
					when.pass().map(this),
					when.fail().map(this)
			);
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	static Predicate<Collection<? extends Value>> all(final Shape shape) {
		return values -> !Boolean.FALSE.equals(shape.map(new SetProbe() {

			@Override public Boolean probe(final All all) { return values.containsAll(all.values()); }

		}));
	}

	static Predicate<Collection<? extends Value>> any(final Shape shape) {
		return values -> !Boolean.FALSE.equals(shape.map(new SetProbe() {

			@Override public Boolean probe(final Any any) { return any.values().stream().anyMatch(values::contains); }

		}));
	}


	private abstract static class SetProbe extends Probe<Boolean> {

		private static boolean or(final Boolean x, final Boolean y) {
			return x == null ? y : y == null ? x : x || y;
		}

		private static boolean and(final Boolean x, final Boolean y) {
			return x == null ? y : y == null ? x : x && y;
		}


		@Override public Boolean probe(final And and) {
			return and.shapes().stream().map(this).reduce(true, SetProbe::and);
		}

		@Override public Boolean probe(final Or or) {
			return or.shapes().stream().map(this).reduce(false, SetProbe::or);
		}

		@Override public Boolean probe(final When when) {
			return Stream.of(
					when.pass().map(this), when.fail().map(this)
			).reduce(false, SetProbe::or);
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final Pattern AliasPattern=Pattern.compile("\\w+");
	private static final Pattern NamedIRIPattern=Pattern.compile("([/#:])(?<name>[^/#:]+)(/|#|#_|#id|#this)?$");


	static Map<String, Field> fields(final Shape shape) {
		return fields(shape, emptyMap());
	}

	static Map<String, Field> fields(final Shape shape, final Map<String, String> keywords) {
		return shape == null ? emptyMap() : Field.fields(shape)

				.map(field -> entry(alias(field, keywords), field))

				.map(entry -> {

					final String alias=entry.getKey();
					final Field field=entry.getValue();

					if ( field.name().equals(RDF.TYPE) // !!! factor with alias()
							&& alias.equals(keywords.getOrDefault("@type", "@type")) ) {

						return entry;

					} else if ( !AliasPattern.matcher(alias).matches() ) {

						return error("malformed alias <%s> for <field(%s)>", alias, field.name());

					} else if ( alias.startsWith("@") || keywords.containsValue(alias) ) {

						return error("reserved alias <%s> for <field(%s)>", alias, field.name());

					} else {

						return entry;
					}

				})

				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> error(
						"clashing aliases for <field(%s)> / <field(%s)>", x.name(), y.name()
				), LinkedHashMap::new));
	}


	private static String alias(final Field field, final Map<String, String> keywords) {

		final Set<String> aliases=field.shape().map(new AliasesProbe()).collect(toSet());

		if ( aliases.size() > 1 ) { // clashing aliases

			return error("multiple aliases <%s> for <field(%s)>", aliases, field.name()
			);

		} else if ( aliases.size() == 1 ) { // user-defined alias

			return aliases.iterator().next();

		} else { // system-inferred alias  // !!! factor with fields()

			return field.name().equals(RDF.TYPE) ? keywords.getOrDefault("@type", "@type") : Optional

					.of(NamedIRIPattern.matcher(field.name().stringValue()))
					.filter(Matcher::find)
					.map(matcher -> matcher.group("name"))
					.map(alias -> direct(field.name()) ? alias : alias+"Of") // !!! inverse?

					.orElseThrow(() -> new IllegalArgumentException(String.format(
							"undefined alias for <field(%s)>", field.name()
					)));

		}
	}


	private static final class AliasesProbe extends Probe<Stream<String>> {

		@Override public Stream<String> probe(final Meta meta) {
			return meta.label().equals("alias") ? Stream.of(meta.value()) : Stream.empty();
		}

		@Override public Stream<String> probe(final And and) {
			return and.shapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<String> probe(final Or or) {
			return or.shapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<String> probe(final When when) {
			return Stream.of(when.pass(), when.fail()).flatMap(this);
		}

		@Override public Stream<String> probe(final Shape shape) {
			return Stream.empty();
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JSONLDCodec() {}

}
