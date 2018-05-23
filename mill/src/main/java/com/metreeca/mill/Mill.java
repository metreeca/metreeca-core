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

import com.metreeca.tray.Tool;
import com.metreeca.tray.Tray;
import com.metreeca.tray.sys.Trace;

import java.util.stream.Stream;

import static com.metreeca.spec.things.Values.bnode;
import static com.metreeca.tray.sys.Trace.time;

import static java.lang.String.format;


public final class Mill {

	private final Tool.Loader tools;


	public Mill() { this(Tray.tray());}

	public Mill(final Tool.Loader tools) {

		if ( tools == null ) {
			throw new NullPointerException("null tools");
		}

		this.tools=tools;
	}


	public Mill execute(final Task task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		tools.get(Trace.Tool).info(this, format("executed task in %,d ms", time(() -> {
			try (final Stream<_Cell> execute=task.execute(tools, Stream.of(_Cell.cell(bnode())))) { execute.count(); }
		})));

		return this;
	}

}
