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
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;


final class MappingRDFHandler implements RDFHandler {

	private final _Mapping mapping;
	private final RDFHandler handler;


	MappingRDFHandler(final _Mapping mapping, final RDFHandler handler) {
		this.mapping=mapping;
		this.handler=handler;
	}


	@Override public void startRDF() throws RDFHandlerException {
		handler.startRDF();
	}

	@Override public void endRDF() throws RDFHandlerException {
		handler.endRDF();
	}

	@Override public void handleNamespace(final String prefix, final String uri) throws RDFHandlerException {
		handler.handleNamespace(prefix, mapping.external(uri));
	}

	@Override public void handleStatement(final Statement st) throws RDFHandlerException {
		handler.handleStatement(mapping.external(st));
	}

	@Override public void handleComment(final String comment) throws RDFHandlerException {
		handler.handleComment(comment);
	}

}
