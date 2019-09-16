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

package com.metreeca.gae;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static com.metreeca.gae.GAE.compare;
import static com.metreeca.tree.shapes.Clazz.clazz;

import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static org.assertj.core.api.Assertions.assertThat;


final class GAETest extends GAETestBase {

	@Nested final class Keys {

		@Test void testHierarchy() {

			assertThat(GAE.key("/"))
					.isEqualTo(createKey(
							GAE.Entity, "/"));

			assertThat(GAE.key("/container/"))
					.isEqualTo(createKey(
							GAE.Entity, "/container/"));

			assertThat(GAE.key("/resource"))
					.isEqualTo(createKey(
							GAE.Entity, "/resource"));

			assertThat(GAE.key("/container/resource"))
					.isEqualTo(createKey(createKey(
							GAE.Entity, "/container/"),
							GAE.Entity, "/container/resource"));

			assertThat(GAE.key("/container/container/resource"))
					.isEqualTo(createKey(createKey(createKey(
							GAE.Entity, "/container/"),
							GAE.Entity, "/container/container/"),
							GAE.Entity, "/container/container/resource"));

		}

		@Test void testTyping() {

			assertThat(GAE.key("/container/resource", clazz("Kind")))
					.isEqualTo(createKey(createKey(
							GAE.Entity, "/container/"),
							"Kind", "/container/resource"));

			assertThat(GAE.key("/container/resource", "Kind"))
					.isEqualTo(createKey(createKey(
							GAE.Entity, "/container/"),
							"Kind", "/container/resource"));

		}

	}

	@Nested final class Compare {

		@Test void testNulls() {

			assertThat(compare(null, null)).isEqualTo(0);

			assertThat(compare(null, 1)).isLessThan(0);
			assertThat(compare(1, null)).isGreaterThan(0);
		}

		@Test void testIntegers() {

			assertThat(compare(1, 2)).isLessThan(0);
			assertThat(compare(1, 1)).isEqualTo(0);
			assertThat(compare(1, 1L)).isEqualTo(0);
			assertThat(compare(1L, 1)).isEqualTo(0);
			assertThat(compare(2, 1)).isGreaterThan(0);

			assertThat(compare(1, new Date())).isLessThan(0);
			assertThat(compare(new Date(), 1)).isGreaterThan(0);
		}

		@Test void testDates() {

			final Date one=new Date(1);
			final Date two=new Date(2);

			assertThat(compare(one, two)).isLessThan(0);
			assertThat(compare(one, one)).isEqualTo(0);
			assertThat(compare(two, one)).isGreaterThan(0);

			assertThat(compare(new Date(), true)).isLessThan(0);
			assertThat(compare(true, new Date())).isGreaterThan(0);
		}

		@Test void testBooleans() {

			assertThat(compare(false, true)).isLessThan(0);
			assertThat(compare(false, false)).isEqualTo(0);
			assertThat(compare(true, false)).isGreaterThan(0);

			assertThat(compare(true, "")).isLessThan(0);
			assertThat(compare("", true)).isGreaterThan(0);
		}

		@Test void testStrings() {

			assertThat(compare("x", "y")).isLessThan(0);
			assertThat(compare("x", "x")).isEqualTo(0);
			assertThat(compare("y", "x")).isGreaterThan(0);

			assertThat(compare("", 1.0)).isLessThan(0);
			assertThat(compare(1.0, "")).isGreaterThan(0);
		}

		@Test void testFloatings() {

			assertThat(compare(1.0, 2.0)).isLessThan(0);
			assertThat(compare(1.0, 1.0)).isEqualTo(0);
			assertThat(compare(1.0, 1.0D)).isEqualTo(0);
			assertThat(compare(1.0D, 1.0)).isEqualTo(0);
			assertThat(compare(2.0, 1.0)).isGreaterThan(0);

			assertThat(compare(1.0, new EmbeddedEntity())).isLessThan(0);
			assertThat(compare(new EmbeddedEntity(), 1.0)).isGreaterThan(0);
		}

		@Test void testKeys() {

			final Key a=createKey("*", "a");
			final Key b=createKey("*", "b");

			assertThat(compare(a, b)).isLessThan(0);
			assertThat(compare(a, a)).isEqualTo(0);
			assertThat(compare(b, a)).isGreaterThan(0);

			assertThat(compare("", a)).isLessThan(0);
			assertThat(compare(a, "")).isGreaterThan(0);

		}

		@Test void testEntities() {

			final Entity a=new Entity("*", "a");
			final EmbeddedEntity b=new EmbeddedEntity();

			b.setKey(createKey("*", "b"));

			assertThat(compare(a, b)).isLessThan(0);
			assertThat(compare(a, a)).isEqualTo(0);
			assertThat(compare(b, a)).isGreaterThan(0);

			assertThat(compare(a, new Object())).isLessThan(0);
			assertThat(compare(new Object(), b)).isGreaterThan(0);

			assertThat(compare(a, new EmbeddedEntity())).isGreaterThan(0);
			assertThat(compare(new EmbeddedEntity(), a)).isLessThan(0);
			assertThat(compare(new EmbeddedEntity(), new EmbeddedEntity())).isEqualTo(0);

		}

		@Test void testOthers() {

			final Object o=new Object();

			assertThat(compare(o, o)).isEqualTo(0);
		}

	}

}
