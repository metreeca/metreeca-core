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

import com.metreeca.json.Query;
import com.metreeca.json.queries.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;

final class GraphFetcher extends Query.Probe<Collection<Statement>> {

	private final IRI resource;
	private final GraphQueryBase.Options options;


	GraphFetcher(final IRI resource, final GraphQueryBase.Options options) {
		this.resource=resource;
		this.options=options;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Collection<Statement> probe(final Items items) {
		return new GraphQueryItems(options).process(resource, items);
	}

	@Override public Collection<Statement> probe(final Terms terms) {
		return new GraphQueryTerms(options).process(resource, terms);
	}

	@Override public Collection<Statement> probe(final Stats stats) {
		return new GraphQueryStats(options).process(resource, stats);
	}

}
