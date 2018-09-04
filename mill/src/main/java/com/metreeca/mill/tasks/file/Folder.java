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

package com.metreeca.mill.tasks.file;


import com.metreeca.mill.Task;
import com.metreeca.mill._Cell;
import com.metreeca.tray.sys.Trace;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import static com.metreeca.form.things.Values.iri;
import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.sys.Trace.clip;

import static java.lang.String.format;


/**
 * Folder listing task.
 */
public final class Folder implements Task {

	// !!! recursive listing
	// !!! caching?

	private final Trace trace=tool(Trace.Factory);

	@Override public Stream<_Cell> execute(final Stream<_Cell> items) {
		return items.flatMap(item -> {

			final String url=iri(item.focus());

			trace.info(this, format("listing <%s>", clip(url)));

			final Collection<_Cell> cells=new ArrayList<>();

			if ( url.startsWith("file:") ) {

				final File[] files=new File(URI.create(url)).listFiles();

				if ( files != null ) {

					for (final File file : files) {
						cells.add(_Cell.cell(iri(file.toURI().toString()))); // !!! metadata in cell model
					}

				} else {

					trace.warning(this, String.format("<%s> is not a folder", clip(url)));

				}

			} else {

				trace.warning(this, String.format("<%s> is not a file URL", clip(url)));

			}

			return cells.stream();

		});

	}

}
