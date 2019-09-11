/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.rdf.codecs;

import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Traverser;
import com.metreeca.tree.shapes.*;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.RioSettingImpl;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.rdf.Values.iri;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;


abstract class JSONCodec {

	/**
	 * The plain <a href="http://www.json.org/">JSON</a> file format.
	 *
	 * The file extension {@code .json} is recommend for JSON documents. The media type is {@code application/json}. The
	 * character encoding is {@code UTF-8}.
	 */
	public static final RDFFormat JSONFormat=new RDFFormat("JSON",
			asList("application/json", "text/json"),
			StandardCharsets.UTF_8,
			singletonList("json"),
			iri("http://www.json.org/"),
			RDFFormat.NO_NAMESPACES,
			RDFFormat.NO_CONTEXTS
	);

	/**
	 * Sets the focus resource for codecs.
	 *
	 * <p>Defaults to {@code null}.</p>
	 */
	public static final RioSetting<Resource> RioFocus=new RioSettingImpl<>(
			JSONCodec.class.getName()+"#Focus", "Resource focus", null
	);

	/**
	 * Sets the expected shape for the resources handled by codecs.
	 *
	 * <p>Defaults to {@code null}.</p>
	 */
	public static final RioSetting<com.metreeca.tree.Shape> RioShape=new RioSettingImpl<>(
			JSONCodec.class.getName()+"#Shape", "Resource shape", null
	);


	protected static final String This="_this";
	protected static final String Type="_type";

	protected static final Collection<String> Reserved=asList(This, Type);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected Map<String, String> aliases(final Shape shape) {

		if ( shape == null ) { return emptyMap(); } else {

			final Map<String, String> aliases=new LinkedHashMap<>();

			aliases.putAll(shape.map(new SystemAliasesProbe(JSONCodec.Reserved)));
			aliases.putAll(shape.map(new UserAliasesProbe(JSONCodec.Reserved)));

			return aliases;
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private abstract static class AliasesProbe extends Traverser<Map<String, String>> {

		@Override public Map<String, String> probe(final Shape shape) { return emptyMap(); }


		@Override public Map<String, String> probe(final And and) {
			return aliases(and.getShapes());
		}

		@Override public Map<String, String> probe(final Or or) {
			return aliases(or.getShapes());
		}

		@Override public Map<String, String> probe(final When when) {
			return aliases(asList(when.getPass(), when.getFail()));
		}


		private Map<String, String> aliases(final Collection<Shape> shapes) {
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

		private static final Pattern NamedIRIPattern=Pattern.compile("([/#:])(?<name>[^/#:]+)(/|#|#_|#id|#this)?$");


		private final Collection<String> reserved;


		private SystemAliasesProbe(final Collection<String> reserved) {
			this.reserved=reserved;
		}


		@Override public Map<String, String> probe(final Field field) {

			final String name=field.getName();

			return Optional
					.of(NamedIRIPattern.matcher(name))
					.filter(Matcher::find)
					.map(matcher -> matcher.group("name"))
					.filter(alias -> !reserved.contains(alias))
					.map(alias -> singletonMap(name, alias))
					.orElse(emptyMap());
		}

	}

	private static final class UserAliasesProbe extends AliasesProbe {

		private final Collection<String> reserved;


		private UserAliasesProbe(final Collection<String> reserved) {
			this.reserved=reserved;
		}


		@Override public Map<String, String> probe(final Field field) {

			final String name=field.getName();
			final Shape shape=field.getShape();

			return Optional
					.ofNullable(shape.map(new AliasProbe()))
					.filter(alias -> !reserved.contains(alias))
					.map(alias -> singletonMap(name, alias))
					.orElse(emptyMap());
		}

	}


	private static final class AliasProbe extends Traverser<String> {

		@Override public String probe(final Meta meta) {
			return meta.getValue().equals(com.metreeca.tree.Shape.Alias) ? meta.getValue().toString() : null;
		}

		@Override public String probe(final Field field) { return null; }


		@Override public String probe(final And and) {
			return alias(and.getShapes());
		}

		@Override public String probe(final Or or) {
			return alias(or.getShapes());
		}

		@Override public String probe(final When when) {
			return alias(asList(when.getPass(), when.getFail()));
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
