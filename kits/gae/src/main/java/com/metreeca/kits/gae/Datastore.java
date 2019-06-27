/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.kits.gae;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.sail.SailException;

import java.util.Iterator;
import java.util.function.Function;

import static com.google.appengine.api.datastore.Query.FilterOperator.EQUAL;


public final class Datastore {

	private static final int Version=0; // store layout version

	private static final String StoreKind=Datastore.class.getName()+":store";
	private static final String GraphKind=Datastore.class.getName()+":graph";
	private static final String SpaceKind=Datastore.class.getName()+":space";
	private static final String EntryKind=Datastore.class.getName()+":entry";


	private final DatastoreService datastore;
	private final Key root=KeyFactory.createKey(StoreKind, "default"); // !!! name

	private Transaction txn;


	public Datastore() {

		datastore=DatastoreServiceFactory.getDatastoreService();

		final Transaction txn=datastore.beginTransaction();

		try {

			// ;( now way to check for key presence or to perform ancestor-less queries within txns…

			final Entity store=datastore.get(this.root);

			//logger.info(format("opening store with version %s", store.getProperty("version")));

		} catch ( final EntityNotFoundException e ) {

			final Entity store=new Entity(this.root);

			store.setProperty("version", Version);

			datastore.put(txn, store);

			txn.commit();

			//logger.info(format("initializing store with version %s", store.getProperty("version")));

		} finally {

			if ( txn.isActive() ) { txn.rollback(); }

		}

	}


	//// Transactions //////////////////////////////////////////////////////////////////////////////////////////////////

	private void startTransactionInternal() {
		if ( txn == null ) {

			txn=datastore.beginTransaction();

		} else {

			throw new IllegalStateException("active transaction");

		}
	}

	private void commitInternal() {
		if ( txn == null ) {

			throw new IllegalStateException("no active transaction");

		} else {

			try { if ( txn.isActive() ) { txn.commit(); } } finally { txn=null; }

		}
	}

	private void rollbackInternal() {
		if ( txn == null ) {

			throw new IllegalStateException("no active transaction");

		} else {

			try { if ( txn.isActive() ) { txn.rollback(); } } finally { txn=null; }

		}
	}


	//// Namespaces ////////////////////////////////////////////////////////////////////////////////////////////////////

	private CloseableIteration<? extends Namespace, SailException> getNamespacesInternal() {

		final Query query=new Query(SpaceKind, root);

		return iteration(map(
				datastore.prepare(txn, query).asIterator(),
				entity -> new SimpleNamespace(
						(String)entity.getProperty("prefix"),
						(String)entity.getProperty("name")
				))
		);
	}

	private String getNamespaceInternal(final String prefix) {

		final Query query=new Query(SpaceKind, root)
				.setFilter(new FilterPredicate("prefix", EQUAL, prefix.isEmpty() ? "<default>" : prefix));

		final Entity space=datastore.prepare(txn, query).asSingleEntity();

		return space == null ? null : (String)space.getProperty("name");
	}

	private void setNamespaceInternal(final String prefix, final String name) {

		final Entity space=new Entity(SpaceKind, prefix.isEmpty() ? "<default>" : prefix, root);

		space.setProperty("prefix", prefix);
		space.setProperty("name", name);

		datastore.put(txn, space);
	}

	private void removeNamespaceInternal(final String prefix) {

		final Query query=new Query(SpaceKind, root)
				.setFilter(new FilterPredicate("prefix", EQUAL, prefix.isEmpty() ? "<default>" : prefix))
				.setKeysOnly();

		datastore.delete(txn, () -> map(
				datastore.prepare(txn, query).asIterator(),
				Entity::getKey
		));
	}

	private void clearNamespacesInternal() {

		final Query query=new Query(SpaceKind, root)
				.setKeysOnly();

		datastore.delete(txn, () -> map(
				datastore.prepare(txn, query).asIterator(),
				Entity::getKey
		));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private <V, R> Iterator<R> map(final Iterator<V> iterator, final Function<V, R> mapper) {
		return new Iterator<R>() {

			@Override public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override public R next() {
				return mapper.apply(iterator.next());
			}

		};
	}

	private <V, E extends Exception> CloseableIteration<V, E> iteration(final Iterator<V> iterator) {
		return new AbstractCloseableIteration<V, E>() {

			@Override public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override public V next() {
				return iterator.next();
			}

			@Override public void remove() {
				throw new UnsupportedOperationException("read only iteration");
			}

		};
	}

}
