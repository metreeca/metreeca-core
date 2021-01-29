/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rest.wrappers;

import com.metreeca.rest.*;

import java.util.*;

import static com.metreeca.rest.Response.Unauthorized;
import static java.util.Arrays.asList;


/**
 * Role-base access controller.
 *
 * <p>Authorizes request checking that their user {@linkplain Request#roles() roles} intersect a provided set of
 * {@linkplain #controller(Object...) enabled roles}.</p>
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

				return roles.isEmpty() ? request.reply(MessageException.status(Unauthorized)) // !!! 404 under strict security
						: handler.handle(request.roles(roles));

			}
		};
	}

}
