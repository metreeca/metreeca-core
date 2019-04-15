
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

package com.metreeca.tray.rdf;


import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.repository.RepositoryReadOnlyException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.metreeca.form.things.ValuesTest.construct;
import static com.metreeca.form.things.ValuesTest.export;
import static com.metreeca.form.things.ValuesTest.select;
import static com.metreeca.tray.Tray.tool;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


public final class GraphTest {

	public static Graph graph() {
		return new Graph() {{ repository(new SailRepository(new MemoryStore())); }};
	}


	public static Model model(final Resource... contexts) {
		return tool(Graph.graph()).query(connection -> { return export(connection, contexts); });
	}

	public static Model model(final String sparql) {
		return tool(Graph.graph()).query(connection -> { return construct(connection, sparql); });
	}

	public static List<Map<String, Value>> tuples(final String sparql) {
		return tool(Graph.graph()).query(connection -> { return select(connection, sparql); });
	}


	public static Runnable model(final Iterable<Statement> model, final Resource... contexts) {
		return () -> tool(Graph.graph()).update(connection -> { connection.add(model, contexts); });
	}



	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testPreventUpdateTransactionsOnReadOnlyRepositories() {
		try (final Graph graph=graph()) {

			assertThatExceptionOfType(RepositoryReadOnlyException.class)
					.isThrownBy(() -> graph.isolation(Graph.READ_ONLY).update(connection -> {}));

		}
	}

}
