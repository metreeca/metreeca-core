/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

import com.metreeca.rest.Request;

import static com.metreeca.rest.Wrapper.wrapper;
import static com.metreeca.tree.Shape.*;


/**
 * Model-driven resource relator.
 */
public final class Relator extends Actor { // !!! tbd

	public Relator() {
		delegate(relator()

				.with(connector())
				.with(trimmer())
				.with(wrapper(Request::collection,
						throttler(Relate, Holder, Digest),
						throttler(Relate, Detail)
				))

		);
	}

}