/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;

import java.util.Map;

import static java.util.stream.Collectors.toMap;


final class MappingGraphQueryResult implements GraphQueryResult {

	private final _Mapping mapping;
	private final GraphQueryResult result;


	MappingGraphQueryResult(final _Mapping mapping, final GraphQueryResult result) {
		this.mapping=mapping;
		this.result=result;
	}


	@Override public Map<String, String> getNamespaces() throws QueryEvaluationException {
		return result.getNamespaces().entrySet().stream()
				.collect(toMap(Map.Entry::getKey, entry -> mapping.external(entry.getValue())));
	}

	@Override public void close() throws QueryEvaluationException {
		result.close();
	}

	@Override public boolean hasNext() throws QueryEvaluationException {
		return result.hasNext();
	}

	@Override public Statement next() throws QueryEvaluationException {
		return mapping.external(result.next());
	}

	@Override public void remove() throws QueryEvaluationException {
		result.remove();
	}

}
