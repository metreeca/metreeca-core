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

package com.metreeca.form.probes;

import com.metreeca.form.Shape;
import com.metreeca.form.shapes.*;


/**
 * Shape visitor.
 *
 * <p>Generates a result by visiting shapes.</p>
 *
 * @param <V> the type of the generated result value
 */
public abstract class Visitor<V> implements Shape.Probe<V> {

	/**
	 * Probes a generic shape.
	 *
	 * @param shape the generic shape to be probed
	 *
	 * @return the result generated by probing {@code shape}; by default {@code null}
	 */
	public V probe(final Shape shape) { return null; }


	//// Annotations ///////////////////////////////////////////////////////////////////////////////////////////////

	@Override public V probe(final Meta meta) { return probe((Shape)meta); }

	@Override public V probe(final When when) { return probe((Shape)when); }


	//// Term Constraints //////////////////////////////////////////////////////////////////////////////////////////

	@Override public V probe(final Datatype datatype) { return probe((Shape)datatype); }

	@Override public V probe(final Clazz clazz) { return probe((Shape)clazz); }


	@Override public V probe(final MinExclusive minExclusive) { return probe((Shape)minExclusive); }

	@Override public V probe(final MaxExclusive maxExclusive) { return probe((Shape)maxExclusive); }

	@Override public V probe(final MinInclusive minInclusive) { return probe((Shape)minInclusive); }

	@Override public V probe(final MaxInclusive maxInclusive) { return probe((Shape)maxInclusive); }


	@Override public V probe(final MinLength minLength) { return probe((Shape)minLength); }

	@Override public V probe(final MaxLength maxLength) { return probe((Shape)maxLength); }

	@Override public V probe(final Pattern pattern) { return probe((Shape)pattern); }

	@Override public V probe(final Like like) { return probe((Shape)like); }


	//// Set Constraints ///////////////////////////////////////////////////////////////////////////////////////////

	@Override public V probe(final MinCount minCount) { return probe((Shape)minCount); }

	@Override public V probe(final MaxCount maxCount) { return probe((Shape)maxCount); }

	@Override public V probe(final In in) { return probe((Shape)in); }

	@Override public V probe(final All all) { return probe((Shape)all); }

	@Override public V probe(final Any any) { return probe((Shape)any); }


	//// Structural Constraints ////////////////////////////////////////////////////////////////////////////////////

	@Override public V probe(final Trait trait) { return probe((Shape)trait); }


	//// Logical Constraints ///////////////////////////////////////////////////////////////////////////////////////

	@Override public V probe(final And and) { return probe((Shape)and); }

	@Override public V probe(final Or or) { return probe((Shape)or); }

	@Override public V probe(final Option option) { return probe((Shape)option); }

}
