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

package com.metreeca.link.handlers.ldp;

import com.metreeca.link.*;
import com.metreeca.spec.Shape;

import static com.metreeca.link.handlers.Dispatcher.dispatcher;
import static com.metreeca.link.handlers.shape.Deleter.deleter;
import static com.metreeca.link.handlers.shape.Relator.relator;
import static com.metreeca.link.handlers.shape.Updater.updater;
import static com.metreeca.link.wrappers.Inspector.inspector;
import static com.metreeca.spec.things.Values.format;


/**
 * Model-driven LDP RDF Source handler.
 *
 * <p>Manages read/write operations for individual LDP RDF sources under the control of a linked data {@linkplain Shape
 * shape}.</p>
 *
 * @see <a href="https://www.w3.org/TR/ldp/#ldprs">Linked Data Platform 1.0 - §4.3 RDF Source</a>
 */
public final class RDFSource implements Handler {

	public static RDFSource resource() { return new RDFSource(); }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Handler dispatcher;


	/**
	 * Configures the linked data shape for this handler.
	 *
	 * @param shape the shape driving read/write operations for LDP RDF sources managed by this handler
	 *
	 * @return this handler
	 *
	 * @throws NullPointerException if {@code shape} is {@code null}
	 */
	public RDFSource shape(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		this.dispatcher=dispatcher()

				.get(relator(shape))
				.put(updater(shape))
				.delete(deleter(shape))

				.wrap(inspector().shape(shape));

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void handle(final Request request, final Response response) {
		dispatcher.handle(

				writer -> writer.copy(request)

						.done(),

				reader -> response.copy(reader)

						.header("Link",

								format(Link.ShapedResource)+"; rel=\"type\"",
								"<http://www.w3.org/ns/ldp#RDFSource>; rel=\"type\"",
								"<http://www.w3.org/ns/ldp#Resource>; rel=\"type\"")

						.done()

		);
	}

}

