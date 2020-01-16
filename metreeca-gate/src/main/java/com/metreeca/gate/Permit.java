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

package com.metreeca.gate;

import java.util.*;

import javax.json.Json;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;


/**
 * User permit.
 *
 * <p>Describes an entry in a user roster.</p>
 */
public final class Permit {

	private final String id;

	private final String user;
	private final Set<Object> roles;

	private final Map<String, Object> profile;


	/**
	 * Creates a user permit.
	 *
	 * @param id      an opaque handle uniquely identifying the user at the time the permit was created; must be
	 *                accepted as a handle for {@linkplain Roster#lookup(String) looking up} the user in the roster;
	 *                must change on credential and account status updates and user {@linkplain Roster#login(String,
	 *                String) login}/{@linkplain Roster#logout(String) logout}
	 * @param user    an id uniquely identifying the user
	 * @param roles   a set of ids uniquely identifying the roles attributed to the user
	 * @param profile a front-end profile for the user, providing information such as name, email, picture and
	 *                operational roles; usually {@linkplain Json#createObjectBuilder(Map)} converted} to JSON before
	 *                use
	 *
	 * @throws NullPointerException if any of the arguments is null or contains null values
	 */
	public Permit(final String id, final String user, final Collection<Object> roles, final Map<String, Object> profile) {

		if ( id == null ) {
			throw new NullPointerException("null hash");
		}

		if ( user == null ) {
			throw new NullPointerException("null user");
		}

		if ( roles == null || roles.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null roles");
		}

		if ( profile == null ) {
			throw new NullPointerException("null profile");
		}

		this.id=id;

		this.user=user;
		this.roles=new LinkedHashSet<>(roles);

		this.profile=new LinkedHashMap<>(profile);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the permit id.
	 *
	 * @return an opaque handle uniquely identifying the permit user at the time the permit was created
	 */
	public String id() {
		return id;
	}


	/**
	 * Retrieves the permit user.
	 *
	 * @return an id uniquely identifying the permit user
	 */
	public String user() {
		return user;
	}

	/**
	 * Retrieves the permit user roles.
	 *
	 * @return a set of ids uniquely identifying the roles attributed to the permit user
	 */
	public Set<Object> roles() {
		return unmodifiableSet(roles);
	}


	/**
	 * Retrieves the permit user profile.
	 *
	 * @return a front-end profile for the permit user
	 */
	public Map<String, Object> profile() {
		return unmodifiableMap(profile);
	}

}
