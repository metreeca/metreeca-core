/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.gate;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

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

	private final String handle;
	private final String digest;

	private final IRI user;
	private final Set<Value> roles;

	private final JsonObject profile;


	/**
	 * Creates a user permit.
	 *
	 * @param handle  the opaque handle used to identify the user in the roster
	 * @param digest  an opaque value uniquely identifying the state of the user at the time the permit was created;
	 *                must change on credential and account status updates
	 * @param user    an IRI uniquely identifying the user
	 * @param roles   a set of values uniquely identifying the roles attributed to the user
	 * @param profile a profile for the user, including information useful for handling front-end activities, such as
	 *                name, picture and operational roles
	 *
	 * @throws NullPointerException if any of the arguments is null or contains null values
	 */
	public Permit(
			final String handle, final String digest,
			final IRI user, final Collection<Value> roles,
			final JsonObject profile
	) {

		if ( digest == null ) {
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

		this.handle=handle;
		this.digest=digest;

		this.user=user;
		this.roles=set(roles);

		this.profile=object(profile);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the permit handle.
	 *
	 * @return the opaque handle used to identify the permit user in the roster
	 */
	public String handle() {
		return handle;
	}

	/**
	 * Retrieves the permit digest.
	 *
	 * @return an opaque value uniquely identifying the state of the user at the time the permit was create; will change
	 * on credential and account status updates
	 */
	public String digest() {
		return digest;
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
	 * @return a set of values uniquely identifying the roles attributed to the permit user
	 */
	public Set<Value> roles() {
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
