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

package com.metreeca.mongo.assets;

import com.metreeca.json.Shape;
import com.metreeca.rest.Context;
import com.metreeca.rest.Request;

import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.*;
import java.util.stream.Stream;

import static com.metreeca.json.Shape.optional;
import static com.metreeca.json.Shape.required;
import static com.metreeca.json.Values.IRIType;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.ValuesTest.Base;
import static com.metreeca.json.ValuesTest.small;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.mongo.assets.Mongo.mongo;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.Context.text;
import static com.metreeca.rest.assets.Engine.engine;
import static com.metreeca.rest.formats.JSONLDFormat.*;
import static com.metreeca.rest.formats.OutputFormat.output;
import static com.mongodb.client.model.Projections.include;
import static java.lang.String.format;
import static java.util.Collections.singletonMap;

final class MongoTest {

	static { // logging not configured: reset and enable fine console logging

		if ( System.getProperty("java.util.logging.config.file") == null
				&& System.getProperty("java.util.logging.config.class") == null ) {

			final Level level=Level.WARNING;

			LogManager.getLogManager().reset();

			final ConsoleHandler handler=new ConsoleHandler();

			handler.setLevel(level);

			final Logger logger=Logger.getLogger("");

			logger.setLevel(level);
			logger.addHandler(handler);

			System.setProperty("java.util.logging.config.file", "");
		}

	}


	@Test void test() {
		new Context()

				.set(mongo(), () -> new Mongo(new MongoMock()))

				.set(engine(), MongoEngine::new)

				.exec(() -> asset(mongo()).exec(client -> {

					final MongoDatabase database=client.getDatabase("birt");

					Stream.of("offices", "employees").forEach(dataset -> Optional
							.of(database.getCollection(dataset))
							.filter(collection -> collection.countDocuments() == 0)
							.ifPresent(collection -> collection.insertMany(Document
									.parse(text(this, format("%s.json", dataset)))
									.getList(dataset, Document.class)
							))
					);

					return this;

				}))

				.exec(() -> asset(mongo()).exec(client -> {

					final MongoDatabase database=client.getDatabase("birt");

					database.getCollection("offices").find()
							.projection(include("id"))
							.forEach((Consumer<? super Document>)document -> {});

					return this;

				}))

				.clear();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static IRI term(final String name) {
		return iri("http://example.com/terms#", name);
	}

	private static final Shape Office=and(

			field(RDFS.LABEL, required(), datatype(XSD.STRING)),
			field(term("code"), required(), datatype(XSD.STRING)),

			field(term("area"), required(), datatype(XSD.STRING)),

			field(term("country"), required(), datatype(XSD.STRING),
					field(RDFS.LABEL, required(), datatype(XSD.STRING)),
					field(WGS84.LAT, required(), datatype(XSD.DECIMAL)),
					field(WGS84.LONG, required(), datatype(XSD.DECIMAL))
			),

			field(term("city"), required(), datatype(XSD.STRING),
					field(RDFS.LABEL, required(), datatype(XSD.STRING)),
					field(WGS84.LAT, required(), datatype(XSD.DECIMAL)),
					field(WGS84.LONG, required(), datatype(XSD.DECIMAL))
			)

	);

	private static final Shape Employee=and(

			field(RDFS.LABEL, required(), datatype(XSD.STRING)),
			field(term("code"), required(), datatype(XSD.STRING)),

			field(term("forename"), required(), datatype(XSD.STRING)),
			field(term("surname"), required(), datatype(XSD.STRING)),
			field(term("email"), required(), datatype(XSD.STRING)),
			field(term("title"), required(), datatype(XSD.STRING)),

			field(term("office"), required(), datatype(IRIType)),

			field(term("seniority"), required(), datatype(XSD.INTEGER)),
			field(term("supervisor"), optional(), datatype(IRIType))

	);

	@Test void offices() {
		new Context()

				.set(keywords(), () -> singletonMap("@id", "id"))

				.exec(() -> new Request()

						.base(Base)
						.path("/offices-basic/")

						.reply(response -> response
								.attribute(shape(), field(LDP.CONTAINS, Office))
								.body(jsonld(), small())
						)

						.accept(response -> response.body(output()).accept(
								e -> {}, target -> target.accept(System.out)
						))
				);
	}

	@Test void employees() {
		new Context()

				.set(keywords(), () -> singletonMap("@id", "id"))

				.exec(() -> new Request()

						.base(Base)
						.path("/employees-basic/")

						.reply(response -> response
								.attribute(shape(), field(LDP.CONTAINS, Employee))
								.body(jsonld(), small())
						)

						.accept(response -> response.body(output()).accept(
								e -> {}, target -> target.accept(System.out)
						))
				);
	}

}
