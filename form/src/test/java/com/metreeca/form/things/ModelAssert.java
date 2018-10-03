/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.form.things;

import org.assertj.core.api.AbstractAssert;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Models;

import java.util.Arrays;
import java.util.Collection;

import static com.metreeca.form.things.Values.format;


public final class ModelAssert extends AbstractAssert<ModelAssert, Model> {

	public static ModelAssert assertThat(final Statement... model) {
		return assertThat(model == null ? null : new LinkedHashModel(Arrays.asList(model)));
	}

	public static ModelAssert assertThat(final Collection<Statement> model) {
		return assertThat(model == null ? null : new LinkedHashModel(model));
	}

	public static ModelAssert assertThat(final Model model) {
		return new ModelAssert(model);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private ModelAssert(final Model actual) {
		super(new TreeModel(actual), ModelAssert.class);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public ModelAssert isEmpty() {

		isNotNull();

		if ( !actual.isEmpty() ) {
			failWithMessage("expected model to be empty but was <%s>", actual);
		}

		return this;
	}

	public ModelAssert isNotEmpty() {

		isNotNull();

		if ( !actual.isEmpty() ) {
			failWithMessage("expected model to be not empty");
		}

		return this;
	}


	public ModelAssert isIsomorphicTo(final Statement... model) {
		return isIsomorphicTo(model == null ? null : Arrays.asList(model));
	}

	public ModelAssert isIsomorphicTo(final Collection<Statement> model) {
		return isIsomorphicTo(model == null ? null : new LinkedHashModel(model));
	}

	/**
	 * Asserts that actual and expected statement collections are isomorphic.
	 *
	 * @param expected the expected statement collection
	 */
	public ModelAssert isIsomorphicTo(final Model expected) {

		if ( expected == null ) {
			throw new NullPointerException("null expected");
		}

		isNotNull();

		if ( !Models.isomorphic(actual, expected) ) {
			failWithMessage("expected <%s> to be isomorphic to <%s>", format(actual), format(new TreeModel(expected)));
		}

		return this;
	}


	public ModelAssert hasSubset(final Statement... model) {
		return hasSubset(model == null ? null : Arrays.asList(model));
	}

	public ModelAssert hasSubset(final Collection<Statement> model) {
		return hasSubset(model == null ? null : new LinkedHashModel(model));
	}

	/**
	 * Asserts that the expected statement collection is a subset of the actual one.
	 *
	 * @param expected the expected model
	 */
	public ModelAssert hasSubset(final Model expected) {

		if ( expected == null ) {
			throw new NullPointerException("null expected");
		}

		isNotNull();

		if ( !Models.isSubset(expected, actual) ) {
			failWithMessage("expected <%s> to have subset <%s>", format(actual), format(new TreeModel(expected)));
		}

		return this;
	}


	/**
	 * Asserts that the actual statement collection contains a statement matching a given pattern.
	 */
	public ModelAssert hasStatement(final Resource subject, final IRI predicate, final Value object) {

		isNotNull();

		final Model matching=actual.filter(subject, predicate, object);

		if ( matching.isEmpty() ) {
			failWithMessage("expected <%s> to contain no statements matching <%s %s %s> but has none",
					format(subject), format(predicate), format(object), format(actual));
		}

		return this;
	}

	/**
	 * Asserts that the actual statement collection does not contain a statement matching a given pattern.
	 */
	public ModelAssert doesNotHaveStatement(final Resource subject, final IRI predicate, final Value object) {

		isNotNull();

		final Model matching=actual.filter(subject, predicate, object);

		if ( !matching.isEmpty() ) {
			failWithMessage("expected <%s> to contain no statements matching <%s %s %s> but has <%s>",
					format(subject), format(predicate), format(object), format(actual), format(matching));
		}

		return this;
	}

}
