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

package com.metreeca.rdf.services;

import com.metreeca.rdf.Values;
import com.metreeca.rdf.ValuesTest;
import com.metreeca.rest.Context;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VOID;

import static com.metreeca.rdf.services.Graph.graph;
import static com.metreeca.rest.Context.service;


abstract class GraphProcessorTest {

	protected void exec(final Runnable... tasks) {
		new Context()

				.set(graph(), GraphTest::graph)

				//.set(graph(), this::graphdb)
				//.set(graph(), this::virtuoso)
				//.set(graph(), this::stardog)
				//.set(graph(), this::dydra)

				.exec(() -> service(graph()).exec(connection -> {

					if ( !connection.hasStatement(Values.iri(ValuesTest.Base), RDF.TYPE, VOID.DATASET, false) ) {
						connection.add(ValuesTest.small()); // load test dataset
					}

				}))

				.exec(tasks)

				.clear();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//private Graph graphdb() {
	//	return new RDF4JRemote("http://localhost:7200/repositories/birt-small");
	//}
	//
	//private Graph virtuoso() {
	//	return new Virtuoso("jdbc:virtuoso://localhost:1111/", "dba", "dba", iri(Base));
	//}
	//
	//private Graph stardog() {
	//	return new Stardog(ConnectionConfiguration
	//			.from("http://localhost:5820/birt-small")
	//			.credentials("admin", "admin")
	//	);
	//}
	//
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//private Graph dydra() {
	//	return new RDF4JSPARQL("https://dydra.com/metreeca/birt-small/sparql");
	//}
	//
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//@Test void testUploadDataset() {
	//	exec(() -> ModelAssert.assertThat(GraphTest.model("construct where { ?office a :Office }"))
	//			.as("test dataset is actually loaded")
	//			.isNotEmpty());
	//}

}
