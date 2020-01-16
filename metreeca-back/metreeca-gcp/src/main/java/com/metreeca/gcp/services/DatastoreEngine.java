/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.gcp.services;

import com.metreeca.gcp.GCP;
import com.metreeca.rest.*;
import com.metreeca.rest.services.Engine;
import com.metreeca.tree.Shape;

import java.util.function.Supplier;

import static com.metreeca.gcp.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.tree.Shape.multiple;
import static com.metreeca.tree.Shape.optional;
import static com.metreeca.tree.Shape.required;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Field.field;

import static com.google.cloud.datastore.ValueType.LONG;
import static com.google.cloud.datastore.ValueType.STRING;


/**
 * Model-driven Google Cloud Datastore engine.
 *
 * <p>Manages datastore transactions and handles model-driven CRUD actions on LDP resources stored in the shared
 * {@linkplain Datastore datastore}.</p>
 */
public final class DatastoreEngine implements Engine {

	static final String terms="terms";
	static final String stats="stats";

	static final String value="value";
	static final String count="count";
	static final String max="max";
	static final String min="min";


	private static final Shape TermShape=and(
			field(GCP.label, and(optional(), datatype(STRING)))
	);

	static final Shape TermsShape=and(
			field(terms, and(multiple(),
					field(value, and(required(), TermShape)),
					field(count, and(required(), datatype(LONG)))
			))
	);

	static final Shape StatsShape=and(

			field(count, and(required(), datatype(LONG))),
			field(min, and(optional(), TermShape)),
			field(max, and(optional(), TermShape)),

			field(stats, and(multiple(),
					field(count, and(required(), datatype(LONG))),
					field(min, and(required(), TermShape)),
					field(max, and(required(), TermShape))
			))

	);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Datastore datastore=service(datastore());

	private final DatastoreValidator validator=new DatastoreValidator();
	private final DatastoreTrimmer trimmer=new DatastoreTrimmer();

	private final DatastoreCreator creator=new DatastoreCreator();
	private final DatastoreRelator relator=new DatastoreRelator();
	private final DatastoreUpdater updater=new DatastoreUpdater();
	private final DatastoreDeleter deleter=new DatastoreDeleter();


	@Override public <R> R exec(final Supplier<R> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return datastore.exec(service -> task.get());
	}


	@Override public <M extends Message<M>> Result<M, Failure> validate(final M message) {

		if ( message == null ) {
			throw new NullPointerException("null message");
		}

		return validator.validate(message);
	}

	@Override public <M extends Message<M>> Result<M, Failure> trim(final M message) {

		if ( message == null ) {
			throw new NullPointerException("null message");
		}

		return trimmer.trim(message);
	}


	@Override public Future<Response> create(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		return creator.handle(request);
	}

	@Override public Future<Response> relate(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		return relator.handle(request);
	}

	@Override public Future<Response> update(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		return updater.handle(request);
	}

	@Override public Future<Response> delete(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		return deleter.handle(request);
	}

}
