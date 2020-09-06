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

import com.metreeca.core.*;
import com.metreeca.json.Shape;
import com.metreeca.json.probes.Optimizer;
import com.metreeca.json.probes.Redactor;
import com.metreeca.rest.assets.Engine;

import java.util.Optional;

import static com.metreeca.core.formats.TextFormat.text;
import static com.metreeca.rest.assets.Engine.shape;
import static java.lang.String.format;


/**
 * Shape-based content driver.
 *
 * <p>Drives the lifecycle of linked data resources managed by wrapped handlers with a {@linkplain #Driver(Shape)
 * shape} model:
 *
 * <ul>
 *
 * <li>{@linkplain Engine#shape() associates} the driving shape model to incoming requests;</li>
 *
 * <li>advertises the association between response focus {@linkplain Response#item() items} and the driving shape model
 * through a "{@code Link: <resource?specs>; rel=http://www.w3.org/ns/ldp#constrainedBy}" header;</li>
 *
 * <li>handles GET requests for the advertised shape model resource ({@code <resource>?specs}) with a response
 * containing a textual description of the {@link #Driver(Shape) shape} model {@linkplain Redactor redacted} taking into
 * account the {@linkplain Request#roles() roles} of the current request {@linkplain Request#user() user} and removing
 * filtering-only constraints.</li>
 *
 * </ul>
 *
 * <p>Wrapped handlers are responsible for:</p>
 *
 * <ul>
 *
 * <li>redacting the shape {@linkplain Engine#shape() associated} to incoming request according to the task to be 
 * performed;</li>
 *
 * <li>{@linkplain Engine#shape() associating} a shape to outgoing responses in order to drive further processing
 * (e.g. JSON body mapping).</li>
 *
 * </ul>
 *
 * <p><strong>Warning</strong> / Both operations must be performed taking into account the {@linkplain Request#roles()
 * roles} of the current request {@linkplain Request#user() user}: no user-related shape redaction is performed by the
 * driver wrapper on behalf of nested handlers.</p>
 *
 * @see <a href="https://www.w3.org/TR/ldp/#ldpr-resource">Linked Data Platform 1.0 - § 4.2.1.6 Resource -
 * 		ldp:constrainedBy</a>
 */
public final class Driver implements Wrapper {

	private static final String SpecsQuery="specs";
	private static final String SpecsLink="http://www.w3.org/ns/ldp#constrainedBy";


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Shape shape;


	/**
	 * Creates a content driver.
	 *
	 * @param shape the shape driving the lifecycle of the linked data resources managed by the wrapped handler
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	public Driver(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		this.shape=shape.map(new Optimizer());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return request -> specs(request).orElseGet(() ->
				handler.handle(request.map(this::before)).map(this::after)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Optional<Future<Response>> specs(final Request request) {

		// !!! handle HEAD requests on ?specs (delegate to Worker)

		return request.method().equals(Request.GET) && request.query().equals(SpecsQuery)

				? Optional.of(request.reply(response -> response
				.status(Response.OK)
				.header("Content-Type", "text/plain")
				.body(text(), shape
						.map(new Redactor(Shape.Role, request.roles()))
						.map(new Redactor(Shape.Mode, Shape.Convey))
						.toString()
				)
		))

				: Optional.empty();
	}


	private Request before(final Request request) {
		return request.attribute(shape(), shape);
	}

	private Response after(final Response response) {
		return response.request().safe() && response.success() ? response.header("+Link", format(
				"<%s?%s>; rel=%s", response.request().item(), SpecsQuery, SpecsLink
		)) : response;
	}

}
