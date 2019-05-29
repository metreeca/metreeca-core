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

import org.eclipse.rdf4j.model.IRI;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import javax.json.JsonObject;

import static com.metreeca.form.things.JsonValues.object;
import static com.metreeca.form.things.Sets.set;

import static java.util.Collections.unmodifiableSet;


/**
 * User permit.
 *
 * <p>Describes an entry in a user roster.</p>
 */
public final class Permit {

	private final String id;

	private final IRI user;
	private final Set<IRI> roles;

	private final JsonObject profile;


	/**
	 * Creates a user permit.
	 *
	 * @param id      an opaque handle uniquely identifying the user at the time the permit was created; must be
	 *                accepted as a handle for {@linkplain Roster#lookup(IRI) looking up} the user in the roster; must
	 *                change on credential and account status updates and user {@linkplain Roster#login(IRI, String)
	 *                login}/{@linkplain Roster#logout(String) logout}
	 * @param user    an IRI uniquely identifying the user
	 * @param roles   a set of IRIs uniquely identifying the roles attributed to the user
	 * @param profile a front-end profile for the user, providing information such as name, email, picture and
	 *                operational roles
	 *
	 * @throws NullPointerException if any of the arguments is null or contains null values
	 */
	public Permit(final String id, final IRI user, final Collection<IRI> roles, final JsonObject profile) {

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
		this.roles=set(roles);

		this.profile=object(profile);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the permit id.
	 *
	 * @return an opaque handle uniquely identifying the permit user at the time the permit was create
	 */
	public String id() {
		return id;
	}


	/**
	 * Retrieves the permit user.
	 *
	 * @return an IRI uniquely identifying the permit user
	 */
	public IRI user() {
		return user;
	}

	/**
	 * Retrieves the permit user roles.
	 *
	 * @return a set of IRIs uniquely identifying the roles attributed to the permit user
	 */
	public Set<IRI> roles() {
		return unmodifiableSet(roles);
	}


	/**
	 * Retrieves the permit user profile.
	 *
	 * @return a front-end profile for the permit user
	 */
	public JsonObject profile() {
		return object(profile);
	}

}
