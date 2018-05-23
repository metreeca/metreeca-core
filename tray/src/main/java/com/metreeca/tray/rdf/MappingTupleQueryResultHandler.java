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

import org.eclipse.rdf4j.query.*;

import java.util.List;


final class MappingTupleQueryResultHandler implements TupleQueryResultHandler {

	private final _Mapping mapping;
	private final TupleQueryResultHandler handler;


	MappingTupleQueryResultHandler(final _Mapping mapping, final TupleQueryResultHandler handler) {
		this.mapping=mapping;
		this.handler=handler;
	}


	@Override public void handleBoolean(final boolean value) throws QueryResultHandlerException {
		handler.handleBoolean(value);
	}

	@Override public void handleLinks(final List<String> linkUrls) throws QueryResultHandlerException {
		handler.handleLinks(mapping.external(linkUrls));
	}

	@Override public void startQueryResult(final List<String> bindingNames) throws TupleQueryResultHandlerException {
		handler.startQueryResult(bindingNames);
	}

	@Override public void endQueryResult() throws TupleQueryResultHandlerException {
		handler.endQueryResult();
	}

	@Override public void handleSolution(final BindingSet bindingSet) throws TupleQueryResultHandlerException {
		handler.handleSolution(mapping.external(bindingSet));
	}

}
