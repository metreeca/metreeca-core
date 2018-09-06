/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.mill;

import com.metreeca.tray.sys.Trace;

import java.util.stream.Stream;

import static com.metreeca.form.things.Values.bnode;
import static com.metreeca.tray._Tray.tool;
import static com.metreeca.tray.sys.Trace.time;

import static java.lang.String.format;


public final class Mill {

	private final Trace trace=tool(Trace.Factory);


	public Mill execute(final Task task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		trace.info(this, format("executed task in %,d ms", time(() -> {
			try (final Stream<_Cell> execute=task.execute(Stream.of(_Cell.cell(bnode())))) { execute.count(); }
		})));

		return this;
	}

}
