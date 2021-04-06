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

package com.metreeca.toys;

import com.metreeca.rest.handlers.Delegator;

import org.eclipse.rdf4j.model.vocabulary.*;

import static com.metreeca.json.Shape.exactly;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.*;
import static com.metreeca.json.shapes.MaxLength.maxLength;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.rest.handlers.Router.router;
import static com.metreeca.rest.operators.Relator.relator;
import static com.metreeca.rest.wrappers.Driver.driver;


public final class ProductLines extends Delegator {

	public ProductLines() {
		delegate(driver(or(relate(), role(Toys.staff)).then(

				filter(clazz(Toys.ProductLine)),

				field(RDF.TYPE, exactly(Toys.ProductLine)),

				field(RDFS.LABEL, required(), datatype(XSD.STRING), maxLength(50)),
				field(RDFS.COMMENT, required(), datatype(XSD.STRING), maxLength(750)),

				field(iri("http://schema.org/image"), optional()),

				digest(
						field(Toys.size, required(), datatype(XSD.INTEGER))
				)

		)).wrap(router()

				.path("/", router()
						.get(relator()))

				.path("/{}", router()
						.get(relator())
				)

		));
	}

}
