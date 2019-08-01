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

package com.metreeca.back.gae;

import com.metreeca.form.Focus;
import com.metreeca.form.Query;
import com.metreeca.form.Shape;
import com.metreeca.rest.Engine;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

import static com.metreeca.back.gae.Datastore.datastore;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.tray.Tray.tool;


public final class DatastoreEngine implements Engine {

	private final Datastore datastore=tool(datastore());


	@Override public <R> R exec(final Supplier<R> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return datastore.exec(service -> task.get());
	}


	@Override public Collection<Statement> relate(final IRI resource, final Query query) {
		return set();
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
