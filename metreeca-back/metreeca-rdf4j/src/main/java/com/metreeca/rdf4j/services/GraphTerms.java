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

package com.metreeca.rdf4j.services;

import com.metreeca.json.Frame;
import com.metreeca.json.Shape;
import com.metreeca.json.queries.Terms;
import com.metreeca.rest.Config;
import com.metreeca.rest.services.Engine;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.*;

import java.math.BigInteger;
import java.util.*;

import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.rdf4j.SPARQLScribe.*;
import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.rest.Scribe.indent;
import static com.metreeca.rest.Scribe.text;
import static com.metreeca.rest.Scribe.*;
import static com.metreeca.rest.Toolbox.service;
import static com.metreeca.rest.Xtream.task;

final class GraphTerms extends GraphFacts {

	private final Graph graph=service(graph());


	GraphTerms(final Config config) {
		super(config);
	}


	Frame process(final Value focus, final Terms terms) {
		if ( focus.isResource() ) {

			final Shape shape=terms.shape();
			final List<IRI> path=terms.path();
			final int offset=terms.offset();
			final int limit=terms.limit();

			final Shape filter=shape
					.filter(focus)
					.resolve(focus); // .filter() may introduce focus values › resolve afterwards

			final Shape convey=shape
					.convey()
					.resolve(focus);

			final Shape select=and(filter, path(convey, path)).label(this::label); // requires path to exist in convey

			final String hook=hook(select, path);

			final Collection<Statement> model=new LinkedHashSet<>();

			evaluate(() -> graph.query(task(connection -> {
				connection.prepareTupleQuery(compile(() -> code(list(

						comment("terms query"),

						prefix(OWL.NS),
						prefix(RDFS.NS),

						space(select(), where(

								space(block(

										space(select(space(indent(
												as("value", var(hook)),
												as("count", count(true, var(root)))
										)))),

										space(where(

												space(tree(select, true))

												// !!! sampling w/ options.stats()

										)),

										space(
												line(group(var(hook))),
												line(having(gt(count(var(root)), text(0)))),
												line(order(desc(var("count")), var("value"))),
												line(offset(offset)),
												line(limit(limit))
										)

								)),

								space(
										line(optional(edge(var("value"), "rdfs:label", var("label")))),
										line(optional(edge(var("value"), "rdfs:comment", var("notes"))))
								)

						))

				)))).evaluate(new AbstractTupleQueryResultHandler() {
					@Override public void handleSolution(final BindingSet bindings) throws TupleQueryResultHandlerException {

						// ;(virtuoso) counts are returned as xsd:int… cast to stay consistent

						final Value value=bindings.getValue("value");
						final Value count=literal(integer(bindings.getValue("count")).orElse(BigInteger.ZERO));

						final Value label=bindings.getValue("label");
						final Value notes=bindings.getValue("notes");

						final BNode term=bnode(md5(format(value)));

						model.add(statement((Resource)focus, Engine.terms, term));

						model.add(statement(term, Engine.value, value));
						model.add(statement(term, Engine.count, count));

						if ( label != null ) { model.add(statement((Resource)value, RDFS.LABEL, label)); }
						if ( notes != null ) { model.add(statement((Resource)value, RDFS.COMMENT, notes)); }

					}
				});
			})));

			return frame(focus, model);

		} else { return frame(focus); }
	}

}
