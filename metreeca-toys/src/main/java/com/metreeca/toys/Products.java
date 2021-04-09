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

import static com.metreeca.json.Values.literal;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.*;
import static com.metreeca.json.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.json.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.json.shapes.MaxLength.maxLength;
import static com.metreeca.json.shapes.MinExclusive.minExclusive;
import static com.metreeca.json.shapes.MinInclusive.minInclusive;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.Pattern.pattern;
import static com.metreeca.rdf4j.services.Graph.update;
import static com.metreeca.rest.Toolbox.text;
import static com.metreeca.rest.Wrapper.postprocessor;
import static com.metreeca.rest.handlers.Router.router;
import static com.metreeca.rest.operators.Creator.creator;
import static com.metreeca.rest.operators.Deleter.deleter;
import static com.metreeca.rest.operators.Relator.relator;
import static com.metreeca.rest.operators.Updater.updater;
import static com.metreeca.rest.wrappers.Driver.driver;

public final class Products extends Delegator {

	public Products() {
		delegate(driver(or(relate(), role(Toys.staff)).then(

				filter(clazz(Toys.Product)),

				field(RDF.TYPE, exactly(Toys.Product)),

				field(RDFS.LABEL, required(), datatype(XSD.STRING), maxLength(50)),
				field(RDFS.COMMENT, required(), datatype(XSD.STRING), maxLength(500)),

				server(field(Toys.code, required())),

				field(Toys.line, required(), convey(clazz(Toys.ProductLine)),

						relate(field(RDFS.LABEL, required()))

				),

				field(Toys.scale, required(),
						datatype(XSD.STRING),
						pattern("1:[1-9][0-9]{1,2}")
				),

				field(Toys.vendor, required(),
						datatype(XSD.STRING),
						maxLength(50)
				),

				field("price", Toys.sell, required(),
						datatype(XSD.DECIMAL),
						minExclusive(literal(0.0)),
						maxExclusive(literal(1000.0))
				),

				role(Toys.staff).then(field(Toys.buy, required(),
						datatype(XSD.DECIMAL),
						minInclusive(literal(0.0)),
						maxInclusive(literal(1000.0))
				)),

				server().then(field(Toys.stock, required(),
						datatype(XSD.INTEGER),
						minInclusive(literal(0)),
						maxExclusive(literal(10_000))
				))

		)).wrap(router()

				.path("/", router()
						.get(relator())
						.post(creator()
								.slug(new ProductsSlug())
								.with(postprocessor(update(text(Products.class, "ProductsCreate.ql"))))
						)
				)

				.path("/*", router()
						.get(relator())
						.put(updater())
						.delete(deleter())
				)

		));
	}

}
