/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.gate;

import com.metreeca.rest.Result;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;

import static java.lang.Math.abs;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonMap;


public final class RosterMock implements Roster {

	private final Map<String, String> users=new HashMap<>(); // id > secret


	@SafeVarargs public RosterMock(final Map.Entry<String, String> ...entries) {
		for (final Map.Entry<String, String> entry : entries) {

			users.put(entry.getKey(), entry.getValue());

		}
	}


	@Override public Result<Permit, String> lookup(final String handle) {
		return resolve(handle)
				.map(this::permit)
				.orElseGet(this::error);
	}


	@Override public Result<Permit, String> login(final String handle, final String secret) {
		return resolve(handle)
				.filter(entry -> entry.getValue().equals(secret))
				.map(entry -> handle(entry, secret))
				.map(this::permit)
				.orElseGet(this::error);
	}

	@Override public Result<Permit, String> login(final String handle, final String secret, final String update) {
		return resolve(handle)
				.filter(entry -> entry.getValue().equals(secret))
				.map(entry -> handle(entry, update))
				.map(this::permit)
				.orElseGet(this::error);
	}

	@Override public Result<Permit, String> logout(final String handle) {
		return resolve(handle)
				.map(entry -> handle(entry, entry.getValue()))
				.map(this::permit)
				.orElseGet(this::error);
	}


	private Optional<Map.Entry<String, String>> resolve(final String handle) {
		return users.entrySet().stream()
				.filter(entry -> handle.equals(entry.getKey()) || handle.equals(handle(entry)))
				.findFirst();
	}


	private String handle(final Map.Entry<String, String> entry) {
		return String.valueOf(abs(System.identityHashCode(entry.getValue())));
	}

	private Map.Entry<String, String> handle(final Map.Entry<String, String> entry, final String secret) {

		entry.setValue(new String(secret.getBytes(UTF_8), UTF_8)); // a unique secret whose id hash is the handle

		return entry;

	}


	private Result<Permit, String> permit(final Map.Entry<String, String> entry) {
		return Value(new Permit(handle(entry),
				"/users/"+entry.getKey(), emptySet(),
				singletonMap("user", entry.getKey())
		));
	}

	private Result<Permit, String> error() {
		return Error(CredentialsInvalid);
	}

}
