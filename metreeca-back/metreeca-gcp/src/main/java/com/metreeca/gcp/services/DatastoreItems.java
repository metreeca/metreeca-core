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
import com.metreeca.json.queries.*;
import com.metreeca.rest.*;

import com.google.cloud.datastore.*;
import com.google.cloud.datastore.Query;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.gcp.services.Datastore.datastore;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.convey;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.Toolbox.service;
import static com.metreeca.rest.formats.JSONLDFormat.shape;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

final class DatastoreItems {

	DatastoreItems(final Config config) {}

	Optional<Frame> process(final Resource resource, final Items items) {

		throw new UnsupportedOperationException("to be implemented"); // !!! tbi

		//return service(datastore()).query(datastore -> stream(spliteratorUnknownSize(
		//
		//		datastore.run(Query.newEntityQueryBuilder().build()), Spliterator.ORDERED
		//
		//), false));
	}



	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//private Future<Response> holder(final Request request) {
	//
	//	return request.query(entity, digest(request.shape()))
	//
	//			.value(query -> query.map(new com.metreeca.json.Query.Probe<Function<Response, Response>>() {
	//
	//				@Override public Function<Response, Response> probe(final Items items) {
	//					return items(request.path(), items);
	//				}
	//
	//				@Override public Function<Response, Response> probe(final Terms terms) {
	//					return terms(request.path(), terms);
	//				}
	//
	//				@Override public Function<Response, Response> probe(final Stats stats) {
	//					return stats(request.path(), stats);
	//				}
	//
	//			}))
	//
	//			.fold(request::reply, request::reply);
	//}
	//

	//private Future<Response> member(final Request request) {
	//	return request.reply(response -> datastore.exec(service -> {
	//
	//		final Shape shape=convey(detail(request.shape()));
	//
	//		final Key key=service.newKeyFactory()
	//				.setKind(clazz(shape).map(Object::toString).orElse(GCP.Resource))
	//				.newKey(request.path());
	//
	//		// ;( projecting only properties actually included in the shape would lower costs, as projection queries
	//		// are counted as small operations: unfortunately, a number of limitations apply:
	//		// https://cloud.google.com/appengine/docs/standard/java/datastore/projectionqueries
	//		// #Java_Limitations_on_projections
	//		// https://cloud.google.com/appengine/docs/standard/java/datastore/projectionqueries
	//		// #Java_Projections_and_multiple_valued_properties
	//
	//		return Optional.ofNullable(service.get(key))
	//
	//				.map(entity -> response
	//						.status(OK)
	//						.shape(shape)
	//						.body(this.entity, entity)
	//				)
	//
	//				.orElseGet(() -> response
	//						.status(NotFound)
	//				);
	//
	//	}));
	//}

	//private Function<Response, Response> items(final String path, final Items items) {
	//
	//	final Shape shape=items.shape();
	//
	//	final List<Order> orders=items.orders();
	//	final int offset=items.offset();
	//	final int limit=items.limit();
	//
	//	final Entity container=Entity.newBuilder(datastore.newKeyFactory().setKind(GCP.Resource).newKey(path))
	//
	//			.set(Shape.Contains, this.<List<EntityValue>>entities(shape, orders, offset, limit, entities -> entities
	//
	//					.collect(toList())
	//
	//			))
	//
	//			.build();
	//
	//	return response -> response
	//			.status(OK) // containers are virtual and respond always with 200 OK
	//			.set(shape(), field(Shape.Contains, convey(shape)))
	//			.body(entity, container);
	//
	//}

}
