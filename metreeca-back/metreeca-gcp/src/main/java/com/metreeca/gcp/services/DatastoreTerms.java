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

import com.metreeca.json.Frame;
import com.metreeca.json.queries.Terms;
import com.metreeca.rest.Config;

import java.util.Optional;

final class DatastoreTerms {

	DatastoreTerms(final Config config) {}


	Optional<Frame> process(final org.eclipse.rdf4j.model.Value value, final Terms terms) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//private Function<Response, Response> terms(final String path, final Terms terms) {
	//
	//	final Comparator<Map.Entry<? extends Value<?>, Long>> byCount=comparingLong(Map.Entry::getValue);
	//	final Comparator<Map.Entry<? extends Value<?>, Long>> byLabel=comparing(x -> label(x.getKey()));
	//	final Comparator<Map.Entry<? extends Value<?>, Long>> byValue=comparing(Map.Entry::getKey, Datastore::compare);
	//
	//	final Entity container=Entity.newBuilder(datastore.newKeyFactory().setKind(GCP.Resource).newKey(path))
	//
	//			.set(DatastoreEngine.terms, this.<List<EntityValue>>values(terms.getShape(), terms.getPath(),
	//					values -> values
	//
	//							.collect(groupingBy(v -> v, counting()))
	//							.entrySet()
	//							.stream()
	//
	//							.sorted(byCount.reversed().thenComparing(byLabel.thenComparing(byValue)))
	//
	//							.map(entry -> FullEntity.newBuilder()
	//
	//									.set(DatastoreEngine.value, entry.getKey())
	//									.set(DatastoreEngine.count, entry.getValue())
	//
	//									.build())
	//
	//							.map(EntityValue::of)
	//							.collect(toList())
	//			))
	//
	//			.build();
	//
	//	return response -> response
	//			.status(OK)
	//			.set(shape(), DatastoreEngine.TermsShape)
	//			.body(entity, container);
	//
	//}


}
