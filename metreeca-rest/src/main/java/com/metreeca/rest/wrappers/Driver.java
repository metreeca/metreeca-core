/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.wrappers;

import com.metreeca.json.Shape;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.JSONLDFormat;

import static com.metreeca.json.shapes.And.and;


/**
 * Shape-based content driver.
 *
 * <p>Drives the lifecycle of linked data resources managed by wrapped handlers with a {@linkplain #driver(Shape...)
 * shape} model:
 *
 * <ul>
 *
 * <li>{@linkplain JSONLDFormat#shape() associates} the driving shape model to incoming requests;</li>
 *
 * </ul>
 *
 * <p>Wrapped handlers are responsible for:</p>
 *
 * <ul>
 *
 * <li>redacting the shape {@linkplain JSONLDFormat#shape() associated} to incoming request according to the task to be
 * performed;</li>
 *
 * <li>{@linkplain JSONLDFormat#shape() associating} a shape to outgoing responses in order to drive further processing
 * (e.g. JSON body mapping).</li>
 *
 * </ul>
 *
 * <p><strong>Warning</strong> / Both operations must be performed taking into account the {@linkplain Request#roles()
 * roles} of the current request {@linkplain Request#user() user}: no user-related shape redaction is performed by the
 * driver wrapper on behalf of nested handlers.</p>
 */
public final class Driver implements Wrapper {

	/**
	 * Creates a content driver.
	 *
	 * @param shapes the shapes driving the lifecycle of the linked data resources managed by the wrapped handler
	 *
	 * @return a new shape-based content driver
	 *
	 * @throws NullPointerException if {@code shape} is null of ccntains null elements
	 */
	public static Driver driver(final Shape... shapes) {
		return new Driver(and(shapes));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Shape shape;


	private Driver(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		this.shape=shape;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return request -> handler.handle(request.attribute(JSONLDFormat.shape(), shape));
	}

}
