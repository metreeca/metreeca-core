/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.next;

@FunctionalInterface public interface Wrapper {

	public static Wrapper wrapper() { // identity wrapper
		return new Wrapper() {

			@Override public Wrapper wrap(final Wrapper wrapper) { return wrapper; }

			@Override public Handler wrap(final Handler handler) { return handler; }

		};
	}


	public Handler wrap(final Handler handler);

	public default Wrapper wrap(final Wrapper wrapper) {

		if ( wrapper == null ) {
			throw new NullPointerException("null wrapper");
		}

		return handler -> wrap(wrapper.wrap(handler));
	}

}
