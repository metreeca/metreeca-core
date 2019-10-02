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

package com.metreeca.tree.probes;

import com.metreeca.tree.Shape;
import com.metreeca.tree.shapes.*;


/**
 *
 * <p>Generates a result by inspecting shapes; concrete implementations:</p>
 *
 * <ul>
 * <li>may override probing methods for shapes of interest;</li>
 * <li>may override the {@linkplain #probe(Shape) generic probing method}, applied by default to all shapes;</li>
 * </ul>
 *
 * @param <V> the type of the generated result value
 */
public abstract class Inspector<V> extends Traverser<V> {

	//// Structural Constraints ////////////////////////////////////////////////////////////////////////////////////

	@Override public V probe(final Field field) { return probe((Shape)field); }


	//// Logical Constraints ///////////////////////////////////////////////////////////////////////////////////////

	@Override public V probe(final And and) { return probe((Shape)and); }

	@Override public V probe(final Or or) { return probe((Shape)or); }

	@Override public V probe(final When when) { return probe((Shape)when); }

}
