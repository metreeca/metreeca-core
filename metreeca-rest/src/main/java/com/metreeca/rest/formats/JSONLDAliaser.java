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

import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.json.Values.direct;
import static com.metreeca.rest.Xtream.entry;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

final class JSONLDAliaser extends Shape.Probe<Stream<String>> {

	private static final java.util.regex.Pattern AliasPattern=Pattern.compile("\\w+");
	private static final Pattern NamedIRIPattern=Pattern.compile("([/#:])(?<name>[^/#:]+)(/|#|#_|#id|#this)?$");


	static Map<String, Field> aliases(final Shape shape) {
		return aliases(shape, emptyMap());
	}

	static Map<String, Field> aliases(final Shape shape, final Map<String, String> keywords) {
		return shape == null ? emptyMap() : Field.fields(shape)

				.map(field -> entry(alias(field, keywords), field))

				.map(entry -> {

					final String alias=entry.getKey();
					final Field field=entry.getValue();

					if ( field.name().equals(RDF.TYPE) // !!! factor with alias()
							&& alias.equals(keywords.getOrDefault("@type", "@type")) ) {

						return entry;

					} else if ( !AliasPattern.matcher(alias).matches() ) {

						throw new IllegalArgumentException(format(
								"malformed alias <%s> for <field(%s)>", alias, field.name()
						));

					} else if ( alias.startsWith("@") || keywords.containsValue(alias) ) {

						throw new IllegalArgumentException(format(
								"reserved alias <%s> for <field(%s)>", alias, field.name()
						));

					} else {

						return entry;
					}

				})

				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> {

					throw new IllegalArgumentException(format(
							"clashing aliases for <field(%s)> / <field(%s)>", x.name(), y.name()
					));

				}, LinkedHashMap::new));
	}


	private static String alias(final Field field, final Map<String, String> keywords) {

		final Set<String> aliases=field.shape().map(new JSONLDAliaser()).collect(toSet());

		if ( aliases.size() > 1 ) { // clashing aliases

			throw new IllegalArgumentException(format(
					"multiple aliases <%s> for <field(%s)>", aliases, field.name()
			));

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

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
