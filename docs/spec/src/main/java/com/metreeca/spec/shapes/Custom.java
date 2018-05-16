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

import com.metreeca.spec.Issue;
import com.metreeca.spec.Shape;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;

import static java.lang.Math.abs;


/**
 * Custom value constraint.
 *
 * <p>States that each term in the focus set meets a custom constraint, defined as a select SPARQL query.</p>
 */
public final class Custom implements Shape {

	// !!! document
	//
	// Input bindings:
	//
	// ?this value in focus set
	// values ?/$this {}; ignoring spaces/case
	//
	// Output bindings:
	//
	// variables -> template values for message
	//

	public static Custom custom(final Issue.Level level, final String label, final String query) {
		return new Custom(level, label, query);
	}

	public static String message(final String template, final Iterable<Binding> bindings) {

		if ( template == null ) {
			throw new NullPointerException("null template");
		}

		if ( bindings == null ) {
			throw new NullPointerException("null bindings");
		}

		if ( template.isEmpty() ) { return ""; } else {

			String message=template;

			for (final Binding binding : bindings) { // !!! optimize using regex

				final String name=binding.getName();
				final Value value1=binding.getValue();

				message=template.replaceAll("\\{[?$]"+name+"\\}",
						value1 == null ? "null" : value1.stringValue());
			}

			return message;
		}
	}


	private final Issue.Level level;
	private final String message;
	private final String query;


	public Custom(final Issue.Level level, final String message, final String query) {

		if ( level == null ) {
			throw new NullPointerException("null level");
		}

		if ( message == null ) {
			throw new NullPointerException("null label");
		}

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		this.level=level;
		this.message=message;
		this.query=query;
	}


	public Issue.Level getLevel() {
		return level;
	}

	public String getMessage() {
		return message;
	}

	public String getQuery() {
		return query;
	}


	@Override public <V> V accept(final Probe<V> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.visit(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Custom
				&& level == ((Custom)object).level
				&& message.equals(((Custom)object).message)
				&& query.equals(((Custom)object).query);
	}

	@Override public int hashCode() {
		return level.hashCode()^message.hashCode()^query.hashCode();
	}

	@Override public String toString() {
		return String.format("custom(%s) [%s/#%d]",
				message, level.toString().toLowerCase(), abs(query.hashCode()));
	}

}
