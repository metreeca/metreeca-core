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

package com.metreeca.rdf.formats;

import com.metreeca.rdf.Values;
import com.metreeca.tree.probes.Optimizer;


/**
 * Shape optimizer.
 *
 * <p>Recursively removes redundant constructs from a shape.</p>
 */
final class RDFOptimizer extends Optimizer {

	@Override protected boolean derives(final Object upper, final Object lower) {
		return upper.equals(Values.ValueType)
				|| upper.equals(Values.ResourceType) && resource(lower)
				|| upper.equals(Values.LiteralType) && literal(lower);
	}


	private boolean resource(final Object type) {
		return type.equals(Values.ResourceType) || type.equals(Values.BNodeType) || type.equals(Values.IRIType);
	}

	private boolean literal(final Object type) {
		return type.equals(Values.LiteralType) || !type.equals(Values.ValueType) && !resource(type);
	}

}
