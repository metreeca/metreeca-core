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

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.util.Collection;

import static com.metreeca.form.things.ValuesTest.encode;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.truth.Fact.fact;
import static com.google.common.truth.Fact.simpleFact;
import static com.google.common.truth.Truth.assertAbout;

import static java.util.Arrays.asList;


public final class ModelSubject extends Subject<ModelSubject, Model> {

	public static ModelSubject assertThat(final Collection<Statement> model) {
		return assertThat(model == null ? null : new LinkedHashModel(model));
	}

	public static ModelSubject assertThat(final Model model) {
		return assertAbout(ModelSubject::new).that(model);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private ModelSubject(final FailureMetadata metadata, final Model subject) {
		super(metadata, subject);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public void isEmpty() {
		if (!actual().isEmpty()) {
			failWithActual(simpleFact("expected to be empty"));
		}
	}

	public void isNotEmpty() {
		if (actual().isEmpty()) {
			failWithoutActual(simpleFact("expected not to be empty"));
		}
	}

	public void hasSize(final int size) {
		checkArgument(size >= 0, "size (%s) must be >= 0", size);
		check("size()").that(actual().size()).isEqualTo(size);
	}

	public void isIsomorphicTo(final Statement... model) {
		isIsomorphicTo(model == null ? null : asList(model));
	}

	public void isIsomorphicTo(final Collection<Statement> model) {
		isIsomorphicTo(model == null ? null : new LinkedHashModel(model));
	}

	public void isIsomorphicTo(final Model model) {

		if ( model == null ) {

			if ( actual() != null ) {
				failWithoutActual(fact("expected", format(model)), fact("but was", format(actual())));
			}

		} else {

			if ( actual() == null || !Models.isomorphic(actual(), model) ) {

				failWithoutActual(fact("expected", format(model)), fact("but was", format(actual())));

			}

		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String format(final Model model) {
		return model == null ? "null model" : encode(new TreeModel(model), RDFFormat.NTRIPLES);
	}

}
