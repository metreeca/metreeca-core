/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
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

	private final String hash;

	private final IRI user;
	private final Set<IRI> roles;

	private final JsonObject profile;


	/**
	 * Creates a user permit.
	 *
	 * @param hash  an opaque value uniquely identifying the state of the user at the time the permit was created;
	 *                must change on credential and account status updates
	 * @param user    an IRI uniquely identifying the user
	 * @param roles   a set of IRIs uniquely identifying the roles attributed to the user
	 * @param profile a profile for the user, including information useful for handling front-end activities, such as
	 *                name, picture and operational roles
	 *
	 * @throws NullPointerException if any of the arguments is null or contains null values
	 */
	public Permit(
			final String hash,
			final IRI user, final Collection<IRI> roles,
			final JsonObject profile
	) {

		if ( hash == null ) {
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

		this.hash=hash;

		this.user=user;
		this.roles=set(roles);

		this.profile=object(profile);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the permit hash.
	 *
	 * @return an opaque value uniquely identifying the state of the user at the time the permit was create; will change
	 * on credential and account status updates
	 */
	public String hash() {
		return hash;
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
	 * @return a profile for the permit user, including information useful for handling front-end activities, such as
	 * name, picture and operational roles
	 */
	public JsonObject profile() {
		return object(profile);
	}

}
