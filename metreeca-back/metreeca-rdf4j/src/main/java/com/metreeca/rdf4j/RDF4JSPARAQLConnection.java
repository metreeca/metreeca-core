/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.rdf4j;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;


/**
 * ;(rdf4j Patches commit on read-only txns (see https://github.com/eclipse/rdf4j/issues/1960)
 */
final class RDF4JSPARAQLConnection extends RepositoryConnectionWrapper {

	private boolean dirty; // true if an update operation was executed in the current txn


	RDF4JSPARAQLConnection(final RepositoryConnection connection) {
		super(connection.getRepository(), connection);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void begin() throws RepositoryException {
		try { super.begin(); } finally { dirty=false; }
	}

	@Override public void begin(final IsolationLevel level) throws RepositoryException {
		try { super.begin(level); } finally { dirty=false; }
	}

	@Override public void commit() throws RepositoryException {
		try { if ( dirty ) { super.commit(); } else { super.rollback(); } } finally { dirty=false; }
	}

	@Override public void rollback() throws RepositoryException {
		try { super.rollback(); } finally { dirty=false; }
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override protected boolean isDelegatingAdd() throws RepositoryException {
		try { return super.isDelegatingAdd(); } finally { dirty=true; }
	}

	@Override protected boolean isDelegatingRemove() throws RepositoryException {
		try { return super.isDelegatingRemove(); } finally { dirty=true; }
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void setNamespace(final String prefix, final String name) throws RepositoryException {
		try { super.setNamespace(prefix, name); } finally { dirty=true; }
	}

	@Override public void removeNamespace(final String prefix) throws RepositoryException {
		try { super.removeNamespace(prefix); } finally { dirty=true; }
	}

	@Override public void clearNamespaces() throws RepositoryException {
		try { super.clearNamespaces();} finally { dirty=true; }
	}


	//// Not logged to the curent txn  /////////////////////////////////////////////////////////////////////////////////

	//@Override public Update prepareUpdate(final String update) {
	//	try { return super.prepareUpdate(update); } finally { dirty=true; }
	//}
	//
	//@Override public Update prepareUpdate(final QueryLanguage ql, final String update) {
	//	try { return super.prepareUpdate(ql, update); } finally { dirty=true; }
	//}
	//
	//@Override public Update prepareUpdate(final QueryLanguage ql, final String update, final String baseURI) {
	//	try { return super.prepareUpdate(ql, update, baseURI); } finally { dirty=true; }
	//}

}
