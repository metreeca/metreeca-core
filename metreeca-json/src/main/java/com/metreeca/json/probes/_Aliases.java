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

package com.metreeca.json.probes;

import com.metreeca.json.Shape;
import com.metreeca.json.shapes.*;

import org.eclipse.rdf4j.model.IRI;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.json.Values.direct;
import static com.metreeca.json.shapes.Meta.alias;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

public final class _Aliases {

	public static Map<IRI, String> aliases(final Shape shape) {
		if ( shape == null ) { return emptyMap(); } else {

			final Map<IRI, String> aliases=new LinkedHashMap<>();

			aliases.putAll(shape.map(new SystemAliasesProbe()));
			aliases.putAll(shape.map(new UserAliasesProbe()));

			return aliases;
		}
	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private abstract static class AliasesProbe extends Inspector<Map<IRI, String>> {

		@Override public Map<IRI, String> probe(final Shape shape) { return emptyMap(); }


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

	private static final class SystemAliasesProbe extends AliasesProbe {

		private static final java.util.regex.Pattern NamedIRIPattern
				=Pattern.compile("([/#:])(?<name>[^/#:]+)(/|#|#_|#id|#this)?$");


		@Override public Map<IRI, String> probe(final Field field) {

			final IRI name=field.name();

			return Optional
					.of(NamedIRIPattern.matcher(name.stringValue()))
					.filter(Matcher::find)
					.map(matcher -> matcher.group("name"))
					.filter(alias -> !alias.startsWith("@"))
					.map(alias -> singletonMap(name, direct(name) ? alias : alias+"Of"))
					.orElse(emptyMap());
		}

	}

	private static final class UserAliasesProbe extends AliasesProbe {

		@Override public Map<IRI, String> probe(final Field field) {

			final IRI name=field.name();
			final Shape shape=field.shape();

			return alias(shape)
					.filter(alias -> !alias.startsWith("@"))
					.map(alias -> singletonMap(name, alias))
					.orElse(emptyMap());
		}

	}
}
