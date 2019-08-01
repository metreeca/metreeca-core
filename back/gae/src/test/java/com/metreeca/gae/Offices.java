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

package com.metreeca.gae;

import com.metreeca.rest.handlers.*;
import com.metreeca.rest.wrappers.Driver;

import static com.metreeca.tree.Shape.*;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.MaxLength.maxLength;
import static com.metreeca.tree.shapes.Type.type;


public final class Offices extends Delegator {

	public Offices() {
		delegate(new Driver(role(

				"staff"

		).then(relate().then(

				field("label", "Offices"),

				field("contains", and(

						server().then(
								field("code", and(required(), type("string"))),
								field("label", and(required(), type("string")))
						),

						field("name", and(required(), type("string"), maxLength(75)))

				))

		))).wrap(new Router()
				.path("/", new Worker()
						.get(new Relator())
						.post(new Creator())
				)
				.path("/{id}", new Worker()
						.get(new Relator())
						.put(new Updater())
						.delete(new Deleter())
				)
		));
	}

}
