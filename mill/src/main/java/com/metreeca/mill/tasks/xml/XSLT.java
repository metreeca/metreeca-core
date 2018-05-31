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

import javax.xml.transform.Source;

import static com.metreeca.tray.Tray.tool;


/**
 * XSLT processing tasks.
 */
public final class XSLT extends XML<XSLT> {

	private final Saxon saxon=tool(Saxon.Tool);


	@Override protected XSLT self() {
		return this;
	}


	@Override protected Function<Source, Stream<? extends XdmValue>> processor(final String transform) {

		final XsltExecutable xslt=saxon.xslt(transform);

		return source -> {
			try {

				final XsltTransformer transformer=xslt.load();
				final XdmDestination destination=new XdmDestination();

				transformer.setSource(source);
				transformer.setDestination(destination);

				transformer.transform();

				return Stream.of(destination.getXdmNode());

			} catch ( final SaxonApiException e ) {
				throw new SaxonApiUncheckedException(e);
			}
		};
	}

}
