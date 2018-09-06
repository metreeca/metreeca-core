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

package com.metreeca.mill.tasks.xml;


import com.metreeca.tray.xml.Saxon;

import net.sf.saxon.s9api.*;

import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.xml.transform.Source;

import static com.metreeca.tray._Tray.tool;


/**
 * XQuery processing tasks.
 */
public final class XQuery extends XML<XQuery> {

	private final Saxon saxon=tool(Saxon.Tool);


	@Override protected XQuery self() {
		return this;
	}

	@Override protected Function<Source, Stream<? extends XdmValue>> processor(final String transform) {

		final XQueryExecutable xquery=saxon.xquery(transform);

		return source -> {
			try {

				final XQueryEvaluator evaluator=xquery.load();

				evaluator.setSource(source);

				return StreamSupport.stream(evaluator.spliterator(), false);

			} catch ( final SaxonApiException e ) {
				throw new SaxonApiUncheckedException(e);
			}
		};
	}

}
