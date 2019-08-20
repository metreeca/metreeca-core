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
import com.metreeca.tree.shapes.Datatype;

import static com.metreeca.tree.Shape.*;
import static com.metreeca.tree.shapes.All.all;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.MaxLength.maxLength;
import static com.metreeca.tree.shapes.Datatype.datatype;


public final class _Offices extends Delegator {

	public _Offices() {
		delegate(new Driver(role(

				"staff"

		).then(

				relate().then(
						field("label", all("Offices"))
				),

				field(GAE.Contains, and(

						clazz("Office"),

						server().then(
								field("code", and(required(), Datatype.datatype("string"))),
								field("label", and(required(), Datatype.datatype("string")))
						),

						field("name", and(required(), Datatype.datatype("string"), maxLength(75)))

				))

		)).wrap(new Router()
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
