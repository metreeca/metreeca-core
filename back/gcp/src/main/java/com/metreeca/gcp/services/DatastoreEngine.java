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

package com.metreeca.gcp.services;

import com.metreeca.rest.*;
import com.metreeca.rest.services.Engine;
import com.metreeca.tree.Shape;

import java.util.function.Supplier;

import static com.metreeca.gcp.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;


public final class DatastoreEngine implements Engine {

	private final Datastore datastore=service(datastore());

	private final DatastoreSplitter splitter=new DatastoreSplitter();
	private final DatastoreTrimmer trimmer=new DatastoreTrimmer();
	private final DatastoreValidator validator=new DatastoreValidator();

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


	@Override public Shape container(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return splitter.container(shape);
	}

	@Override public Shape resource(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return splitter.resource(shape);
	}


	@Override public <M extends Message<M>> Result<M, Failure> trim(final M message) {

		if ( message == null ) {
			throw new NullPointerException("null message");
		}

		return trimmer.trim(message);
	}

	@Override public <M extends Message<M>> Result<M, Failure> validate(final M message) {

		if ( message == null ) {
			throw new NullPointerException("null message");
		}

		return validator.validate(message);
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
