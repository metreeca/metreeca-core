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

import com.metreeca.rdf4j.services.Graph;
import com.metreeca.rest.Request;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryResult;

import java.util.function.Function;

import static com.metreeca.json.Values.literal;
import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.rest.Toolbox.service;
import static com.metreeca.rest.formats.JSONLDFormat.jsonld;

public final class ProductsSlug implements Function<Request, String> {

	private final Graph graph=service(graph());

	@Override
	public String apply(final Request request) {
		return graph.query(connection -> {

			final Value scale=literal(request.body(jsonld()).get()
					.flatMap(frame -> frame.string(Toys.scale))
					.orElse("1:1")
			);

			int serial=0;

			try ( final RepositoryResult<Statement> matches=connection.getStatements(
					null, Toys.scale, scale
			) ) {
				for (; matches.hasNext(); matches.next()) { ++serial; }
			}

			String code="";

			do {
				code=String.format("S%s_%d", scale.stringValue().substring(2), serial);
			} while ( connection.hasStatement(
					null, Toys.code, literal(code), true
			) );

			return code;

		});
	}

}
