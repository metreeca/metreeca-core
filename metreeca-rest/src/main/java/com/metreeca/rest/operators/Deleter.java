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

package com.metreeca.rest.operators;

import com.metreeca.json.shapes.Guard;
import com.metreeca.rest.Request;
import com.metreeca.rest.assets.Engine;
import com.metreeca.rest.handlers.Delegator;

import static com.metreeca.json.shapes.Guard.*;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.Wrapper.wrapper;
import static com.metreeca.rest.assets.Engine.engine;
import static com.metreeca.rest.assets.Engine.throttler;


/**
 * Model-driven resource deleter.
 *
 * <p>Performs:</p>
 *
 * <ul>
 *
 * <li>shape-based {@linkplain Engine#throttler(Object, Object...) authorization}, considering shapes enabled by the
 * {@linkplain Guard#Delete} task and the {@linkplain Guard#Target} area, when operating on
 * {@linkplain Request#collection() collections}, or the {@linkplain Guard#Detail} area, when operating on other
 * resources;</li>
 *
 * <li>engine assisted resource {@linkplain Engine#delete(Request) deletion}.</li>
 *
 * </ul>
 *
 * <p>All operations are executed inside a single {@linkplain Engine engine transaction}.</p>
 */
public final class Deleter extends Delegator {

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

		final Engine engine=asset(engine());

		delegate(engine.wrap(engine::delete)

				.with(wrapper(Request::collection,
						throttler(Delete, Target),
						throttler(Delete, Detail)
				))

		);
	}

}
