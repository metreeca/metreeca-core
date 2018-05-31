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

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.metreeca.mill._Cell.cell;
import static com.metreeca.spec.things.Transputs.reader;
import static com.metreeca.spec.things.Values.*;
import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.sys.Trace.clip;

import static java.lang.Math.max;
import static java.lang.String.format;


/**
 * CSV extraction task.
 */
public final class CSV implements Task {

	private static final IRI Record=iri(Internal, "Record");


	private final _Cache cache=tool(_Cache.Tool);
	private final Trace trace=tool(Trace.Tool);

	// !!! escape
	// !!! quote

	private String encoding="";
	private boolean trim;

	private CSVFormat format=CSVFormat.DEFAULT;


	public CSV encoding(final String encoding) {

		if ( encoding == null ) {
			throw new NullPointerException("null encoding");
		}

		this.encoding=encoding;

		return this;
	}

	public CSV header(final String... header) {

		if ( header == null ) {
			throw new NullPointerException("null header");
		}

		format=format.withHeader(header);

		return this;
	}

	public CSV delimiter(final char delimiter) {

		format=format.withDelimiter(delimiter);

		return this;
	}

	public CSV trim(final boolean trim) {

		this.trim=trim;

		return this;
	}


	@Override public Stream<_Cell> execute(final Stream<_Cell> items) {

		final AtomicLong count=new AtomicLong();
		final AtomicLong elapsed=new AtomicLong();

		return items.flatMap(item -> {

			try {

				trace.debug(this, format("opening record stream <%s>", clip(item.focus())));

				final _Cache.Entry entry=cache.get(iri(item.focus()));

				final Reader reader=encoding.isEmpty() ? entry.reader() : reader(entry.input(), encoding);
				final CSVParser records=new CSVParser(reader, format);

				final Map<String, Integer> fields=records.getHeaderMap();
				final IRI[] links=new IRI[fields.size()];

				// !!! normalize column names
				// !!! excel-style links if no header is present/defined

				for (final Map.Entry<String, Integer> field : fields.entrySet()) {
					links[field.getValue()]=iri(Internal, field.getKey().trim());
				}

				return StreamSupport.stream(records.spliterator(), true).map(record -> {

					final long start=System.currentTimeMillis();

					final IRI focus=iri();
					final Collection<Statement> model=new ArrayList<>();

					model.add(statement(focus, RDF.TYPE, Record));

					for (int i=0; i < links.length; ++i) {

						final String cell=record.get(i);
						final String value=trim ? cell.trim() : cell;

						if ( !value.isEmpty() ) { // !!! parametrize empty string omission?
							model.add(statement(focus, links[i], literal(value)));
						}
					}

					final long stop=System.currentTimeMillis();

					count.incrementAndGet();
					elapsed.addAndGet(stop-start);

					return cell(focus, model);

				}).onClose(() -> {

					try {

						trace.debug(this, format("closing record stream <%s>", clip(item.focus())));

						records.close();
						reader.close();

					} catch ( final IOException e ) {
						trace.error(this, format("unable to close record stream <%s>", clip(item.focus())), e);
					}

				});

			} catch ( final IOException e ) {

				trace.error(this, format("unable to retrieve item <%s>", clip(item.focus())), e);

				return Stream.empty();

			}

		}).onClose(() -> {

			final long c=count.get();
			final long e=elapsed.get();

			trace.info(this, format(
					"converted %,d records in %d ms (%d ms/record)", c, e, c == 0 ? 0 : max(e/c, 1)));

		});
	}

}
