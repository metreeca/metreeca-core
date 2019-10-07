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

import com.metreeca.gcp.GCP;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tree.Shape;

import com.google.cloud.datastore.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.metreeca.gcp.formats.EntityFormat.entity;
import static com.metreeca.gcp.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.tree.Shape.*;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.tree.shapes.MinInclusive.minInclusive;
import static com.metreeca.tree.shapes.Or.or;

import static com.google.cloud.datastore.ValueType.LONG;
import static com.google.cloud.datastore.ValueType.STRING;
import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;


final class DatastoreRelatorTest extends DatastoreTestBase {

	private Shape employee() {
		return and(

				clazz("Employee"),

				convey().then(

						field(GCP.label, and(required(), datatype(STRING))),

						field("code", and(required(), datatype(STRING))),
						field("forename", and(required(), datatype(STRING))),
						field("surname", and(required(), datatype(STRING))),
						field("email", and(required(), datatype(STRING))),

						field("title", and(required(), datatype(STRING))),
						field("seniority", and(required(), datatype(LONG))),

						field("office", and(required(), clazz("Office"), relate().then(
								field(GCP.label, and(required(), datatype(STRING)))
						))),

						field("supervisor", and(optional(), clazz("Employee"), relate().then(
								field(GCP.label, and(required(), datatype(STRING)))
						))),

						field("subordinates", and(multiple(), clazz("Employee"), relate().then(
								field(GCP.label, and(required(), datatype(STRING)))
						)))

				)

		);
	}


	@Nested final class Holder {

		private Key key(final String id, final String type) {
			return service(datastore()).newKeyFactory().setKind(type).newKey(id);
		}

		private Request request(final String query) {
			return new Request()
					.path("/employees/")
					.query(query.replace('\'', '"'))
					.shape(employee());
		}


		@Nested final class Items {

			private Map<String, Value<?>> items(final Predicate<Entity> filter) {
				return items(filter, 0, 0, comparing(e -> KeyValue.of(e.getKey()), Datastore::compare));
			}

			private Map<String, Value<?>> items(final Predicate<Entity> filter,
					final int offset, final int limit, final Comparator<Entity> order
			) {

				final Stream<Entity> a=birt().get().stream()
						.filter(e -> e.getKey().getKind().equals("Employee"))
						.filter(filter)
						.sorted(order);

				final Stream<Entity> b=(offset > 0) ? a.skip(offset) : a;
				final Stream<Entity> c=(limit > 0) ? b.limit(limit) : b;

				return singletonMap(GCP.contains, ListValue.of(c.map(Datastore::value).collect(toList())));
			}


			@Test void testPlain() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request(""))

