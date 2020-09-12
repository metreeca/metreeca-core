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

import com.metreeca.json.shapes.Guard;
import com.metreeca.rest.Message;
import com.metreeca.rest.Request;
import com.metreeca.rest.assets.Engine;

import static com.metreeca.rest.Wrapper.wrapper;


/**
 * Model-driven resource updater.
 *
 * <p>Performs:</p>
 *
 * <ul>
 *
 * <li>{@linkplain Guard#Role role}-based request shape redaction and shape-based
 * {@linkplain Actor#throttler(Object, Object...) authorization}, considering shapes enabled by the
 * {@linkplain Guard#Update} task and the {@linkplain Guard#Target} area, when operating on
 * {@linkplain Request#collection() collections}, or the {@linkplain Guard#Detail} area, when operating on other
 * resources;</li>
 *
 * <li>engine-assisted request payload {@linkplain Engine#validate(Message) validation};</li>
 *
 * <li>engine assisted resource {@linkplain Engine#update(Request) updating}.</li>
 *
 * </ul>
 *
 * <p>All operations are executed inside a single {@linkplain Engine#exec(Runnable) engine transaction}.</p>
 */
public final class Updater extends Actor {

	/**
	 * Creates a resource updater.
	 *
	 * @return a new resource updater
	 */
	public static Updater updater() {
		return new Updater();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Updater() {
		delegate(_updater()

				.with(connector())
				.with(wrapper(Request::collection,
						throttler(Guard.Update, Guard.Target),
						throttler(Guard.Update, Guard.Detail)
				))
				.with(validator())

		);

	}

}
