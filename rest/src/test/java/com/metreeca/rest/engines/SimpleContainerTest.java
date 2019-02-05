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

import com.metreeca.rest.Engine;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.Model;
import org.junit.jupiter.api.Nested;

import static com.metreeca.form.things.ValuesTest.small;
import static com.metreeca.rest.HandlerAssert.graph;
import static com.metreeca.tray.Tray.tool;

final class SimpleContainerTest {

	private void exec(final Runnable task) {
		new Tray()
				.exec(graph(dataset()))
				.exec(task)
				.clear();
	}


	private Model dataset() {
		return small();
	}

	private Engine engine() {
		return new SimpleContainer(tool(Graph.Factory));
	}


	@Nested final class Basic {

		@Nested final class Relate {

		}

		@Nested final class Create {


		}

	}

	@Nested final class Direct {

		@Nested final class Relate {

		}

		@Nested final class Create {

		}

	}

}
