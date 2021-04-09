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

import com.metreeca.json.queries.Items;
import com.metreeca.rest.Config;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Query;
import org.eclipse.rdf4j.model.IRI;

import java.util.Spliterator;
import java.util.stream.Stream;

import static com.metreeca.gcp.services.Datastore.datastore;
import static com.metreeca.rest.Toolbox.service;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

final class DatastoreItems {

	DatastoreItems(final Config config) {}

	Stream<Entity> process(final IRI root, final Items items) {
		return service(datastore()).query(datastore -> stream(spliteratorUnknownSize(

				datastore.run(Query.newEntityQueryBuilder().build()), Spliterator.ORDERED

		), false));
	}

}
