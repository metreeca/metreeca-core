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

package com.metreeca.mill.tasks.file;


import com.metreeca.mill.Task;
import com.metreeca.mill._Cell;
import com.metreeca.tray.Tool;
import com.metreeca.tray.sys.Trace;
import com.metreeca.tray.sys._Cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.metreeca.jeep.rdf.Values.iri;
import static com.metreeca.tray.sys.Trace.clip;

import static java.lang.String.format;


/**
 * ZIP extraction task.
 */
public final class ZIP implements Task {

	@Override public Stream<_Cell> execute(final Tool.Loader tools, final Stream<_Cell> items) {

		final _Cache cache=tools.get(_Cache.Tool);
		final Trace trace=tools.get(Trace.Tool);

		return items.flatMap(item -> {

			final String url=iri(item.focus());
			final String memo=url+"@"+getClass().getName();

			if ( cache.has(memo) ) {

				try {

					return _Cell.decode(cache.get(memo).reader()).stream();

				} catch ( final IOException e ) {

					trace.error(this, format("unable to extract cached zip entries for <%s>", clip(url)), e);

					return Stream.empty();

				}

			} else {

				trace.info(this, format("unzipping <%s>", clip(url)));

				try (final ZipInputStream zip=new ZipInputStream(cache.get(url).input())) {

					final Collection<_Cell> chunks=new ArrayList<>();

					for (ZipEntry entry; (entry=zip.getNextEntry()) != null; ) {

						final String name=entry.getName();

						if ( !name.endsWith("/") ) {

							trace.info(this, format("extracting <%s>", clip(name)));

							final String chunk=url+"#"+name;

							chunks.add(_Cell.cell(iri(chunk))); // !!! metadata in cell model
							cache.set(chunk, zip, url);
						}

					}

					cache.set(memo, _Cell.encode(chunks), url);

					return chunks.stream();

				} catch ( IOException e ) {

					trace.error(this, format("unable to extract zip entries from <%s>", clip(url)), e);

					return Stream.empty();

				}

			}

		});

	}

}
