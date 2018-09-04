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

package com.metreeca.rest;


/**
 * Linked data resource handler wrapper.
 *
 * <p>Inspects and possibly alters incoming {@linkplain Request requests} and outgoing {@linkplain Response responses}
 * processed by resource {@linkplain Handler handlers}.</p>
 */
@FunctionalInterface public interface Wrapper {

	/**
	 * Creates an identity wrapper.
	 *
	 * @return return an identity wrapper, that is a wrapper whose {@linkplain #wrap(Handler) handler} and {@linkplain
	 * #wrap(Wrapper) wrapper} wrap methods return their unchanged argument
	 */
	public static Wrapper wrapper() {
		return new Wrapper() {

			@Override public Wrapper wrap(final Wrapper wrapper) { return wrapper; }

			@Override public Handler wrap(final Handler handler) { return handler; }

		};
	}


	/**
	 * Wraps a handler.
	 *
	 * @param handler the handler to be wrapped
	 *
	 * @return the combined handler obtained by wrapping this wrapper around {@code handler}
	 */
	public Handler wrap(final Handler handler);

	/**
	 * Wraps a wrapper.
	 *
	 * @param wrapper the handler to be wrapped
	 *
	 * @return the combined wrapper obtained by wrapping this wrapper around {@code wrapper}
	 *
	 * @throws NullPointerException if {@code wrapper} is {@code null}
	 */
	public default Wrapper wrap(final Wrapper wrapper) {

		if ( wrapper == null ) {
			throw new NullPointerException("null wrapper");
		}

		return handler -> wrap(wrapper.wrap(handler));
	}

}
