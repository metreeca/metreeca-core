/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.form.codecs;

import com.metreeca.form.*;
import com.metreeca.form.probes.Traverser;
import com.metreeca.form.shapes.*;
import com.metreeca.form.things.Maps;

import org.eclipse.rdf4j.model.IRI;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Values.direct;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

final class BaseCodec { // !! review/optimize

	static Map<IRI, String> aliases(final Shape shape) {
		return aliases(shape, emptySet());
	}

	static Map<IRI, String> aliases(final Shape shape, final Collection<String> reserved) {

		if ( reserved == null ) {
			throw new NullPointerException("null reserved");
		}

		if ( shape == null ) { return emptyMap(); } else {

			final Map<IRI, String> aliases=new LinkedHashMap<>();

			aliases.putAll(shape.map(new SystemAliasesProbe(reserved)));
			aliases.putAll(shape.map(new UserAliasesProbe(reserved)));

			return aliases;
		}
	}


	private BaseCodec() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private abstract static class AliasesProbe extends Traverser<Map<IRI, String>> {

		@Override public Map<IRI, String> probe(final Shape shape) { return Maps.map(); }


		@Override public Map<IRI, String> probe(final And and) {
			return aliases(and.getShapes());
		}

		@Override public Map<IRI, String> probe(final Or or) {
			return aliases(or.getShapes());
		}

		@Override public Map<IRI, String> probe(final When when) {
			return aliases(list(when.getPass(), when.getFail()));
		}


		private Map<IRI, String> aliases(final Collection<Shape> shapes) {
			return shapes.stream()

					// collect edge-to-alias mappings from nested shapes

					.flatMap(shape -> shape.map(this).entrySet().stream())

					// remove duplicate mappings

					.distinct()

					// group by edge and remove edges mapped to multiple aliases

					.collect(groupingBy(Map.Entry::getKey)).values().stream()
					.filter(group -> group.size() == 1)
					.map(group -> group.get(0))

					// group by alias and remove aliases mapped from multiple edges

					.collect(groupingBy(Map.Entry::getValue)).values().stream()
					.filter(group -> group.size() == 1)
					.map(group -> group.get(0))

					// collect non-clashing mappings

					.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
		}

	}

	private static final class SystemAliasesProbe extends AliasesProbe {

		private static final java.util.regex.Pattern NamedIRIPattern=Pattern.compile("([/#:])(?<name>[^/#:]+)(/|#|#_|#id|#this)?$");


		private final Collection<String> reserved;


		private SystemAliasesProbe(final Collection<String> reserved) {
			this.reserved=reserved;
		}


		@Override public Map<IRI, String> probe(final Field field) {

			final IRI iri=field.getIRI();

			return Optional
					.of(NamedIRIPattern.matcher(iri.stringValue()))
					.filter(Matcher::find)
					.map(matcher -> matcher.group("name"))
					.map(name -> direct(iri) ? name : name+"Of")
					.filter(alias -> !reserved.contains(alias))
					.map(alias -> singletonMap(iri, alias))
					.orElse(emptyMap());
		}

	}

	private static final class UserAliasesProbe extends AliasesProbe {

		private final Collection<String> reserved;


		private UserAliasesProbe(final Collection<String> reserved) {
			this.reserved=reserved;
		}


		@Override public Map<IRI, String> probe(final Field field) {

			final IRI iri=field.getIRI();
			final Shape shape=field.getShape();

			return Optional
					.ofNullable(shape.map(new AliasProbe()))
					.filter(alias -> !reserved.contains(alias))
					.map(alias -> singletonMap(iri, alias))
					.orElse(emptyMap());
		}

	}

	private static final class AliasProbe extends Traverser<String> {

		@Override public String probe(final Meta meta) {
			return meta.getIRI().equals(Form.Alias) ? meta.getValue().stringValue() : null;
		}

		@Override public String probe(final Field field) { return null; }


		@Override public String probe(final And and) {
			return alias(and.getShapes());
		}

		@Override public String probe(final Or or) {
			return alias(or.getShapes());
		}

		@Override public String probe(final When when) {
			return alias(list(when.getPass(), when.getFail()));
		}


		private String alias(final Collection<Shape> shapes) {
			return Optional
					.of(shapes.stream()
							.map(shape -> shape.map(this))
							.filter(alias -> alias != null && !alias.isEmpty())
							.collect(toSet())
					)
					.filter(aliases -> aliases.size() == 1)
					.map(aliases -> aliases.iterator().next())
					.orElse(null);
		}

	}

}
