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

package com.metreeca.rest.handlers.shape;


import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.things.ValuesTest;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.rest.RestTest;
import com.metreeca.rest.formats.RDFFormat;
import com.metreeca.rest.formats.ShapeFormat;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.things.ValuesTest.*;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


final class RelatorTest {

	private Tray tray() {
		return new Tray().exec(RestTest.dataset(small()));
	}

	private Request direct() {
		return new Request()
				.method(Request.GET)
				.base(ValuesTest.Base)
				.path("/employees/1370");
	}

	private Request shaped() {
		return direct()
				.roles(ValuesTest.Manager)
				.body(ShapeFormat.shape()).set(ValuesTest.Employee);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testDirectRelate() {
		tray()

				.get(Relator::new)

				.handle(direct())

				.accept(response -> {

					final Optional<Model> rdfBody=response.body(RDFFormat.rdf()).get().value().map(LinkedHashModel::new);
					final Optional<Shape> shapeBody=response.body(ShapeFormat.shape()).get().value();

					assertFalse(shapeBody.isPresent(), "response shape body omitted");
					assertTrue(rdfBody.isPresent(), "response RDF body included");

					assertTrue(
							rdfBody.get().contains(response.item(), null, null),
							"response RDF body contains a resource description"
					);

				});
	}

	@Test void testDirectUnknown() {
		tray()

				.get(Relator::new)

				.handle(direct().path("/employees/9999"))

				.accept(response -> assertEquals(Response.NotFound, response.status()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testShapedRelate() {

		final Tray tray=tray();
		final Graph graph=tray.get(Graph.Factory);

		tray.get(Relator::new)

				.handle(shaped())

				.accept(response -> graph.query(connection -> {

					assertEquals(Response.OK, response.status());

					final Model expected=construct(connection,
							"construct where { <employees/1370> a :Employee; :code ?c; :seniority ?s }");

					response.body(RDFFormat.rdf()).get().handle(
							model -> assertSubset("items retrieved", expected, model),
							error -> fail("missing RDF body")
					);

				}));
	}

	@Test void testShapedRelateLimited() {

		final Tray tray=tray();
		final Graph graph=tray.get(Graph.Factory);

		tray.get(Relator::new)

				.handle(shaped().roles(ValuesTest.Salesman))

				.accept(response -> graph.query(connection -> {

					assertEquals(Response.OK, response.status());

					final Model expected=construct(connection,
							"construct where { <employees/1370> a :Employee; :code ?c }");

					response.body(RDFFormat.rdf()).get().handle(
							model -> {
								assertSubset("items retrieved", expected, model);
								assertTrue(
										new LinkedHashModel(model).filter(null, term("seniority"), null).isEmpty(), // !!! unwrap
										"properties restricted to manager role not included"
								);
							},
							error -> fail("missing RDF body")
					);

				}));
	}


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testShapedForbidden() {
		tray()

				.get(Relator::new)

				.handle(shaped().body(ShapeFormat.shape()).set(or()))

				.accept(response -> assertEquals(Response.Forbidden, response.status()));
	}

	@Test void testShapedUnauthorized() {
		tray()

				.get(Relator::new)

				.handle(shaped().roles(Form.none))

				.accept(response -> assertEquals(Response.Unauthorized, response.status()));
	}

	@Test void testShapedUnknown() {
		tray()

				.get(Relator::new)

				.handle(shaped().path("/employees/9999"))

				.accept(response -> assertEquals(Response.NotFound, response.status()));
	}

}
