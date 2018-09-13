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

package com.metreeca.rest.services;

import com.metreeca.form.Form;
import com.metreeca.rest.*;
import com.metreeca.tray.iam.Roster;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import javax.json.JsonException;

import static com.metreeca.rest.Handler.error;
import static com.metreeca.rest.handlers.Dispatcher.dispatcher;
import static com.metreeca.form.things.JSON.field;
import static com.metreeca.form.things.JSON.object;
import static com.metreeca.tray._Tray.tool;

import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;


/**
 * Basic session manager (work in progress…).
 *
 * <p>Manages session lifecycle interaction with the shared {@link Roster#Factory roster} tool.</p>
 */
public final class Session implements Service {

	private static final String TicketMalformed="ticket-malformed";


	private final Index tool=tool(Index.Factory);
	private final Roster roster=tool(Roster.Factory);


	@Override public void load() {
		tool.insert("/~", dispatcher()

				.post(this::evolve));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void evolve(final Request request, final Response response) {
		try {

			if ( !handle(request, response, request.json()) ) {
				response.status(Response.BadRequest).json(error(TicketMalformed));
			}

		} catch ( final JsonException e ) {
			response.status(Response.BadRequest).json(error(TicketMalformed, e.getMessage()));
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private boolean handle(final Request request, final Response response, final Object payload) {
		return payload instanceof Map && handle(request, response, (Map<?, ?>)payload);
	}

	private boolean handle(final Request request, final Response response, final Map<?, ?> payload) {
		return extend(request, response, payload)
				|| change(request, response, payload)
				|| update(request, response, payload);
	}

	private boolean extend(final Request request, final Response response, final Map<?, ?> payload) {
		if ( validate(payload) ) {

			final IRI user=request.user();

			if ( user.equals(Form.none) ) {

				response.status(Response.OK).json(object());

			} else {

				final Roster.Permit permit=roster.refresh(user.stringValue());

				if ( permit.valid(currentTimeMillis()) ) {

					response.status(Response.OK).json(ticket(permit));

				} else {

					response.status(Response.Forbidden).json(error(permit.issue()));

				}
			}

			return true;

		} else {

			return false;

		}
	}

	private boolean change(final Request request, final Response response, final Map<?, ?> payload) {
		if ( validate(payload, "usr", "pwd") ) {

			final String usr=(String)payload.get("usr");
			final String pwd=(String)payload.get("pwd");

			final IRI current=request.user();

			if ( usr.isEmpty() && pwd.isEmpty() ) { // logout

				roster.release(current.stringValue());

				response.status(Response.OK).json(object());

			} else { // switch user

				final Roster.Permit permit=roster.acquire(usr, pwd);

				if ( permit.valid(currentTimeMillis()) ) {

					if ( !current.equals(permit.user()) ) {
						roster.release(current.stringValue());
					}

					response.status(Response.OK).json(ticket(permit));

				} else {

					response.status(Response.Forbidden).json(error(permit.issue()));

				}

			}

			return true;

		} else {

			return false;

		}
	}

	private boolean update(final Request request, final Response response, final Map<?, ?> payload) {
		if ( validate(payload, "usr", "old", "new") ) {

			final String usr=(String)payload.get("usr");
			final String old=(String)payload.get("old");
			final String neu=(String)payload.get("new");

			final IRI current=request.user();

			// switch and activate user

			final Roster.Permit permit=roster.acquire(usr, old, neu);

			if ( permit.valid(currentTimeMillis()) ) {

				if ( !current.equals(permit.user()) ) {
					roster.release(current.stringValue());
				}

				response.status(Response.OK).json(ticket(permit));

			} else {

				response.status(Response.Forbidden).json(error(permit.issue()));

			}

			return true;

		} else {

			return false;

		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private boolean validate(final Map<?, ?> payload, final String... fields) {
		return payload.keySet().equals(new HashSet<>(asList(fields)))
				&& Arrays.stream(fields).allMatch(field -> payload.get(field) instanceof String);
	}

	private Map<String, Object> ticket(final Roster.Permit permit) { // !!! review user/roles reporting
		return object(
				field("label", permit.label()),
				field("token", permit.token()),
				field("lease", permit.expiry()),
				field("roles", permit.roles().stream().map(Value::stringValue).collect(toList()))
		);
	}

}
