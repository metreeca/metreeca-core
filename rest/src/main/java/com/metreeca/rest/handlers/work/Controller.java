/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.handlers.work;

import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Wrapper;

import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.metreeca.rest.Handler.refused;

import static java.util.Arrays.asList;


/**
 * Role-base access controller.
 */
public final class Controller implements Wrapper {

	private final Set<Value> roles;


	/**
	 * Creates a controller.
	 *
	 * @param roles the user {@linkplain Request#roles(Value...) roles} enabled to perform the action managed by the
	 *              wrapped handler; empty for public access; may be further restricted by role-based annotations in the
	 *              {@linkplain Request#shape() request shape}, if one is present
	 *
	 * @throws NullPointerException if either {@code roles} is null or contains null values
	 */
	public Controller(final Value... roles) {
		this(asList(roles));
	}

	/**
	 * Configures the permitted roles.
	 *
	 * @param roles the user {@linkplain Request#roles(Value...) roles} enabled to perform the action managed by the
	 *              wrapped handler; empty for public access; may be further restricted by role-based annotations in the
	 *              {@linkplain Request#shape() request shape}, if one is present
	 *
	 * @throws NullPointerException if either {@code roles} is null or contains null values
	 */
	public Controller(final Collection<? extends Value> roles) {

		if ( roles == null ) {
			throw new NullPointerException("null roles");
		}

		if ( roles.contains(null) ) {
			throw new NullPointerException("null role");
		}

		this.roles=new HashSet<>(roles);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return request -> {
			if ( roles.isEmpty() ) {

				return handler.handle(request);

			} else {

				final Collection<Value> roles=new HashSet<>(this.roles);

				roles.retainAll(request.roles());

				return roles.isEmpty() ? refused(request) : handler.handle(request.roles(roles));

			}
		};
	}

}
