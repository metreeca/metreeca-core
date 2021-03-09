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

package com.metreeca.rdf4j.assets;

import com.metreeca.json.Shape;
import com.metreeca.json.queries.Terms;
import com.metreeca.rdf4j.assets.GraphEngine.Options;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.*;

import java.math.BigInteger;
import java.util.*;

import static com.metreeca.json.Values.*;
import static com.metreeca.rdf4j.assets.Graph.graph;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.Scribe.code;
import static com.metreeca.rest.Scribe.text;

final class GraphQueryTerms extends GraphQueryBase {

	private final Options options=options();

	private final Graph graph=asset(graph());


	GraphQueryTerms(final Options options) {
		super(options);
	}


	Collection<Statement> process(final IRI resource, final Terms terms) {

		final Shape shape=terms.shape();
		final List<IRI> path=terms.path();
		final int offset=terms.offset();
		final int limit=terms.limit();

		final String target=path.isEmpty() ? Root : "hook";

		final Shape filter=shape
				.filter(resource)
				.resolve(resource)
				.label(1);

		final Collection<Statement> model=new LinkedHashSet<>();

		evaluate(() -> graph.exec(connection -> {
			connection.prepareTupleQuery(compile(() -> code(text(

					"# terms query\n"
							+"\n"
							+"prefix owl: <http://www.w3.org/2002/07/owl#>\n"
							+"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
							+"\n"
							+"select ?value ?count ?label ?notes where {\n"
							+"\n"
							+"\t{\n"
							+"\n"
							+"\t\tselect ({target} as ?value) (count(distinct {source}) as ?count)\n"
							+"\n"
							+"\t\twhere {\n"
							+"\n"
							+"\t\t\t{roots}\n"
							+"\n"
							+"\t\t\t{filters}\n"
							+"\n"
							+"\t\t\t{path}\n"
							+"\n"
							+"\t\t}\n"
							+"\n"
							+"\t\tgroup by {target} \n"
							+"\t\thaving ( count(distinct {source}) > 0 ) \n"
							+"\t\torder by desc(?count) ?value\n"
							+"\t\t{offset}\n"
							+"\t\t{limit}\n"
							+"\n"
							+"\t}\n"
							+"\n"
							+"\toptional { ?value rdfs:label ?label }\n"
							+"\toptional { ?value rdfs:comment ?notes }\n"
							+"\n"
							+"}",

					var(target),
					var(Root),

					roots(filter),
					filters(filter), // !!! use filter(selector, emptySet(), 0, 0) to support sampling

					path(path, target),

					offset(offset),
					limit(limit, options.terms())

			)))).evaluate(new AbstractTupleQueryResultHandler() {
				@Override public void handleSolution(final BindingSet bindings) throws TupleQueryResultHandlerException {

					// ;(virtuoso) counts are returned as xsd:int… cast to stay consistent

					final Value value=bindings.getValue("value");
					final Value count=literal(integer(bindings.getValue("count")).orElse(BigInteger.ZERO));

					final Value label=bindings.getValue("label");
					final Value notes=bindings.getValue("notes");

					final BNode term=bnode(md5(format(value)));

					model.add(statement(resource, GraphQueryBase.terms, term));

					model.add(statement(term, GraphQueryBase.value, value));
					model.add(statement(term, GraphQueryBase.count, count));

					if ( label != null ) { model.add(statement((Resource)value, RDFS.LABEL, label)); }
					if ( notes != null ) { model.add(statement((Resource)value, RDFS.COMMENT, notes)); }

				}
			});
		}));

		return model;
	}

}
