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

package com.metreeca.rest.engines;

import com.metreeca.form.Focus;
import com.metreeca.form.Shape;
import com.metreeca.rest.Engine;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.Optional;

/**
 * Shape-driven container engine.
 *
 * <p>Manages CRUD lifecycle operations on container resource descriptions defined by a shape.</p>
 */
final class ShapedContainer implements Engine {

	ShapedContainer(final Graph graph, final Shape shape) {}


	@Override public Optional<Collection<Statement>> relate(final IRI resource) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public Optional<Focus> create(final IRI resource, final IRI related, final Collection<Statement> model) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public Optional<Focus> update(final IRI resource, final Collection<Statement> model) {
		throw new UnsupportedOperationException("shaped container updating not supported");
	}

	@Override public Optional<IRI> delete(final IRI resource) {
		throw new UnsupportedOperationException("shaped container deletion not supported");
	}

}
