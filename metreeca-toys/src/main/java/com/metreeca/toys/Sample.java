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

import com.metreeca.jee.JEEServer;
import com.metreeca.rdf4j.services.Graph;
import com.metreeca.rdf4j.services.GraphEngine;

import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import javax.servlet.annotation.WebFilter;

import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.rest.Wrapper.preprocessor;
import static com.metreeca.rest.Xtream.entry;
import static com.metreeca.rest.Xtream.map;
import static com.metreeca.rest.formats.JSONLDFormat.keywords;
import static com.metreeca.rest.handlers.Router.router;
import static com.metreeca.rest.services.Engine.engine;
import static com.metreeca.rest.wrappers.Bearer.bearer;
import static com.metreeca.rest.wrappers.Server.server;

@WebFilter(urlPatterns="/*")
public final class Sample extends JEEServer {

	public Sample() {
		delegate(toolbox -> toolbox

				.set(graph(), () -> new Graph(new SailRepository(new MemoryStore())))
				.set(engine(), GraphEngine::new)

				.set(keywords(), () -> map(
						entry("@id", "id"),
						entry("@type", "type")
				))

				.exec(new Toys())

				.get(() -> server()

						.with(preprocessor(request -> request.base(Toys.Base)))

						.with(bearer("secret", Toys.staff))

						.wrap(router()

								.path("/products/*", new Products())
								.path("/product-lines/*", new ProductLines())

						)

				)
		);
	}

}