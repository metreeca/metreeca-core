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

package com.metreeca.form;

import com.metreeca.form.shifts.Count;
import com.metreeca.form.shifts.Step;
import com.metreeca.form.shifts.Table;


/**
 * Focus-shifting operator.
 */
public interface Shift {

	public <V> V map(final Probe<V> probe);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Shift probe.
	 *
	 * <p>Generates a result by probing shifts.</p>
	 *
	 * @param <V> the type of the generated result value
	 */
	public static interface Probe<V> {

		public V probe(final Step step);

		public V probe(final Table table);

		public V probe(final Count count);

	}

}
