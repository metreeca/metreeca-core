/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.engines;

import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.rdf.graphs.RDF4JSPARQL;

import org.eclipse.rdf4j.IsolationLevels;

import static com.metreeca.form.things.ValuesTest.small;
import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.rdf.Graph.READ_ONLY;


abstract class GraphProcessorTest {

	protected void exec(final Runnable... tasks) {
		new Tray()

				.set(Graph.Factory, () -> new RDF4JSPARQL("https://dydra.com/metreeca/birt-small/sparql").isolation(READ_ONLY))

				.exec(() -> {

					final Graph graph=tool(Graph.Factory); // expect pre-loaded dataset if read-only

					if ( graph.isolation().isCompatibleWith(IsolationLevels.NONE) ) {
						graph.update(connection -> {
							if ( connection.isEmpty() ) { connection.add(small()); }
						});
					}

				})

				.exec(tasks)

				.clear();
	}

}
