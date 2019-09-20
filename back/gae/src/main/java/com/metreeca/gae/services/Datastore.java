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

import com.metreeca.gae.GAE;

import com.google.cloud.datastore.*;

import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;


/**
 * Google Cloud Datastore.
 *
 * <p>Manages task execution on Cloud Datastore.</p>
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
		return () -> new Datastore(DatastoreOptions.getDefaultInstance());
	}


	private static final ThreadLocal<Transaction> context=new ThreadLocal<>();


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
	 * Creates an incomplete key for a resource type.
	 *
	 * @param type the type of the resource
	 *
	 * @return an incomplete datastore key for the resource {@code type}
	 *
	 * @throws NullPointerException     if {@code type} is null
	 * @throws IllegalArgumentException if {@code type} is empty
	 */
	public IncompleteKey key(final String type) {

		if ( type == null ) {
			throw new NullPointerException("null type");
		}

		if ( type.isEmpty() ) {
			throw new IllegalArgumentException("empty type");
		}

		return null;
	}

	/**
	 * Creates a datastore key for a typed resource.
	 *
	 * @param id   the id of the resource
	 * @param type the type of the resource
	 *
	 * @return a datastore key for the resource identified by {@code id} and {@code type}
	 *
	 * @throws NullPointerException     if either {@code id} or {@code type} is null
	 * @throws IllegalArgumentException if either {@code id} or {@code type} is empty
	 */
	public Key key(final String id, final String type) {

		if ( type == null ) {
			throw new NullPointerException("null type");
		}

		if ( type.isEmpty() ) {
			throw new IllegalArgumentException("empty type");
		}

		if ( id == null ) {
			throw new NullPointerException("null id");
		}

		if ( id.isEmpty() ) {
			throw new IllegalArgumentException("empty id");
		}

		final KeyFactory factory=datastore.newKeyFactory().setKind(type);

		if ( id.startsWith("/") ) { // ignore external ids
			for (int slash=0; slash >= 0; slash=id.indexOf('/', slash+1)) {
				if ( slash > 0 && slash+1 < id.length() ) { // ignore leading/trailing slashes

					factory.addAncestor(PathElement.of(GAE.Resource, id.substring(0, slash+1)));

				}
			}
		}

		return factory.newKey(id);
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
	public <V> V exec(final Function<com.google.cloud.datastore.Datastore, V> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		if ( context.get() != null ) {

			return requireNonNull(task.apply(datastore), "null task return value");

		} else {

			final Transaction txn=datastore.newTransaction();

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

}
