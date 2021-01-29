/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
