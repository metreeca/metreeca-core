
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

package com.metreeca.tray.rdf;


import org.eclipse.rdf4j.query.resultio.text.tsv.SPARQLResultsTSVWriter;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;


final class GraphTest {

	@Test void testUpdateVisibilityWithinTransaction() { // !!! automate using assertions

		final Repository repository=new SailRepository(new MemoryStore());

		//final Repository repository=new SailRepository(new NativeStore(new File("data/work")));
		//final Repository repository=new HTTPRepository("http://localhost:7200/repositories/work"); // graphdb
		//final Repository repository=new SPARQLRepository("http://localhost:9999/blazegraph/namespace/work/sparql"); // blazegraph

		//final Repository repository=new StardogRepository(ConnectionConfiguration
		//		.from("http://localhost:5820/work")
		//		.credentials("admin", "admin"));


		repository.initialize();

		try (final RepositoryConnection connection=repository.getConnection()) {

			connection.clear();

			connection.begin();

			//try {
			//	connection.add(Rio.parse(new StringReader("<test:x> <test:y> <test:z>."), "", RDFFormat.TURTLE));
			//} catch ( final IOException e ) {
			//	throw new UncheckedIOException(e);
			//}

			//connection.prepareUpdate(""
			//		+"insert data { <test:x> <test:y> <test:z> };\n"
			//		+"insert { <test:w> ?p ?o } where { <test:x> ?p ?o };"
			//).execute();

			connection.prepareUpdate("insert data { <test:x> <test:y> <test:z> }").execute();
			connection.prepareUpdate("insert { <test:w> ?p ?o } where { <test:x> ?p ?o };").execute();

			System.out.println("---");

			connection.prepareTupleQuery("select * { ?s ?p ?o } limit 10").
					evaluate(new SPARQLResultsTSVWriter(System.out));

			//connection.commit();
			connection.rollback();

			System.out.println("---");

			connection.prepareTupleQuery("select * { ?s ?p ?o } limit 10").
					evaluate(new SPARQLResultsTSVWriter(System.out));

		}

		repository.shutDown();
	}

}
