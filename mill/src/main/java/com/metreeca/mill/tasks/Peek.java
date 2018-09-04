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

package com.metreeca.mill.tasks;


import com.metreeca.mill.Task;
import com.metreeca.mill._Cell;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;

import java.io.StringWriter;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.form.things.Values.format;
import static com.metreeca.tray.Tray.tool;


/**
 * Feed peeking task.
 *
 * <p>Dumps the feed to the {@linkplain Trace#Factory execution trace}.</p>
 */
public final class Peek implements Task {

	private final Trace trace=tool(Trace.Factory);

	private final Function<_Cell, String> mapper;


	public Peek(final Namespace... namespaces) {
		this(cell -> {

			final StringWriter writer=new StringWriter();
			final RDFWriter turtle=Rio.createWriter(RDFFormat.TURTLE, writer);

			// ;(TurtleWriter) ignores BasicParserSettings.NAMESPACES configuration setting

			for (final Namespace namespace : namespaces) {
				turtle.handleNamespace(namespace.getPrefix(), namespace.getName());
			}

			Rio.write(cell.model(), turtle);

			return writer.toString();

		});
	}

	public Peek(final Function<_Cell, String> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		this.mapper=mapper;
	}


	@Override public Stream<_Cell> execute(final Stream<_Cell> items) {
		return items.peek(cell -> {

			final String focus=format(cell.focus());
			final String model=mapper.apply(cell).trim();

			trace.info(this, focus
					+(model.isEmpty() ? "" : model.contains("\n") ? "\n\n"+model+"\n" : " / "+model));

		});
	}

}
