/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.tray.rdf;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.impl.SimpleBinding;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFHandler;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.metreeca.spec.Values.statement;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


final class _Mapping {

	private final String external;
	private final String internal;

	private final Repository repository;


	_Mapping(final String external, final String internal, final Repository repository) {

		this.external=external;
		this.internal=internal;

		this.repository=repository;
	}


	private ValueFactory factory() {
		return repository.getValueFactory(); // ;( NPE on NativeStore if retrieved before initialization
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String internal(final QueryLanguage ql, final String query) {
		if ( ql == null ) {

			return null;

		} else if ( ql.equals(QueryLanguage.SPARQL) ) {

			return query == null ? null : query.replace("<"+external, "<"+internal);

		} else {
			throw new UnsupportedOperationException("unsupported query language ["+ql+"]");
		}
	}

	public Dataset internal(final Dataset dataset) {
		return dataset == null ? null : new Dataset() {

			@Override public Set<IRI> getDefaultRemoveGraphs() {
				return internal(dataset.getDefaultRemoveGraphs());
			}

			@Override public IRI getDefaultInsertGraph() {
				return internal(dataset.getDefaultInsertGraph());
			}

			@Override public Set<IRI> getDefaultGraphs() {
				return internal(dataset.getDefaultGraphs());
			}

			@Override public Set<IRI> getNamedGraphs() {
				return internal(dataset.getNamedGraphs());
			}

		};
	}


	public <T extends Statement> Statement internal(final T statement) {
		return statement == null ? null : statement(
				internal(statement.getSubject()),
				internal(statement.getPredicate()),
				internal(statement.getObject()),
				internal(statement.getContext()));
	}

	public Resource[] internal(final Resource... resources) {
		if ( resources == null ) { return null; } else {

			final Resource[] internal=new Resource[resources.length];

			for (int i=0, n=resources.length; i < n; ++i) {
				internal[i]=internal(resources[i]);
			}

			return internal;
		}
	}


	public Value internal(final Value value) {
		return value instanceof IRI ? internal((IRI)value) : value;
	}

	public Resource internal(final Resource resource) {
		return resource instanceof IRI ? internal((IRI)resource) : resource;
	}

	public IRI internal(final IRI iri) {
		return iri != null && iri.stringValue().startsWith(external) ?
				factory().createIRI(internal, iri.stringValue().substring(external.length())) : iri;
	}

	@Deprecated public URI internal(final URI uri) {
		return uri != null && uri.stringValue().startsWith(external) ?
				factory().createIRI(internal, uri.stringValue().substring(external.length())) : uri;
	}


	public Set<IRI> internal(final Set<IRI> iris) {
		return iris == null ? null : iris.stream().map(this::internal).collect(toSet());
	}

	public String internal(final String iri) {
		return iri != null && iri.startsWith(external) ?
				internal+iri.substring(external.length()) : iri;
	}


	public Iterable<Statement> internal(final Iterable<? extends Statement> statements) {
		return () -> new Iterator<Statement>() {

			private final Iterator<? extends Statement> iterator=statements.iterator();

			@Override public boolean hasNext() { return iterator.hasNext(); }

			@Override public Statement next() { return internal(iterator.next()); }

		};
	}

	public <E extends Exception> Iteration<Statement, E> internal(final Iteration<? extends Statement, E> statements) throws E {
		return new Iteration<Statement, E>() {

			@Override public boolean hasNext() throws E {
				return statements.hasNext();
			}

			@Override public Statement next() throws E {
				return internal(statements.next());
			}

			@Override public void remove() throws E {
				statements.remove();
			}

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Query external(final Query query) {
		return new MappingQuery(this, query);
	}

	public TupleQuery external(final TupleQuery query) {
		return new MappingTupleQuery(this, query);
	}

	public GraphQuery external(final GraphQuery query) {
		return new MappingGraphQuery(this, query);
	}

	public BooleanQuery external(final BooleanQuery query) {
		return new MappingBooleanQuery(this, query);
	}

	public Update external(final Update update) {
		return new MappingUpdate(this, update);
	}


	public TupleQueryResult external(final TupleQueryResult result) {
		return result == null ? null : new MappingTupleQueryResult(this, result);
	}

	public TupleQueryResultHandler external(final TupleQueryResultHandler handler) {
		return new MappingTupleQueryResultHandler(this, handler);
	}

	public GraphQueryResult external(final GraphQueryResult result) {
		return result == null ? null : new MappingGraphQueryResult(this, result);
	}


	public Dataset external(final Dataset dataset) {
		return dataset == null ? null : new Dataset() {

			@Override public Set<IRI> getDefaultRemoveGraphs() {
				return external(dataset.getDefaultRemoveGraphs());
			}

			@Override public IRI getDefaultInsertGraph() {
				return external(dataset.getDefaultInsertGraph());
			}

			@Override public Set<IRI> getDefaultGraphs() {
				return external(dataset.getDefaultGraphs());
			}

			@Override public Set<IRI> getNamedGraphs() {
				return external(dataset.getNamedGraphs());
			}

		};
	}

	public BindingSet external(final BindingSet bindings) {
		return bindings == null ? null : new MappingBindingSet(this, bindings);
	}

	public Binding external(final Binding binding) {
		return binding == null ? null : new SimpleBinding(binding.getName(), external(binding.getValue()));
	}

	public RDFHandler external(final RDFHandler handler) {
		return handler == null ? null : new MappingRDFHandler(this, handler);
	}

	public SimpleNamespace external(final Namespace namespace) {
		return namespace == null ? null : new SimpleNamespace(namespace.getPrefix(), external(namespace.getName()));
	}


	public Statement external(final Statement statement) {
		return statement == null ? null : statement(
				external(statement.getSubject()),
				external(statement.getPredicate()),
				external(statement.getObject()),
				external(statement.getContext()));
	}

	public Value external(final Value value) {
		return value instanceof IRI ? external((IRI)value) : value;
	}

	public Resource external(final Resource resource) {
		return resource instanceof IRI ? external((IRI)resource) : resource;
	}

	public IRI external(final IRI iri) {
		return iri != null && iri.stringValue().startsWith(internal) ?
				factory().createIRI(external, iri.stringValue().substring(internal.length())) : iri;
	}


	public List<String> external(final List<String> iris) {
		return iris == null ? null : iris.stream().map(this::external).collect(toList());
	}

	private Set<IRI> external(final Set<IRI> iris) {
		return iris == null ? null : iris.stream().map(this::external).collect(toSet());
	}

	public String external(final String iri) {
		return iri != null && iri.startsWith(internal) ?
				external+iri.substring(internal.length()) : iri;
	}


	public <T> RepositoryResult<T> external(final Function<T, T> external, final RepositoryResult<T> result) {
		return result == null ? null : new RepositoryResult<>(new CloseableIteration<T, RepositoryException>() {

			@Override public void close() throws RepositoryException { result.close(); }

			@Override public boolean hasNext() throws RepositoryException { return result.hasNext(); }

			@Override public T next() throws RepositoryException { return external.apply(result.next()); }

			@Override public void remove() throws RepositoryException { result.remove(); }

		});
	}

	public <T> Iterator<T> external(final Function<T, T> external, final Iterator<T> iterator) {
		return iterator == null ? null : new Iterator<T>() {

			@Override public boolean hasNext() { return iterator.hasNext(); }

			@Override public T next() { return external.apply(iterator.next()); }

		};
	}

}
