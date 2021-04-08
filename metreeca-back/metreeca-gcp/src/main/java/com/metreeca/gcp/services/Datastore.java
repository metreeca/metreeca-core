/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.gcp.services;

import com.metreeca.rest.Wrapper;

import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Transaction;

import java.util.function.Function;
import java.util.function.Supplier;

import static com.metreeca.rest.Toolbox.service;


/**
 * Google Cloud Datastore.
 *
 * <p>Manages task execution on Google Cloud Datastore.</p>
 *
 * <p>Nested task executions on the same thread will share the same transaction through a {@link
 * ThreadLocal} context variable.</p>
 *
 * @see <a href="https://cloud.google.com/datastore/docs/how-to">Google Cloud Datastore</a>
 */
public final class Datastore {

	private static final ThreadLocal<Transaction> context=new ThreadLocal<>();


	/**
	 * Retrieves the default datastore factory.
	 *
	 * @return the default datastore factory, which creates datastores with the default configuration
	 */
	public static Supplier<Datastore> datastore() {
		return () -> new Datastore(DatastoreOptions.getDefaultInstance());
	}


	//// Transactions //////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a transaction wrapper.
	 *
	 * @return a wrapper ensuring that requests are handled within a single datastore transaction
	 */
	public static Wrapper txn() {

		final Datastore datastore=service(datastore());

		return handler -> request -> consumer -> datastore.update(connection -> {

			handler.handle(request).accept(consumer);

			return datastore;

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final com.google.cloud.datastore.Datastore datastore;


	/**
	 * Create a new datastore.
	 *
	 * @param options the datastore options
	 *
	 * @throws NullPointerException if {@code options} is null
	 */
	public Datastore(final DatastoreOptions options) {

		if ( options == null ) {
			throw new NullPointerException("null options");
		}

		this.datastore=options.getService();
	}


	/**
	 * Executes a query on this datastore.
	 *
	 * @param query the query to be executed; takes as argument the datastore service
	 * @param <V>   the type of the value returned by {@code query}
	 *
	 * @return the value returned by {@code query}
	 *
	 * @throws NullPointerException if {@code query} is null
	 */
	public <V> V query(final Function<com.google.cloud.datastore.Datastore, V> query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		return query.apply(datastore);
	}

	/**
	 * Executes an update inside a transaction on this datastore.
	 *
	 * <ul>
	 *
	 *      <li>if a transaction is not already active on the underlying storage, begins one and commits it on
	 *      successful update completion;</li>
	 *
	 *      <li>if the update throws an exception, rolls back the transaction and rethrows the exception;</li>
	 *
	 *      <li>in either case, no action is taken if the transaction was already terminated inside the update.</li>
	 *
	 * </ul>
	 *
	 * @param update the update to be executed; takes as argument a transaction on the datastore service
	 * @param <V>    the type of the value returned by {@code update}
	 *
	 * @return the value returned by {@code update}
	 *
	 * @throws NullPointerException if {@code update} is null
	 */
	public <V> V update(final Function<Transaction, V> update) {

		if ( update == null ) {
			throw new NullPointerException("null update");
		}

		Transaction txn=context.get();

		if ( txn != null ) { return update.apply(txn); } else {

			context.set(txn=datastore.newTransaction());

			try {

				final V value=update.apply(txn);

				if ( txn.isActive() ) { txn.commit(); }

				return value;

			} finally {

				context.remove();

				if ( txn.isActive() ) { txn.rollback(); }

			}

		}

	}

}
