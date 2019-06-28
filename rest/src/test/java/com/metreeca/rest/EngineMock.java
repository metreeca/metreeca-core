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

package com.metreeca.rest;

import com.metreeca.form.Focus;
import com.metreeca.form.Query;
import com.metreeca.form.Shape;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;


public final class EngineMock implements Engine {

	@Override public <R> R exec(final Supplier<R> task) {
		return null;
	}


	@Override public Collection<Statement> relate(final IRI resource, final Query query) {
		return null;
	}

	@Override public Optional<Focus> create(final IRI resource, final Shape shape, final Collection<Statement> model) {
		return Optional.empty();
	}

	@Override public Optional<Focus> update(final IRI resource, final Shape shape, final Collection<Statement> model) {
		return Optional.empty();
	}

	@Override public Optional<Focus> delete(final IRI resource, final Shape shape) {
		return Optional.empty();
	}
}
