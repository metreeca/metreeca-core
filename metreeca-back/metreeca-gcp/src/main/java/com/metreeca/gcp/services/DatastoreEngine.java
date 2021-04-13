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

import com.metreeca.json.*;
import com.metreeca.json.Query;
import com.metreeca.json.queries.*;
import com.metreeca.rest.*;
import com.metreeca.rest.services.Engine;

import com.google.cloud.datastore.*;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.Optional;

import static com.metreeca.gcp.services.Datastore.datastore;
import static com.metreeca.json.Frame.frame;
import static com.metreeca.rest.Toolbox.service;

import static com.google.cloud.datastore.StructuredQuery.PropertyFilter.eq;


/**
 * Model-driven Google Cloud Datastore engine.
 *
 * <p>Handles model-driven CRUD operations on linked data resources stored in the shared {@linkplain Datastore
 * datastore}.</p>
 */
public final class DatastoreEngine extends Setup<DatastoreEngine> implements Engine {

	private final Datastore datastore=service(datastore());


	@Override public Optional<Frame> create(final Frame frame, final Shape shape) {

		throw new UnsupportedOperationException("to be implemented"); // !!! tbi

		//return graph.update(connection -> Optional.of(frame.focus())
		//
		//		.filter(item
		//				-> !connection.hasStatement(item, null, null, true)
		//				&& !connection.hasStatement(null, null, item, true)
		//		)
		//
		//		.map(item -> {
		//
		//			connection.add(frame.model());
		//
		//			return frame;
		//
		//		})
		//);

	}

	@Override public Optional<Frame> relate(final Frame frame, final Query query) {
		return query.map(new QueryProbe(this, frame.focus()));
	}

	@Override public Optional<Frame> update(final Frame frame, final Shape shape) {

		throw new UnsupportedOperationException("to be implemented"); // !!! tbi

		//return graph.update(connection -> Optional
		//
		//		.of(Items.items(shape).map(new QueryProbe(this, frame.focus())))
		//
		//		.filter(model -> !model.isEmpty())
		//
		//		.map(model -> {
		//
		//			connection.remove(model);
		//			connection.add(frame.model());
		//
		//			return frame;
		//
		//		})
		//);


		//final Key key=datastore.newKeyFactory()
		//		.setKind(clazz(convey(request.shape())).map(Object::toString).orElse(GCP.Resource))
		//		.newKey(request.path());
		//
		//final KeyQuery query=com.google.cloud.datastore.Query.newKeyQueryBuilder()
		//		.setKind(key.getKind())
		//		.setFilter(eq("__key__", key))
		//		.build();
		//
		//if ( datastore.run(query).hasNext() ) {
		//
		//	datastore.put(Entity.newBuilder(key, entity).build());
		//
		//	return response.status(Response.NoContent);
		//
		//} else {
		//
		//	return response.status(Response.NotFound);
		//
		//}

	}

	@Override public Optional<Frame> delete(final Frame frame, final Shape shape) {

		throw new UnsupportedOperationException("to be implemented"); // !!! tbi

		//return graph.update(connection -> Optional
		//
		//		.of(Items.items(shape).map(new QueryProbe(this, frame.focus())))
		//
		//		.filter(model -> !model.isEmpty())
		//
		//		.map(model -> {
		//
		//			connection.remove(model);
		//
		//			return frame(frame.focus(), model);
		//
		//		})
		//);

		//final Key key=service.newKeyFactory()
		//		.setKind(clazz(convey(request.shape())).map(Object::toString).orElse(GCP.Resource))
		//		.newKey(request.path());
		//
		//final KeyQuery query=com.google.cloud.datastore.Query.newKeyQueryBuilder()
		//		.setKind(key.getKind())
		//		.setFilter(eq("__key__", key))
		//		.build();
		//
		//if ( service.run(query).hasNext() ) {
		//
		//	service.delete(key);
		//
		//	return response.status(Response.NoContent);
		//
		//} else {
		//
		//	return response.status(Response.NotFound);
		//
		//}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class QueryProbe extends Query.Probe<Optional<Frame>> {

		private final Config config;
		private final Resource resource;


		QueryProbe(final Config config, final Resource resource) {
			this.config=config;
			this.resource=resource;
		}


		@Override public Optional<Frame> probe(final Items items) {
			return new DatastoreItems(config).process(resource, items);
		}

		@Override public Optional<Frame> probe(final Terms terms) {
			return new DatastoreTerms(config).process(resource, terms);
		}

		@Override public Optional<Frame> probe(final Stats stats) {
			return new DatastoreStats(config).process(resource, stats);
		}

	}

}
