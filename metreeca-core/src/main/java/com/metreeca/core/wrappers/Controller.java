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

package com.metreeca.core.wrappers;

import com.metreeca.core.*;

import java.util.*;

import static com.metreeca.core.MessageException.status;
import static com.metreeca.core.Response.Unauthorized;
import static java.util.Arrays.asList;


/**
 * Role-base access controller.
 *
 * <p>Authorizes request checking that their user {@linkplain Request#roles() roles} intersect a provided set of
 * {@linkplain #Controller(Collection) enabled roles}.</p>
 */
public final class Controller implements Wrapper {

	/**
	 * Creates a controller.
	 *
	 * @param roles the user {@linkplain Request#roles(Object...) roles} enabled to perform the action managed by the
	 *              wrapped handler; empty for public access
	 *
	 * @return a new controller
	 *
	 * @throws NullPointerException if {@code roles} is null or contains null values
	 */
	public static Controller controller(final Object... roles) {
		return new Controller(asList(roles));
	}

	/**
	 * Creates a controller.
	 *
	 * @param roles the user {@linkplain Request#roles(Collection) roles} enabled to perform the action managed by the
	 *              wrapped handler; empty for public access
	 *
	 * @return a new controller
	 *
	 * @throws NullPointerException if {@code roles} is null or contains null values
	 */
	public static Controller controller(final Collection<Object> roles) {

		if ( roles == null || roles.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null roles");
		}

		return new Controller(roles);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Set<Object> roles;


	private Controller(final Collection<Object> roles) {
		this.roles=new HashSet<>(roles);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return request -> {
			if ( roles.isEmpty() ) {

				return handler.handle(request);

			} else {

				final Collection<Object> roles=new HashSet<>(this.roles);

				roles.retainAll(request.roles());

				return roles.isEmpty() ? request.reply(status(Unauthorized)) // !!! 404 under strict security
						: handler.handle(request.roles(roles));

			}
		};
	}

}
