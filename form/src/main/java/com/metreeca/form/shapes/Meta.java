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

package com.metreeca.form.shapes;

import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.shifts.Step;
import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Values.literal;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;


/**
 * Non-validating annotation constraint.
 *
 * <p>States that the enclosing shape has a given value for an annotation property.</p>
 */
public final class Meta implements Shape {

	public static Meta alias(final String value) {
		return new Meta(Form.Alias, literal(value));
	}

	public static Meta label(final String value) {
		return new Meta(Form.Label, literal(value));
	}

	public static Meta notes(final String value) {
		return new Meta(Form.Notes, literal(value));
	}

	public static Meta placeholder(final String value) {
		return new Meta(Form.Placeholder, literal(value));
	}

	public static Meta dflt(final Value value) {
		return new Meta(Form.Default, value);
	}

	public static Meta hint(final IRI value) {
		return new Meta(Form.Hint, value);
	}

	public static Meta group(final Value value) {
		return new Meta(Form.Group, value);
	}


	public static Meta meta(final IRI label, final Value value) {
		return new Meta(label, value);
	}



	public static Map<Step, String> aliases(final Shape shape) {
		return aliases(shape, emptySet());
	}

	public static Map<Step, String> aliases(final Shape shape, final Collection<String> reserved) {

		if ( reserved == null ) {
			throw new NullPointerException("null reserved");
		}

		if ( shape == null ) { return emptyMap(); } else {

			final Map<Step, String> aliases=new LinkedHashMap<>();

			aliases.putAll(shape.accept(new SystemAliasesProbe(reserved)));
			aliases.putAll(shape.accept(new UserAliasesProbe(reserved)));

			return aliases;
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final IRI iri;
	private final Value value;


	private Meta(final IRI iri, final Value value) {

		if ( iri == null ) {
			throw new NullPointerException("null IRI");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		this.iri=iri;
		this.value=value;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public IRI getIRI() {
		return iri;
	}

	public Value getValue() {
		return value;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T accept(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.visit(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Meta
				&& iri.equals(((Meta)object).iri)
				&& value.equals(((Meta)object).value);
	}

	@Override public int hashCode() {
		return iri.hashCode()^value.hashCode();
	}

	@Override public String toString() {
		return Values.format(iri)+"="+Values.format(value);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private abstract static class AliasesProbe extends Probe<Map<Step, String>> {

		@Override protected Map<Step, String> fallback(final Shape shape) { return map(); }


		@Override public Map<Step, String> visit(final Virtual virtual) {
			return virtual.getTrait().accept(this);
		}


		@Override public Map<Step, String> visit(final And and) {
			return aliases(and.getShapes());
		}

		@Override public Map<Step, String> visit(final Or or) {
			return aliases(or.getShapes());
		}

		@Override public Map<Step, String> visit(final Option option) {
			return aliases(list(option.getPass(), option.getFail()));
		}


		private Map<Step, String> aliases(final Collection<Shape> shapes) {
			return shapes.stream()

					// collect edge-to-alias mappings from nested shapes

					.flatMap(shape -> shape.accept(this).entrySet().stream())

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


		@Override public Map<Step, String> visit(final Trait trait) {

			final Step step=trait.getStep();

			return Optional
					.of(NamedIRIPattern.matcher(step.getIRI().stringValue()))
					.filter(Matcher::find)
					.map(matcher -> matcher.group("name"))
					.map(name -> step.isInverse() ? name+"Of" : name)
					.filter(alias -> !reserved.contains(alias))
					.map(alias -> singletonMap(step, alias))
					.orElse(emptyMap());
		}

	}

	private static final class UserAliasesProbe extends AliasesProbe {

		private final Collection<String> reserved;


		private UserAliasesProbe(final Collection<String> reserved) {
			this.reserved=reserved;
		}


		@Override public Map<Step, String> visit(final Trait trait) {

			final Step step=trait.getStep();
			final Shape shape=trait.getShape();

			return Optional
					.ofNullable(shape.accept(new AliasProbe()))
					.filter(alias -> !reserved.contains(alias))
					.map(alias -> singletonMap(step, alias))
					.orElse(emptyMap());
		}

	}


	private static final class AliasProbe extends Probe<String> {

		@Override public String visit(final Meta meta) {
			return meta.getIRI().equals(Form.Alias)? meta.getValue().stringValue() : null;
		}


		@Override public String visit(final And and) {
			return alias(and.getShapes());
		}

		@Override public String visit(final Or or) {
			return alias(or.getShapes());
		}

		@Override public String visit(final Option option) {
			return alias(list(option.getPass(), option.getFail()));
		}


		private String alias(final Collection<Shape> shapes) {
			return Optional
					.of(shapes.stream()
							.map(shape -> shape.accept(this))
							.filter(alias -> alias != null && !alias.isEmpty())
							.collect(toSet())
					)
					.filter(aliases -> aliases.size() == 1)
					.map(aliases -> aliases.iterator().next())
					.orElse(null);
		}

	}

}
