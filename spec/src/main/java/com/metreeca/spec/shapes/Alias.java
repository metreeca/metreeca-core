/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.spec.shapes;

import com.metreeca.spec.Shape;
import com.metreeca.spec.shifts.Step;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.jeep.Lists.list;
import static com.metreeca.jeep.Maps.map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;


/**
 * Trait alias annotation.
 *
 * <p>Provides an alternate property name for reporting values for the enclosing trait shape (e.g. in the context of
 * JSON-based RDF serialization results).</p>
 */
public final class Alias implements Shape {

	private static final Pattern AliasPattern=Pattern.compile("\\w+");
	private static final Pattern NamedIRIPattern=Pattern.compile("([/#:])(?<name>[^/#:]+)(/|#|#_|#id|#this)?$");


	public static Alias alias(final String text) {
		return new Alias(text);
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


	private final String text;


	public Alias(final String text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		if ( !AliasPattern.matcher(text).matches() ) {
			throw new IllegalArgumentException("illegal text ["+text+"]");
		}

		this.text=text;
	}


	public String getText() {
		return text;
	}


	@Override public <T> T accept(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.visit(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Alias
				&& text.equals(((Alias)object).text);
	}

	@Override public int hashCode() {
		return text.hashCode();
	}

	@Override public String toString() {
		return "alias("+text+")";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private abstract static class AliasesProbe extends Probe<Map<Step, String>> {

		@Override protected Map<Step, String> fallback(final Shape shape) { return map(); }


		@Override public Map<Step, String> visit(final Group group) {
			return group.getShape().accept(this);
		}


		@Override public Map<Step, String> visit(final Virtual virtual) {
			return virtual.getTrait().accept(this);
		}


		@Override public Map<Step, String> visit(final And and) {
			return aliases(and.getShapes());
		}

		@Override public Map<Step, String> visit(final Or or) {
			return aliases(or.getShapes());
		}

		@Override public Map<Step, String> visit(final Test test) {
			return aliases(list(test.getPass(), test.getFail()));
		}


		private Map<Step, String> aliases(final Collection<Shape> shapes) {
			return shapes.stream()

					// collect edge-to-alias mappings from nested shapes

					.flatMap(shape -> shape.accept(this).entrySet().stream())

					// remove duplicate mappings

					.distinct()

					// group by edge and remove edges mapped to multiple aliases

					.collect(groupingBy(Entry::getKey)).values().stream()
					.filter(group -> group.size() == 1)
					.map(group -> group.get(0))

					// group by alias and remove aliases mapped from multiple edges

					.collect(groupingBy(Entry::getValue)).values().stream()
					.filter(group -> group.size() == 1)
					.map(group -> group.get(0))

					// collect non-clashing mappings

					.collect(toMap(Entry::getKey, Entry::getValue));
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

	private static final class SystemAliasesProbe extends AliasesProbe {

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


	private static final class AliasProbe extends Probe<String> {

		@Override public String visit(final Alias alias) {
			return alias.getText();
		}


		@Override public String visit(final Group group) {
			return group.getShape().accept(this);
		}

		@Override public String visit(final And and) {
			return alias(and.getShapes());
		}

		@Override public String visit(final Or or) {
			return alias(or.getShapes());
		}

		@Override public String visit(final Test test) {
			return alias(list(test.getPass(), test.getFail()));
		}


		private String alias(final Collection<Shape> shapes) {
			return Optional
					.of(shapes.stream()
							.map(shape -> shape.accept(this))
							.filter(alias -> alias != null)
							.collect(toSet()))
					.filter(aliases -> aliases.size() == 1)
					.map(aliases -> aliases.iterator().next())
					.orElse(null);
		}

	}

}
