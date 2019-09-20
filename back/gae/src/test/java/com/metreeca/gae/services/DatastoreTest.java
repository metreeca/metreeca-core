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
import com.metreeca.gae.GAETestBase;

import com.google.cloud.datastore.PathElement;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.gae.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;

import static org.assertj.core.api.Assertions.assertThat;


final class DatastoreTest extends GAETestBase {

	@Nested final class Keys {

		@Test void testHierarchy() {
			exec(() -> {

				final Datastore datastore=service(datastore());

				assertThat(datastore.key("/", "Entity"))
						.isEqualTo(datastore.exec(_datastore -> _datastore
								.newKeyFactory()
								.setKind("Entity")
								.newKey("/")
						));

				assertThat(datastore.key("/container/", "Entity"))
						.isEqualTo(datastore.exec(_datastore -> _datastore
								.newKeyFactory()
								.setKind("Entity")
								.newKey("/container/")
						));

				assertThat(datastore.key("/resource", "Entity"))
						.isEqualTo(datastore.exec(_datastore -> _datastore
								.newKeyFactory()
								.setKind("Entity")
								.newKey("/resource")
						));

				assertThat(datastore.key("/container/resource", "Entity"))
						.isEqualTo(datastore.exec(_datastore -> _datastore
								.newKeyFactory()
								.addAncestor(PathElement.of(GAE.Resource, "/container/"))
								.setKind("Entity")
								.newKey("/container/resource")
						));

				assertThat(datastore.key("/container/container/resource", "Entity"))
						.isEqualTo(datastore.exec(_datastore -> _datastore
								.newKeyFactory()
								.addAncestor(PathElement.of(GAE.Resource, "/container/"))
								.addAncestor(PathElement.of(GAE.Resource, "/container/container/"))
								.setKind("Entity")
								.newKey("/container/container/resource")
						));

			});

		}

		@Test void testTyping() {
			exec(() -> {

				final Datastore datastore=service(datastore());


				assertThat(datastore.key("/resource", "Class"))
						.isEqualTo(datastore.exec(_datastore -> _datastore
								.newKeyFactory()
								.setKind("Class")
								.newKey("/resource")
						));

				assertThat(datastore.key("/resource", "Class"))
						.isEqualTo(datastore.exec(_datastore -> _datastore
								.newKeyFactory()
								.setKind("Class")
								.newKey("/resource")
						));

			});
		}


		@Test void testExternal() {
			exec(() -> {

				final Datastore datastore=service(datastore());

				assertThat(datastore.key("http://example.com/path", "Class"))
						.isEqualTo(datastore.exec(_datastore -> _datastore
								.newKeyFactory()
								.setKind("Class")
								.newKey("http://example.com/path")
						));

			});

		}

	}

}
