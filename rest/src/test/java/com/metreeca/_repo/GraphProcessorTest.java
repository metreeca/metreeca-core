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

package com.metreeca._repo;

import com.metreeca.kits.rdf4j.RDF4JRemote;
import com.metreeca.kits.rdf4j.RDF4JSPARQL;
import com.metreeca.kits.stardog.Stardog;
import com.metreeca.kits.virtuoso.Virtuoso;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.rdf.GraphTest;

import com.complexible.stardog.api.ConnectionConfiguration;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.ValuesTest.Base;
import static com.metreeca.form.things.ValuesTest.small;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.rdf.Graph.graph;
import static com.metreeca.tray.rdf.GraphTest.model;


abstract class GraphProcessorTest {

	protected void exec(final Runnable... tasks) {
		new Tray()

				.set(graph(), GraphTest::graph)

				//.set(graph(), this::graphdb)
				//.set(graph(), this::virtuoso)
				//.set(graph(), this::stardog)
				//.set(graph(), this::dydra)

				.exec(() -> {

					final Graph graph=tool(graph()); // expect pre-loaded dataset if read-only

					if ( graph.isolation().isCompatibleWith(IsolationLevels.NONE) ) {
						graph.exec(connection -> {
							if ( !connection.hasStatement(iri(Base), RDF.TYPE, VOID.DATASET, false) ) {
								connection.add(small());
							}
						});
					}

				})

				.exec(tasks)

				.clear();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Graph graphdb() {
		return new RDF4JRemote("http://localhost:7200/repositories/birt-small");
	}

	private Graph virtuoso() {
		return new Virtuoso("jdbc:virtuoso://localhost:1111/", "dba", "dba", iri(Base));
	}

	private Graph stardog() {
		return new Stardog(ConnectionConfiguration
				.from("http://localhost:5820/birt-small")
				.credentials("admin", "admin")
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Graph dydra() {
		return new RDF4JSPARQL("https://dydra.com/metreeca/birt-small/sparql");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testUploadDataset() {
		exec(() -> assertThat(model("construct where { ?office a :Office }"))
				.as("test dataset is actually loaded")
				.isNotEmpty());
	}

}
