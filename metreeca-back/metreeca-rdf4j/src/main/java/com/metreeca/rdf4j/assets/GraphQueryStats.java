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
import com.metreeca.json.queries.Stats;
import com.metreeca.rdf4j.assets.GraphEngine.Options;
import com.metreeca.rest.assets.Engine;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.AbstractTupleQueryResultHandler;
import org.eclipse.rdf4j.query.BindingSet;

import java.math.BigInteger;
import java.util.*;

import static com.metreeca.json.Values.*;
import static com.metreeca.rdf4j.assets.Graph.graph;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.Scribe.code;
import static com.metreeca.rest.Scribe.text;

final class GraphQueryStats extends GraphQueryBase {

	private final Options options=options();

	private final Graph graph=asset(graph());


	GraphQueryStats(final Options options) {
		super(options);
	}


	Collection<Statement> process(final IRI resource, final Stats stats) {

		final Shape shape=stats.shape();
		final List<IRI> path=stats.path();
		final int offset=stats.offset();
		final int limit=stats.limit();

		final String target=path.isEmpty() ? root : "hook";

		final Shape filter=shape
				.filter(resource)
				.resolve(resource)
				.label(1);

		final Collection<Statement> model=new LinkedHashSet<>();

		final Map<Value, BigInteger> counts=new HashMap<>();

		final Collection<Value> mins=new ArrayList<>();
		final Collection<Value> maxs=new ArrayList<>();

		evaluate(() -> graph.exec(connection -> {
			connection.prepareTupleQuery(compile(() -> code(text(

					"# stats query\n"
							+"\n"
							+"prefix : <%s>\n"
							+"prefix owl: <http://www.w3.org/2002/07/owl#>\n"
							+"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
							+"\n"
							+"select\n\t"
							+"\n"
							+"?type ?type_label ?type_notes\n"
							+"\n"
							+"?min ?min_label ?min_notes\n"
							+"?max ?max_label ?max_notes\n"
							+"\n"
							+"?count\n"
							+"\n"
							+"\bwhere {\n"
							+"\n"
							+"\t{\n"
							+"\n"
							+"\t\tselect ?type\n"
							+"\n"
							+"\t\t\t(min(%s) as ?min)\n"
							+"\t\t\t(max(%2$s) as ?max) \n"
							+"\n"
							+"\t\t\t(count(distinct %2$s) as ?count)\n"
							+"\n"
							+"\t\twhere {\n"
							+"\n"
							+"\t\t\t%s\n"
							+"\n"
							+"\t\t\t%s\n"
							+"\n"
							+"\t\t\t%s\n"
							+"\n"
							+"\t\t\tbind (if(isBlank(%2$s), :bnode, if(isIRI(%2$s), :iri, datatype(%2$s))) "
							+"as "
							+"?type)\n"
							+"\n"
							+"\t\t}\n"
							+"\n"
							+"\t\tgroup by ?type\n"
							+"\t\thaving ( count(distinct %2$s) > 0 )\n"
							+"\t\torder by desc(?count) ?type\n"
							+"\t\t%s\n"
							+"\t\t%s\n"
							+"\n"
							+"\t}\n"
							+"\n"
							+"\toptional { ?type rdfs:label ?type_label }\n"
							+"\toptional { ?type rdfs:comment ?type_notes }\n"
							+"\n"
							+"\toptional { ?min rdfs:label ?min_label }\n"
							+"\toptional { ?min rdfs:comment ?min_notes }\n"
							+"\n"
							+"\toptional { ?max rdfs:label ?max_label }\n"
							+"\toptional { ?max rdfs:comment ?max_notes }\n"
							+"\n"
							+"}",

					text(Engine.Base),
					var(target),

					roots(filter),
					filters(filter), // !!! use filter(selector, emptySet(), 0, 0) to support sampling

					anchor(path, target),

					offset(offset),
					limit(limit, options.stats())

			)))).evaluate(new AbstractTupleQueryResultHandler() {

				@Override public void handleSolution(final BindingSet bindings) {

					final Resource type=(Resource)bindings.getValue("type");

					final Value type_label=bindings.getValue("type_label");
					final Value type_notes=bindings.getValue("type_notes");

					final Value min=bindings.getValue("min");
					final Value max=bindings.getValue("max");

					final Value min_label=bindings.getValue("min_label");
					final Value min_notes=bindings.getValue("min_notes");

					final Value max_label=bindings.getValue("max_label");
					final Value max_notes=bindings.getValue("max_notes");

					// ;(virtuoso) counts are returned as xsd:int… cast to stay consistent

					final BigInteger count=integer(bindings.getValue("count")).orElse(BigInteger.ZERO);

					model.add(statement(resource, Engine.stats, type));
					model.add(statement(type, Engine.count, literal(count)));

					if ( type_label != null ) { model.add(statement(type, RDFS.LABEL, type_label)); }
					if ( type_notes != null ) { model.add(statement(type, RDFS.COMMENT, type_notes)); }

					if ( min != null ) { model.add(statement(type, Engine.min, min)); }
					if ( max != null ) { model.add(statement(type, Engine.max, max)); }

					if ( min_label != null ) { model.add(statement((Resource)min, RDFS.LABEL, min_label)); }
					if ( min_notes != null ) { model.add(statement((Resource)min, RDFS.COMMENT, min_notes)); }

					if ( max_label != null ) { model.add(statement((Resource)max, RDFS.LABEL, max_label)); }
					if ( max_notes != null ) { model.add(statement((Resource)max, RDFS.COMMENT, max_notes)); }

					counts.putIfAbsent(type, count);

					if ( min != null ) { mins.add(min); }
					if ( max != null ) { maxs.add(max); }

				}

			});
		}));

		model.add(statement(resource, Engine.count, literal(counts.values().stream()
				.reduce(BigInteger.ZERO, BigInteger::add)
		)));

		mins.stream()
				.reduce((x, y) -> compare(x, y) < 0 ? x : y)
				.ifPresent(min -> model.add(statement(resource, Engine.min, min)));

		maxs.stream()
				.reduce((x, y) -> compare(x, y) > 0 ? x : y)
				.ifPresent(max -> model.add(statement(resource, Engine.max, max)));

		return model;
	}

}
