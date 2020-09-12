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

package com.metreeca.json;

import com.metreeca.json.queries.*;

import java.util.function.Function;


/**
 * Shape-driven linked data query.
 */
public interface Query {

	public <V> V map(final Probe<V> probe);

	public default <V> V map(final Function<Query, V> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return mapper.apply(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Query probe.
	 *
	 * <p>Generates a result by probing queries.</p>
	 *
	 * @param <V> the type of the generated result value
	 */
	public static interface Probe<V> extends Function<Query, V> {

		@Override default V apply(final Query query) {
			return query == null ? null : query.map(this);
		}


		public V probe(final Items items);

		public V probe(final Terms terms);

		public V probe(final Stats stats);

	}

}
