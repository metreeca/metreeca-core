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

package com.metreeca.gae.services;

import com.google.appengine.api.datastore.*;

import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;


/**
 * Google Cloud Datastore.
 *
 * <p>Manages task execution on Cloud Datastore .</p>
 *
 * <p>Nested task executions on the datastore from the same thread will share the same transaction through a {@link
 * ThreadLocal} context variable.</p>
 */
public final class Datastore {

	/**
	 * Retrieves the default datastore factory.
	 *
	 * @return the default datastore factory, which creates datastores with the default configuration
	 */
	public static Supplier<Datastore> datastore() {
		return () -> new Datastore(DatastoreServiceConfig.Builder.withDefaults()
				.implicitTransactionManagementPolicy(ImplicitTransactionManagementPolicy.AUTO)
		);
	}


	private static final ThreadLocal<Transaction> context=new ThreadLocal<>();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final DatastoreService datastore;


	/**
	 * Create a new datastore.
	 *
	 * @param config the datastore configuration
	 *
	 * @throws NullPointerException if {@code config} is null
	 */
	public Datastore(final DatastoreServiceConfig config) {

		if ( config == null ) {
			throw new NullPointerException("null config");
		}

		this.datastore=DatastoreServiceFactory.getDatastoreService(config);
	}


	/**
	 * Executes a task inside a transaction on this datastore.
	 *
	 * <p>If a transaction is not already active on the datastore, begins one and commits it on successful task
	 * completion; if the task throws an exception, the transaction is rolled back and the exception rethrown; in either
	 * case no action is taken if the transaction was already closed inside the task.</p>
	 *
	 * @param task the task to be executed; takes as argument a datastore service
	 * @param <V>  the type of the value returned by {@code task}
	 *
	 * @return the value returned by {@code task}
	 *
	 * @throws NullPointerException if {@code task} is null or returns a null value
	 */
	public <V> V exec(final Function<DatastoreService, V> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		if ( context.get() != null ) {

			return requireNonNull(task.apply(datastore), "null task return value");

		} else {

			final Transaction txn=datastore.beginTransaction(TransactionOptions.Builder.withDefaults()
				.setXG(true)
			);

			context.set(txn);

			try {

				final V value=requireNonNull(task.apply(datastore), "null task return value");

				if ( txn.isActive() ) { txn.commit(); }

				return value;

			} catch ( final Throwable t ) {

				if ( txn.isActive() ) { txn.rollback(); }

				throw t;

			} finally {

				context.remove();

			}

		}

	}

	//private <V, R> Iterator<R> map(final Iterator<V> iterator, final Function<V, R> mapper) {
	//	return new Iterator<R>() {
	//
	//		@Override public boolean hasNext() {
	//			return iterator.hasNext();
	//		}
	//
	//		@Override public R next() {
	//			return mapper.apply(iterator.next());
	//		}
	//
	//	};
	//}
	//
	//private <V, E extends Exception> CloseableIteration<V, E> iteration(final Iterator<V> iterator) {
	//	return new AbstractCloseableIteration<V, E>() {
	//
	//		@Override public boolean hasNext() {
	//			return iterator.hasNext();
	//		}
	//
	//		@Override public V next() {
	//			return iterator.next();
	//		}
	//
	//		@Override public void remove() {
	//			throw new UnsupportedOperationException("read only iteration");
	//		}
	//
	//	};
	//}

}
