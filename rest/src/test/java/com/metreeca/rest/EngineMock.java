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

import java.util.*;
import java.util.function.Supplier;

import static com.metreeca.form.Focus.focus;
import static com.metreeca.form.things.Sets.set;


public final class EngineMock implements Engine {

	private final Map<IRI, Collection<Statement>> models=new HashMap<>();


	@Override public <R> R exec(final Supplier<R> task) {
		synchronized ( models ) {
			return task.get();
		}
	}


	@Override public Collection<Statement> relate(final IRI resource, final Query query) {
		synchronized ( models ) {
			return models.getOrDefault(resource, set());
		}
	}

	@Override public Optional<Focus> create(final IRI resource, final Shape shape, final Collection<Statement> model) {
		synchronized ( models ) {
			if ( models.containsKey(resource) ) { return Optional.empty(); } else {

				models.put(resource, new LinkedHashSet<>(model));

				return Optional.of(focus()); // !!! validation

			}
		}
	}

	@Override public Optional<Focus> update(final IRI resource, final Shape shape, final Collection<Statement> model) {
		synchronized ( models ) {
			if ( !models.containsKey(resource) ) { return Optional.empty(); } else {

				models.put(resource, new LinkedHashSet<>(model));

				return Optional.of(focus()); // !!! validation

			}
		}
	}

	@Override public Optional<Focus> delete(final IRI resource, final Shape shape) {
		synchronized ( models ) {
			if ( !models.containsKey(resource) ) { return Optional.empty(); } else {

				models.remove(resource);

				return Optional.of(focus()); // !!! validation

			}
		}
	}

}
