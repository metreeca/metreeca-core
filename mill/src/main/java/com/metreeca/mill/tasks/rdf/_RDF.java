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

package com.metreeca.mill.tasks.rdf;


import com.metreeca.mill.Task;
import com.metreeca.mill._Cell;
import com.metreeca.spec.things.Values;
import com.metreeca.tray.sys.Cache;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static com.metreeca.mill._Cell.cell;
import static com.metreeca.spec.things.Values.iri;
import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.sys.Trace.clip;


/**
 * RDF extraction task.
 *
 * <p>For each feed cell focused on a IRI:</p>
 *
 * <ul>
 *
 * <li>retrieves the content of the IRI from the {@linkplain Cache#Factory network cache};</li>
 *
 * <li>parses the retrieved content as RDF; unless {@linkplain #format(String) explicitly set}, the format is
 * {@linkplain Rio#getParserFormatForFileName(String) guessed} from the IRI filename extension; gzipped content is
 * identified from the ".gz" extension and gracefully handled;</li>
 *
 * <li>generates a cell focused on the IRI and containing the parsed statements.</li>
 *
 * </ul>
 *
 * <p>Feed cells focused on blank nodes or literals are skipped with a warning.</p>
 */
public final class _RDF implements Task { // !!! rename to avoid clashed with RDF vocabulary

	private final Cache cache=tool(Cache.Factory);
	private final Trace trace=tool(Trace.Factory);

	private String base=Values.Internal;
	private RDFFormat format;

	private final ParserConfig config=new ParserConfig();


	/**
	 * Configures the base IRI for parsing RDF resources.
	 *
	 * @param base the absolute base IRI for parsing RDF resources
	 *
	 * @return this task
	 *
	 * @throws IllegalArgumentException if {@code base} is {@code null}
	 */
	public _RDF base(final String base) {

		if ( base == null ) {
			throw new NullPointerException("null base");
		}

		this.base=base;

		return this;
	}

	/**
	 * Configures the format for parsing RDF resources.
	 *
	 * @return this task
	 *
	 * @throws IllegalArgumentException if {@code format} is {@code null}
	 */
	public _RDF format(final String format) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		return format(this.format=Rio.getParserFormatForMIMEType(format)
				.orElseGet(() -> Rio.getParserFormatForMIMEType("application/"+format)
						.orElseGet(() -> Rio.getParserFormatForFileName(format)
								.orElseThrow(() -> new IllegalArgumentException("unknown format ["+format+"]")))));
	}

	public _RDF format(final RDFFormat format) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		this.format=format;

		return this;
	}


	public <T> _RDF option(final RioSetting<T> setting, final T value) {

		if ( setting == null ) {
			throw new NullPointerException("null setting");
		}

		config.set(setting, value);

		return this;
	}


	@Override public Stream<_Cell> execute(final Stream<_Cell> items) {
		return items.flatMap(cell -> {

			final Resource focus=cell.focus();
			final String iri=iri(focus);

			if ( iri != null ) {

				// guess format from IRI extension, unless set
				// !!! guess from http response headers / document

				final RDFFormat format=this.format != null ? this.format : Rio
						.getParserFormatForFileName(iri)
						.orElse(RDFFormat.TURTLE);

				final StatementCollector collector=new StatementCollector();
				final RDFParser parser=Rio.createParser(format); // multi-threading => one parser per cell

				parser.setParserConfig(config);
				parser.setRDFHandler(collector);


				// guess compresson from IRI extension
				// !!! guess from http response headers / document

				return cache.exec(iri, blob -> {
					try (
							final InputStream input=blob.input();
							final InputStream stream=iri.endsWith(".gz") ? new GZIPInputStream(input) : input;
					) {

						trace.info(this, String.format("parsing <%s> as <%s>", clip(iri), format.getName()));

						parser.parse(stream, base);

						return Stream.of(cell(focus, collector.getStatements()));

					} catch ( final IOException|RDFParseException|UnsupportedRDFormatException e ) {

						trace.error(this, String.format("unable to parse RDF from <%s>", clip(iri)), e);

						return Stream.empty();

					}
				});

			} else {

				trace.warning(this, String.format("skipping focus value <%s>", clip(Values.format(focus))));

				return Stream.empty();
			}

		});

	}

}
