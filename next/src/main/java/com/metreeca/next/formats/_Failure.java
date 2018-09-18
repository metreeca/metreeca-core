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

package com.metreeca.next.formats;

import com.metreeca.next.*;

import javax.json.Json;
import javax.json.JsonObjectBuilder;


public final class _Failure implements Format<Failure> {

	public static final _Failure Format=new _Failure();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private _Failure() {} // singleton


	@Override public void set(final Message<?> message, final Failure value) {
		message.as(Response.class).ifPresent(response -> {

			response.status(value.status());

			final JsonObjectBuilder builder=Json.createObjectBuilder();

			value.error().ifPresent(error -> builder.add("error", error));
			value.cause().ifPresent(cause -> builder.add("cause", cause));

			response.body(JSON.Format, builder.build());

		});
	}

}
