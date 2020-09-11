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

package com.metreeca.rdf;

import org.assertj.core.api.AbstractAssert;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Models;

import java.util.Arrays;
import java.util.Collection;

import static com.metreeca.rdf.Values.format;
import static com.metreeca.rdf.ValuesTest.encode;


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
			failWithMessage("expected model to be empty but was <\n%s\n>", indent(encode(actual)));
		}

		return this;
	}

	public ModelAssert isNotEmpty() {

		isNotNull();

		if ( actual.isEmpty() ) {
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
	 * Asserts that actual and model statement collections are isomorphic.
	 *
	 * @param model the model statement collection
	 */
	public ModelAssert isIsomorphicTo(final Model model) {

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		isNotNull();

		if ( !Models.isomorphic(actual, model) ) {
			failWithMessage(
					"model <\n%s\n> to be isomorphic to <\n%s\n>",
					indent(encode(new TreeModel(actual))), indent(encode(new TreeModel(model)))
			);
		}

		return this;
	}


	public ModelAssert isSubsetOf(final Statement... model) {
		return isSubsetOf(model == null ? null : Arrays.asList(model));
	}

	public ModelAssert isSubsetOf(final Collection<Statement> model) {
		return hasSubset(model == null ? null : new LinkedHashModel(model));
	}

	/**
	 * Asserts that the actual statement collection is a subset of the expected one.
	 *
	 * @param model the expected model
	 */
	public ModelAssert isSubsetOf(final Model model) {

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		isNotNull();

		if ( !Models.isSubset(actual, model) ) {

			final Collection<Statement> expected=new TreeModel(model);
			final Collection<Statement> exceeding=new TreeModel(actual);

			exceeding.removeAll(expected);

			failWithMessage(
					"expected model <\n%s\n> to be subset of <\n%s\n> but <\n%s\n> was found",
					indent(encode(new TreeModel(actual))), indent(encode(expected)),
					indent(encode(exceeding))
			);
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
	 * @param model the expected model
	 */
	public ModelAssert hasSubset(final Model model) {

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		isNotNull();

		if ( !Models.isSubset(model, actual) ) {

			final Collection<Statement> expected=new TreeModel(model);
			final Collection<Statement> missing=new TreeModel(expected);

			missing.removeAll(actual);

			failWithMessage(
					"expected model <\n%s\n> to have subset <\n%s\n> but <\n%s\n> was missing",
					indent(encode(new TreeModel(actual))), indent(encode(expected)),
					indent(encode(missing))
			);
		}

		return this;
	}


	public ModelAssert doesNotHaveSubset(final Statement... model) {
		return doesNotHaveSubset(model == null ? null : Arrays.asList(model));
	}

	public ModelAssert doesNotHaveSubset(final Collection<Statement> model) {
		return doesNotHaveSubset(model == null ? null : new LinkedHashModel(model));
	}

	/**
	 * Asserts that the expected statement collection is a subset of the actual one.
	 *
	 * @param model the expected model
	 */
	public ModelAssert doesNotHaveSubset(final Model model) {

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		isNotNull();

		if ( Models.isSubset(model, actual) ) {

			final Collection<Statement> expected=new TreeModel(model);
			final Collection<Statement> present=new TreeModel(expected);

			present.retainAll(actual);

			failWithMessage(
					"expected model not to have subset <\n%s\n> but <\n%s\n> was present",
					indent(encode(expected)), indent(encode(present))
			);
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
			failWithMessage(
					"expected <%s> to contain statements matching <{%s %s %s}> but has none",
					encode(actual), format(subject), format(predicate), format(object)
			);
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
			failWithMessage(
					"expected model to contain no statements matching <{%s %s %s}> but has <%s>",
					format(subject), format(predicate), format(object), encode(matching)
			);
		}

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String indent(final String string) {
		return string;
	}

}
