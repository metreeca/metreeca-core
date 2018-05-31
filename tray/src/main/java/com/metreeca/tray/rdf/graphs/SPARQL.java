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
import com.metreeca.tray.sys.Setup;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.algebra.evaluation.iterator.CollectionIteration;
import org.eclipse.rdf4j.repository.*;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;
import org.eclipse.rdf4j.repository.base.RepositoryWrapper;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import static com.metreeca.spec.things.Values.literal;
import static com.metreeca.tray.Tray.tool;


public final class SPARQL extends Graph {

	public static final Supplier<Graph> Factory=() -> {

		final Setup setup=tool(Setup.Factory);

		final String url=setup.get("graph.sparql.url", "");
		final String query=setup.get("graph.sparql.query", "");
		final String update=setup.get("graph.sparql.update", "");

		if ( url.isEmpty() ) {
			if ( query.isEmpty() && update.isEmpty() ) {
				throw new IllegalArgumentException("missing endpoint URL property");
			} else if ( query.isEmpty() ) {
				throw new IllegalArgumentException("missing endpoint base/query URL property");
			} else if ( update.isEmpty() ) {
				throw new IllegalArgumentException("missing endpoint base/update URL property");
			}
		}

		final String usr=setup.get("graph.sparql.usr", "");
		final String pwd=setup.get("graph.sparql.pwd", "");

		return new SPARQL(resolve(url, query), resolve(url, update), usr, pwd);
	};


	private static String resolve(final String base, final String url) {
		return url.isEmpty() ? base : URI.create(base).resolve(url).toASCIIString();
	}


	public SPARQL(final String url, final String usr, final String pwd) {
		this(url, url, usr, pwd);
	}

	public SPARQL(final String query, final String update, final String usr, final String pwd) {
		super("SPARQL 1.1 Remote Store", IsolationLevels.NONE, () -> { // must be able to see its own changes

			if ( query == null ) {
				throw new NullPointerException("null query endpoint URL");
			}

			if ( update == null ) {
				throw new NullPointerException("null update endpoint URL");
			}

			if ( usr == null ) {
				throw new NullPointerException("null usr");
			}

			if ( pwd == null ) {
				throw new NullPointerException("null pwd");
			}

			final SPARQLRepository repository=new SPARQLRepository(query, update);

			if ( !usr.isEmpty() || !pwd.isEmpty() ) {
				repository.setUsernameAndPassword(usr, pwd);
			}

			return new SPARQLNamespaceRepository(repository); // ;( namespace ops silently ignored in SPARQLRepository

		});
	}


	private static final class SPARQLNamespaceRepository extends RepositoryWrapper {

		private SPARQLNamespaceRepository(final Repository repository) { super(repository); }

		@Override public RepositoryConnection getConnection() throws RepositoryException {
			return new SPARQLNamespaceConnection(getDelegate());
		}

	}

	private static final class SPARQLNamespaceConnection extends RepositoryConnectionWrapper {

		private SPARQLNamespaceConnection(final Repository repository) { super(repository, repository.getConnection()); }


		@Override public String getNamespace(final String prefix) throws RepositoryException {

			final TupleQuery query=query("select ?name { [] a :Namespace; :prefix ?prefix; :name ?name }");

			query.setBinding("prefix", literal(prefix));

			final Collection<String> names=new ArrayList<>();

			query.evaluate(new AbstractTupleQueryResultHandler() {
				@Override public void handleSolution(final BindingSet bindings) {
					names.add(bindings.getBinding("name").getValue().stringValue());
				}
			});

			return names.stream().findFirst().orElse(null);
		}

		@Override public void setNamespace(final String prefix, final String name) throws RepositoryException {

			final Update update=update("delete where { ?ns a :Namespace; :prefix ?prefix; :name ?current };"
					+" insert data { _:ns a :Namespace; :prefix ?prefix; :name ?updated } ");

			update.setBinding("prefix", literal(prefix));
			update.setBinding("updated", literal(name));

			update.execute();
		}

		@Override public void removeNamespace(final String prefix) throws RepositoryException {

			final Update update=update("delete where { [] a :Namespace; :prefix ?prefix; :name ?name }");

			update.setBinding("prefix", literal(prefix));

			update.execute();
		}


		@Override public RepositoryResult<Namespace> getNamespaces() throws RepositoryException {

			final Collection<Namespace> namespaces=new ArrayList<>();

			query("select ?prefix ?name { [] a :Namespace; :prefix ?prefix; :name ?name }")

					.evaluate(new AbstractTupleQueryResultHandler() {
						@Override public void handleSolution(final BindingSet bindings) {
							namespaces.add(new SimpleNamespace(
									bindings.getBinding("prefix").getValue().stringValue(),
									bindings.getBinding("name").getValue().stringValue()));
						}
					});

			return new RepositoryResult<>(new CollectionIteration<>(namespaces));
		}

		@Override public void clearNamespaces() throws RepositoryException {
			update("delete where { [] a :Namespace; :prefix ?prefix; :name ?name }");
		}


		private TupleQuery query(final String query) {
			return prepareTupleQuery("prefix : <"+SESAME.NAMESPACE+"> "+query);
		}

		private Update update(final String update) {
			return prepareUpdate("prefix : <"+SESAME.NAMESPACE+"> "+update);
		}

	}

}
