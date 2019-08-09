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

package com.metreeca.gae.services;

import com.metreeca.gae.GAE;
import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Optimizer;
import com.metreeca.tree.probes.Traverser;
import com.metreeca.tree.shapes.*;

import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Or.or;
import static com.metreeca.tree.shapes.When.when;

import static java.util.stream.Collectors.toList;


final class DatastoreSplitter extends DatastoreProcessor {

	Shape container(final Shape shape) {

		final ContainerProbe probe=new ContainerProbe();
		final Shape container=shape.map(probe);

		return probe.traversed() ? container.map(new Optimizer()) : shape;
	}

	Shape resource(final Shape shape) {

		final SplitterProbe probe=new ResourceProbe();
		final Shape resource=shape.map(probe);

		return probe.traversed() ? resource.map(new Optimizer()) : shape;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private abstract static class SplitterProbe extends Traverser<Shape> {

		private boolean traversed;


		boolean traversed() {
			return traversed;
		}

		Shape traversed(final Shape shape) {
			try { return shape; } finally { this.traversed=true; }
		}


		@Override public Shape probe(final And and) {
			return and(and.getShapes().stream().map(s -> s.map(this)).collect(toList()));
		}

		@Override public Shape probe(final Or or) {
			return or(or.getShapes().stream().map(s -> s.map(this)).collect(toList()));
		}

		@Override public Shape probe(final When when) {
			return when(
					when.getTest().map(this),
					when.getPass().map(this),
					when.getFail().map(this)
			);
		}

	}

	private static final class ContainerProbe extends SplitterProbe {

		@Override public Shape probe(final Shape shape) {
			return shape;
		}

		@Override public Shape probe(final Field field) {
			return field.getName().equals(GAE.Contains) ? traversed(and()) : field;
		}

	}

	private static final class ResourceProbe extends SplitterProbe {

		@Override public Shape probe(final Shape shape) {
			return and();
		}

		@Override public Shape probe(final Guard guard) {
			return guard;
		}

		@Override public Shape probe(final Field field) {
			return field.getName().equals(GAE.Contains) ? traversed(field.getShape()) : and();
		}

	}

}