						.accept(response -> assertThat(response)
								.hasStatus(OK)
								.hasShape()
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> true))
								)
						)

				);
			}

			@Test void testFiltered() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request(">seniority=3&office=/offices/1"))

						.accept(response -> assertThat(response)
								.hasStatus(OK)
								.hasShape()
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> e.getLong("seniority") > 3
												&& e.getEntity("office").getKey().equals(key("/offices/1", "Office"))
										))
								)
						)

				);
			}


			@Test void testSorted() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '_order': ['-office.label', 'seniority' ], '_offset': 10, '_limit': 5 }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> true, 10, 5, Comparator.<Entity, String>

												comparing(e -> e.getEntity("office").getString("label")).reversed()
												.thenComparingLong(e -> e.getLong("seniority"))

										))
								)
						)

				);
			}

			@Test void testSortedOnEmptyPath() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '_order': '' }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> true))
								)
						)

				);
			}


			@Test void testMultipleInequalityFilter() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '>= seniority': 2, '>= code': '1370' }"))

						.accept(response -> assertThat(response)
								.hasStatus(OK)
								.hasShape()
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> e.getLong("seniority") >= 2
												&& e.getString("code").compareTo("1370") >= 0
										))
								)
						)

				);
			}

			@Test void testInconsistentInequalityAndOrder() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '>= seniority': 2, '_order': 'code' }"))

						.accept(response -> assertThat(response)
								.hasStatus(OK)
								.hasShape()
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> e.getLong("seniority") >= 2, 0, 0,

												comparing(e -> e.getString("code"))

										))
								)
						)

				);
			}


			@Test void testDatatype() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '^seniority': 'LONG' }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> e.getValue("seniority").getType() == LONG))
								)
						)

				);
			}

			@Test void testClazz() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '@': 'Employee' }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> e.getKey().getKind().equals("Employee")))
								)
						)

				);
			}


			@Test void testMinExclusive() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '> seniority': 3 }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> e.getLong("seniority") > 3))
								)
						)

				);
			}

			@Test void testMaxExclusive() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '< seniority': 3 }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> e.getLong("seniority") < 3))
								)
						)

				);
			}

			@Test void testMinInclusive() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '>= seniority': 3 }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> e.getLong("seniority") >= 3))
								)
						)

				);
			}

			@Test void testMaxInclusive() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '<= seniority': 3 }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> e.getLong("seniority") <= 3))
								)
						)

				);
			}


			@Test void testMinLength() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '$> forename': 5 }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> e.getString("forename").length() >= 5))
								)
						)

				);
			}

			@Test void testMaxLength() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '$< forename': 5 }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> e.getString("forename").length() <= 5))
								)
						)

				);
			}

			@Test void testPattern() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '*label': 'M??y' }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> e.getString(GCP.label).equals("Mary"))))
						)

				);
			}

			@Test void testLike() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '~label': 'ger' }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> e.getString(GCP.label).contains("Ger"))))
						)

				);
			}


			@Test void testMinCount() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '#> subordinates': 2 }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> e.contains("subordinates") && e.getList("subordinates").size() >= 2))
								)
						)

				);
			}

			@Test void testMaxCount() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '#< subordinates': 2 }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> e.contains("subordinates") && e.getList("subordinates").size() <= 2))
								)
						)

				);
			}


			@Test void testInEmpty() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '%subordinates.label': [] }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> true))
								)
						)

				);
			}

			@Test void testInSingleton() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '%subordinates.label': 'Yoshimi Kato' }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> e.contains("subordinates") && singleton("Yoshimi Kato").containsAll(

												e.getList("subordinates").stream()
														.map(v -> ((EntityValue)v).get().getString("label"))
														.collect(Collectors.<String>toList())

										)))
								)
						)

				);
			}

			@Test void testInMultiple() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '%subordinates.label': ['Jeff Firrelli', 'Mary Patterson'] }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> e.contains("subordinates") && asList("Jeff Firrelli", "Mary Patterson").containsAll(

												e.getList("subordinates").stream()
														.map(v -> ((EntityValue)v).get().getString("label"))
														.collect(Collectors.<String>toList())

										)))
								)
						)

				);
			}


			@Test void testAllEmpty() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '!subordinates': [] }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> true))
								)
						)

				);
			}

			@Test void testAllSingleton() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '!subordinates': '/employees/1076' }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> e.contains("subordinates") && e.getList("subordinates")
												.stream()
												.map(v -> ((EntityValue)v).get().getKey())
												.collect(toList())
												.contains(key("/employees/1076", "Employee"))
										))
								)
						)

				);
			}

			@Test void testAllMultiple() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '!subordinates': ['/employees/1076', '/employees/1056'] }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> e.contains("subordinates") && e.getList("subordinates")
												.stream()
												.map(v -> ((EntityValue)v).get().getKey())
												.collect(toList())
												.containsAll(asList(
														key("/employees/1076", "Employee"),
														key("/employees/1056", "Employee")
												))
										))
								)
						)

				);
			}


			@Test void testAnyEmpty() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ 'title': [] }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> true))
								)
						)

				);
			}

			@Test void testAnySingleton() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ 'title': 'President' }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> "President".equals(
												e.getString("title")
										)))
								)
						)

				);
			}

			@Test void testAnyMultiple() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ 'title': ['President', 'VP Sales'] }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> asList("President", "VP Sales").contains(
												e.getString("title")
										)))
								)
						)

				);
			}

			@Test void testAnyMultipleEntities() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("office=/offices/1&office=/offices/2"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> {

											final IncompleteKey office=e.getEntity("office").getKey();
											final Key office1=key("/offices/1", "Office");
											final Key office2=key("/offices/2", "Office");

											return office.equals(office1) || office.equals(office2);

										}))
								)
						)

				);
			}


			@Test void testOrInequalities() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(new Request()
								.path("/employees/")
								.shape(and(employee(), or(
										field("seniority", minInclusive(3)),
										field("code", maxInclusive("1200"))
								)))
						)

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e
												-> e.getLong("seniority") >= 3
												|| e.getString("code").compareTo("1200") <= 0
										))
								)
						)

				);
			}


			@Test void testInequalitiesOnEntity() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request(">office=/offices/1&<supervisor=/employees/1500"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> {

											final FullEntity<IncompleteKey> office=e.getEntity("office");
											final FullEntity<IncompleteKey> supervisor=e.contains("supervisor") ? e.getEntity("supervisor") : null;

											final Key office1=key("/offices/1", "Office");
											final Key employee1500=key("/employees/1500", "Employee");

											return Datastore.compare(KeyValue.of((Key)office.getKey()), KeyValue.of(office1)) > 0
													&& supervisor != null && Datastore.compare(KeyValue.of((Key)supervisor.getKey()), KeyValue.of(employee1500)) < 0;

										}))
								)
						)

				);
			}

		}

		@Nested final class Terms {

			private Map<String, Value<?>> terms(final Function<Entity, Value<?>> path, final Predicate<Entity> filter) {

				final List<Entity> employees=birt().get().stream()
						.filter(e -> e.getKey().getKind().equals("Employee"))
						.filter(filter)
						.collect(toList());

				final List<Value<?>> values=employees.stream()
						.map(path)
						.filter(v -> v.getType() != ValueType.NULL)
						.collect(toList());

				final Map<Value<?>, Long> terms=values.stream()
						.collect(groupingBy(t -> t, counting()));


				return singletonMap(DatastoreEngine.terms, ListValue.of(terms.entrySet().stream()

						.sorted(Comparator.<Map.Entry<Value<?>, Long>>
								comparingLong(Map.Entry::getValue).reversed()
								.thenComparing(Map.Entry::getKey,

										Comparator.<Value<?>, String>comparing(v ->
												v.getType() == ValueType.ENTITY && ((EntityValue)v).get().contains(GCP.label) ?
														((EntityValue)v).get().getString(GCP.label) : ""
										)

												.thenComparing(Datastore::compare)

								)
						)

						.map(entry -> EntityValue.of(FullEntity.newBuilder()

								.set(DatastoreEngine.value, entry.getKey())
								.set(DatastoreEngine.count, entry.getValue())

								.build()
						))

						.collect(toList())

				));

			}


			@Test void testBasic() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '_terms': 'title' }"))

						.accept(response -> assertThat(response)
								.hasStatus(OK)
								.hasShape()
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(terms(
												e -> e.getValue("title"),
												e -> true
										))
								)
						)

				);
			}

			@Test void testFiltered() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '_terms': 'title', '>= seniority': 3 }"))

						.accept(response -> assertThat(response)
								.hasStatus(OK)
								.hasShape()
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(terms(
												e -> e.getValue("title"),
												e -> e.getLong("seniority") >= 3
										))
								)
						)

				);
			}

			@Test void testNested() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '_terms': 'office.label' }"))

						.accept(response -> assertThat(response)
								.hasStatus(OK)
								.hasShape()
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(terms(
												e -> e.getEntity("office").getValue("label"),
												e -> true
										))
								)
						)

				);
			}

			@Test void testEmbedded() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '_terms': 'supervisor' }"))

						.accept(response -> assertThat(response)
								.hasStatus(OK)
								.hasShape()
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(terms(
												e -> e.contains("supervisor") ? e.getValue("supervisor") : NullValue.of(),
												e -> true
										))
								)
						)

				);
			}


			@Test void testRetainOnlyCoreEntityProperties() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("_terms&~label=ger"))

						.accept(response -> assertThat(response)
								.hasStatus(OK)
								.hasShape()
								.hasBody(entity(), entity -> assertThat(entity.getList("terms")).allSatisfy(term -> {

									final FullEntity<IncompleteKey> value=((EntityValue)term).get().getEntity("value");
									final Set<String> properties=value.getProperties().keySet();

									assertThat(properties)
											.as("only core properties retained")
											.isSubsetOf(GCP.id, GCP.type, GCP.label, GCP.comment);
								}))
						)

				);
			}

		}

		@Nested final class Stats {

			private Map<String, Value<?>> stats(final Function<Entity, Value<?>> path, final Predicate<Entity> filter) {

				final List<Entity> employees=birt().get().stream()
						.filter(e -> e.getKey().getKind().equals("Employee"))
						.filter(filter)
						.collect(toList());

				final List<Value<?>> values=employees.stream()
						.map(path)
						.filter(v -> v.getType() != ValueType.NULL)
						.collect(toList());


				final long count=values.size();

				final Value<?> min=values.stream().min(Datastore::compare).orElse(NullValue.of());
				final Value<?> max=values.stream().max(Datastore::compare).orElse(NullValue.of());

				return FullEntity.newBuilder()

						.set(DatastoreEngine.count, count)
						.set(DatastoreEngine.min, min)
						.set(DatastoreEngine.max, max)

						.set(DatastoreEngine.stats, ListValue.of(FullEntity.newBuilder()

								.set(GCP.type, Optional
										.ofNullable(min.getType().toString())
										.orElse(max.getType().toString())
								)

								.set(DatastoreEngine.count, count)
								.set(DatastoreEngine.min, min)
								.set(DatastoreEngine.max, max)

								.build()
						))

						.build()
						.getProperties();
			}


			@Test void testBasic() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '_stats': 'seniority' }"))

						.accept(response -> assertThat(response)
								.hasStatus(OK)
								.hasShape()
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(stats(
												e -> e.getValue("seniority"),
												e -> true
										))
								)
						)

				);
			}

			@Test void testFiltered() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("_stats=seniority&office=/offices/1"))

						.accept(response -> assertThat(response)
								.hasStatus(OK)
								.hasShape()
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(stats(
												e -> e.getValue("seniority"),
												e -> e.getEntity("office").getKey().equals(key("/offices/1", "Office"))
										))
								)
						)

				);
			}

			@Test void testNested() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '_stats': 'office.label' }"))

						.accept(response -> assertThat(response)
								.hasStatus(OK)
								.hasShape()
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(stats(
												e -> e.getEntity("office").getValue("label"),
												e -> true
										))
								)
						)

				);
			}

			@Test void testEmbedded() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '_stats': 'supervisor' }"))

						.accept(response -> assertThat(response)
								.hasStatus(OK)
								.hasShape()
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(stats(
												e -> e.contains("supervisor") ? e.getValue("supervisor") : NullValue.of(),
												e -> true
										))
								)
						)

				);
			}

			@Test void testRetainOnlyCoreEntityProperties() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("_stats&~label=ger"))

						.accept(response -> assertThat(response)
								.hasStatus(OK)
								.hasShape()
								.hasBody(entity(), entity -> assertThat(asList(
										entity.getEntity("min"),
										entity.getEntity("max")
								)).allSatisfy(term -> {

									final Set<String> properties=term.getProperties().keySet();

									assertThat(properties)
											.as("only core properties retained")
											.isSubsetOf(GCP.id, GCP.type, GCP.label, GCP.comment);
								}))
						)

				);
			}

		}

	}

	@Nested final class Member {

		@Test void testRelate() {
			exec(load(birt()), () -> new DatastoreRelator()

					.handle(new Request()
							.path("/employees/1002")
							.shape(employee())
					)

					.accept(response -> assertThat(response)
							.hasStatus(OK)
							.hasShape()
							.hasBody(entity(), entity -> assertThat(entity)
									.isEqualTo(birt().get().stream()
											.filter(e -> e.getKey().getName().equals(response.request().path()))
											.findFirst()
											.orElse(null)
									)
							)
					)

			);
		}

		@Test void testReportMissing() {
			exec(load(birt()), () -> new DatastoreRelator()

					.handle(new Request()
							.path("/employees/9999")
					)

					.accept(response -> assertThat(response)
							.hasStatus(Response.NotFound)
							.doesNotHaveShape()
							.doesNotHaveBody(entity())
					)

			);
		}

	}

}
