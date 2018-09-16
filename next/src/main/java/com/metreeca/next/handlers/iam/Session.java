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

package com.metreeca.next.handlers.iam;

import com.metreeca.next.*;
import com.metreeca.next.Origin;
import com.metreeca.next.handlers.Dispatcher;
import com.metreeca.tray.iam.Roster;

import org.eclipse.rdf4j.model.Value;

import java.util.Arrays;
import java.util.HashSet;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;

import static com.metreeca.tray.Tray.tool;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;


/**
 * Basic session manager.
 *
 * <p>Manages session lifecycle interaction with the shared {@link Roster#Factory roster} tool.</p>
 *
 * @deprecated Work in progress
 */
@Deprecated final class Session implements Handler {

	private static final String TicketMalformed="ticket-malformed";


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Roster roster=tool(Roster.Factory);

	private final Dispatcher delegate=new Dispatcher().post(this::evolve);


	@Override public Origin<Response> handle(final Request request) {
		return delegate.handle(request);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Origin<Response> evolve(final Request request) {

		throw new UnsupportedOperationException("to be implemented"); // !!! tbi

		//try {
		//
		//	if ( !handle(request, response, request.json()) ) {
		//		response.status(Response.BadRequest).body(JSON.Format, error(TicketMalformed));
		//	}
		//
		//} catch ( final JsonException e ) {
		//	response.status(Response.BadRequest).body(JSON.Format, error(TicketMalformed, e.getMessage()));
		//}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//private boolean handle(final Request request, final Response response, final Object payload) {
	//	return payload instanceof Map && handle(request, response, (Map<?, ?>)payload);
	//}
	//
	//private boolean handle(final Request request, final Response response, final Map<?, ?> payload) {
	//	return extend(request, response, payload)
	//			|| change(request, response, payload)
	//			|| update(request, response, payload);
	//}
	//
	//private boolean extend(final Request request, final Response response, final Map<?, ?> payload) {
	//	if ( validate(payload) ) {
	//
	//		final IRI user=request.user();
	//
	//		if ( user.equals(Form.none) ) {
	//
	//			response.status(Response.OK).body(JSON.Format, object());
	//
	//		} else {
	//
	//			final Roster.Permit permit=roster.refresh(user.stringValue());
	//
	//			if ( permit.valid(currentTimeMillis()) ) {
	//
	//				response.status(Response.OK).body(JSON.Format, ticket(permit));
	//
	//			} else {
	//
	//				response.status(Response.Forbidden).body(JSON.Format, Handler.error(permit.issue()));
	//
	//			}
	//		}
	//
	//		return true;
	//
	//	} else {
	//
	//		return false;
	//
	//	}
	//}
	//
	//private boolean change(final Request request, final Response response, final Map<?, ?> payload) {
	//	if ( validate(payload, "usr", "pwd") ) {
	//
	//		final String usr=(String)payload.get("usr");
	//		final String pwd=(String)payload.get("pwd");
	//
	//		final IRI current=request.user();
	//
	//		if ( usr.isEmpty() && pwd.isEmpty() ) { // logout
	//
	//			roster.release(current.stringValue());
	//
	//			response.status(Response.OK).body(JSON.Format, object());
	//
	//		} else { // switch user
	//
	//			final Roster.Permit permit=roster.acquire(usr, pwd);
	//
	//			if ( permit.valid(currentTimeMillis()) ) {
	//
	//				if ( !current.equals(permit.user()) ) {
	//					roster.release(current.stringValue());
	//				}
	//
	//				response.status(Response.OK).body(JSON.Format, ticket(permit));
	//
	//			} else {
	//
	//				response.status(Response.Forbidden).body(JSON.Format, Handler.error(permit.issue()));
	//
	//			}
	//
	//		}
	//
	//		return true;
	//
	//	} else {
	//
	//		return false;
	//
	//	}
	//}
	//
	//private boolean update(final Request request, final Response response, final Map<?, ?> payload) {
	//	if ( validate(payload, "usr", "old", "new") ) {
	//
	//		final String usr=(String)payload.get("usr");
	//		final String old=(String)payload.get("old");
	//		final String neu=(String)payload.get("new");
	//
	//		final IRI current=request.user();
	//
	//		// switch and activate user
	//
	//		final Roster.Permit permit=roster.acquire(usr, old, neu);
	//
	//		if ( permit.valid(currentTimeMillis()) ) {
	//
	//			if ( !current.equals(permit.user()) ) {
	//				roster.release(current.stringValue());
	//			}
	//
	//			response.status(Response.OK).body(JSON.Format, ticket(permit));
	//
	//		} else {
	//
	//			response.status(Response.Forbidden).body(JSON.Format, Handler.error(permit.issue()));
	//
	//		}
	//
	//		return true;
	//
	//	} else {
	//
	//		return false;
	//
	//	}
	//}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private boolean validate(final JsonObject payload, final String... fields) {
		return payload.keySet().equals(new HashSet<>(asList(fields)))
				&& Arrays.stream(fields).allMatch(field -> payload.get(field) instanceof JsonString);
	}

	private JsonObject ticket(final Roster.Permit permit) { // !!! review user/roles reporting
		return Json.createObjectBuilder()

				.add("label", permit.label())
				.add("token", permit.token())
				.add("lease", permit.expiry())
				.add("roles", Json.createArrayBuilder(permit
						.roles()
						.stream()
						.map(Value::stringValue)
						.collect(toList())
				).build())

				.build();
	}

}
