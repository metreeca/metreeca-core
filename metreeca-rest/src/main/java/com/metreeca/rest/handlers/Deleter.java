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

package com.metreeca.rest.handlers;

import com.metreeca.json.Shape;
import com.metreeca.rest.Request;
import com.metreeca.rest.assets.Engine;

import static com.metreeca.rest.Wrapper.wrapper;


/**
 * Model-driven resource deleter.
 *
 * <p>Performs:</p>
 *
 * <ul>
 *
 * <li>shape-based {@linkplain Actor#throttler(Object, Object...) authorization}, considering shapes enabled by the
 * {@linkplain Shape#Delete} task and the {@linkplain Shape#Holder} area, when operating on
 * {@linkplain Request#collection() collections}, or the {@linkplain Shape#Detail} area, when operating on other
 * resources;</li>
 *
 * <li>engine assisted resource {@linkplain Engine#delete(Request) deletion}.</li>
 *
 * </ul>
 *
 * <p>All operations are executed inside a single {@linkplain Engine#exec(Runnable) engine transaction}.</p>
 */
public final class Deleter extends Actor {

	/**
	 * Creates a resource deleter.
	 *
	 * @return a new resource deleter
	 */
	public static Deleter deleter() {
		return new Deleter();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Deleter() {
		delegate(_deleter()

				.with(connector())
				.with(wrapper(Request::collection,
						throttler(Shape.Delete, Shape.Holder),
						throttler(Shape.Delete, Shape.Detail)
				))

		);
	}

}
