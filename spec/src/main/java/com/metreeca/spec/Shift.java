/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.spec;

import com.metreeca.spec.shifts.Count;
import com.metreeca.spec.shifts.Step;
import com.metreeca.spec.shifts.Table;


public interface Shift {

	public <V> V accept(final Probe<V> probe);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public abstract static class Probe<V> {

		public V visit(final Step step) { return fallback(step); }

		public V visit(final Count count) { return fallback(count); }

		public V visit(final Table table) { return fallback(table); }


		protected V fallback(final Shift t) {
			return null;
		}

	}

}
