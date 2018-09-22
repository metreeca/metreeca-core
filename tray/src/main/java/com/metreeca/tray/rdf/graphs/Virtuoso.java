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

package com.metreeca.tray.rdf.graphs;

import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;
import virtuoso.rdf4j.driver.VirtuosoRepository;


public final class Virtuoso extends Graph {

	private final VirtuosoRepository repository;


	public Virtuoso(final String url, final String usr, final String pwd, final String dflt) {

			if ( url == null ) {
				throw new NullPointerException("null url");
			}

			if ( usr == null ) {
				throw new NullPointerException("null usr");
			}

			if ( pwd == null ) {
				throw new NullPointerException("null pwd");
			}

			if ( dflt == null ) {
				throw new NullPointerException("null default graph IRI");
			}

		this.repository=new VirtuosoRepository(url, usr, pwd, dflt) {

			// ;(virtuoso) define default update graph in the preamble
			// https://github.com/openlink/virtuoso-opensource/issues/417

			private String rewrite(final String update) {
				return "define input:default-graph-uri <"+dflt+">\n\n"+update;
			}


			@Override public RepositoryConnection getConnection() throws RepositoryException {
				return new RepositoryConnectionWrapper(this, super.getConnection()) {

					@Override public Update prepareUpdate(final String update)
							throws RepositoryException, MalformedQueryException {
						return super.prepareUpdate(rewrite(update));
					}

					@Override public Update prepareUpdate(final QueryLanguage ql, final String update)
							throws MalformedQueryException, RepositoryException {
						return super.prepareUpdate(ql, rewrite(update));
					}

					@Override public Update prepareUpdate(final QueryLanguage ql, final String update, final String baseURI)
							throws MalformedQueryException, RepositoryException {
						return super.prepareUpdate(ql, rewrite(update), baseURI);
					}

				};
			}
		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override protected Repository repository() {
		return repository;
	}

	@Override protected IsolationLevel isolation() {
		return IsolationLevels.SERIALIZABLE;
	}

}
