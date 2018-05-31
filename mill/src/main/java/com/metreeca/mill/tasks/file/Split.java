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
import com.metreeca.tray.sys._Cache;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import static com.metreeca.mill._Cell.decode;
import static com.metreeca.mill._Cell.encode;
import static com.metreeca.spec.things.Values.iri;
import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.sys.Trace.clip;

import static java.lang.String.format;


/**
 * File splitting task.
 */
public final class Split implements Task {

	private final _Cache cache=tool(_Cache.Tool);
	private final Trace trace=tool(Trace.Tool);

	private int head;
	private int size;


	public Split head(final int head) {

		if ( head < 0 ) {
			throw new IllegalArgumentException("illegal head ["+head+"]");
		}

		this.head=head;

		return this;
	}

	public Split size(final int size) {

		if ( head < 0 ) {
			throw new IllegalArgumentException("illegal size ["+size+"]");
		}

		this.size=size;

		return this;
	}


	@Override public Stream<_Cell> execute(final Stream<_Cell> items) { // !!! refactor
		return items.flatMap(item -> {

			final String url=iri(item.focus());
			final String memo=url+"@"+getClass().getName();

			if ( cache.has(memo) ) {

				try {

					return decode(cache.get(memo).reader()).stream();

				} catch ( final IOException e ) {

					trace.error(this, format("unable to extract cached chunks for <%s>", clip(url)), e);

					return Stream.empty();

				}

			} else {

				trace.info(this, format("splitting <%s>", clip(url)));

				try (final BufferedReader reader=new BufferedReader(cache.get(url).reader())) {

					for (int head=0; head < this.head; ++head) { reader.readLine(); }

					String line;

					int next=0;
					int size=0;

					final StringBuilder buffer=new StringBuilder(this.size == 0 ? 1000 : this.size*100);

					final Collection<_Cell> chunks=new ArrayList<>();

					do {

						if ( (line=reader.readLine()) != null ) {

							buffer.append(line);
							buffer.append('\n');

							size++;
						}

						if ( line == null || size > 0 && this.size > 0 && size%this.size == 0 ) { // time to write

							trace.debug(this, format("writing chunk %06d", next));

							final String chunk=url+"#"+format("%06d", next);

							chunks.add(_Cell.cell(iri(chunk))); // !!! metadata?
							cache.set(chunk, new StringReader(buffer.toString()), url);

							next++;
							size=0;

							buffer.setLength(size=0);
						}

					} while ( line != null );

					cache.set(memo, encode(chunks), url);

					trace.info(this, format("split <%s> into %,d chunks", clip(url), chunks.size()));

					return chunks.stream();

				} catch ( final IOException e ) {

					trace.error(this, format("unable to split <%s>", clip(url)), e);

					return Stream.empty();
				}

			}

		});
	}

}
