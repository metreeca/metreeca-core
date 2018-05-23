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

import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;


final class MappingGraphQuery extends MappingOperation<GraphQuery> implements GraphQuery {

	MappingGraphQuery(final _Mapping mapping, final GraphQuery query) { super(mapping, query); }


	@Override public GraphQueryResult evaluate() throws QueryEvaluationException {
		return mapping.external(operation.evaluate());
	}

	@Override public void evaluate(final RDFHandler handler) throws QueryEvaluationException, RDFHandlerException {
		operation.evaluate(mapping.external(handler));
	}

	@Deprecated @Override public void setMaxQueryTime(final int maxQueryTime) {
		operation.setMaxQueryTime(maxQueryTime);
	}

	@Deprecated @Override public int getMaxQueryTime() {
		return operation.getMaxQueryTime();
	}

}
