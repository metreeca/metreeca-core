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

package com.metreeca.gcp.services;

import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Optimizer;
import com.metreeca.tree.probes.Redactor;


abstract class DatastoreProcessor {

	Shape expand(final Shape shape) { // !!! caching
		return shape

				.map(new _DatastoreInferencer())
				.map(new Optimizer());
	}


	Shape digest(final Shape shape) { // !!! caching
		return shape

				.map(new Redactor(Shape.Area, Shape.Digest))
				.map(new Optimizer());
	}

	Shape detail(final Shape shape) { // !!! caching
		return shape

				.map(new Redactor(Shape.Area, Shape.Detail))
				.map(new Optimizer());
	}


	Shape convey(final Shape shape) { // !!! caching
		return shape

				.map(new Redactor(Shape.Mode, Shape.Convey))
				.map(new Optimizer());
	}

	Shape filter(final Shape shape) { // !!! caching
		return shape

				.map(new Redactor(Shape.Mode, Shape.Filter))
				.map(new Optimizer());
	}

}
