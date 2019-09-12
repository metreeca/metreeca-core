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
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tree.Shape;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.CompositeFilter;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.metreeca.gae.formats.EntityFormat.entity;
import static com.metreeca.gae.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.tree.Shape.*;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Field.field;

import static com.google.appengine.api.datastore.Query.FilterOperator.EQUAL;
import static com.google.appengine.api.datastore.Query.FilterOperator.GREATER_THAN_OR_EQUAL;
import static com.google.appengine.api.datastore.Query.FilterOperator.IN;
import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;


final class DatastoreRelatorTest extends GAETestBase {

	private Shape employee() {
		return and(

				clazz("Employee"),

				convey().then(

						field("code", and(required(), datatype(GAE.String))),
						field("label", and(required(), datatype(GAE.String))),
						field("forename", and(required(), datatype(GAE.String))),
						field("surname", and(required(), datatype(GAE.String))),
						field("email", and(required(), datatype(GAE.String))),

						field("title", and(required(), datatype(GAE.String))),
						field("seniority", and(required(), datatype(GAE.Integral))),

						field("office", and(required(), clazz("Office"), relate().then(
								field("label", and(required(), datatype(GAE.String)))
						))),

						field("supervisor", and(optional(), clazz("Employee"), relate().then(
								field(GAE.id, and(required(), datatype(GAE.String))),
								field(GAE.label, and(required(), datatype(GAE.String)))
						))),

						field("subordinates", and(multiple(), clazz("Employee"), relate().then(
								field(GAE.id, and(required(), datatype(GAE.String))),
								field(GAE.label, and(required(), datatype(GAE.String)))
						)))

				)

		);
	}


	@Nested final class Container {

		private Request request(final String query) {
			return new Request()
					.path("/employees/")
					.query(query.replace('\'', '"'))
					.shape(employee());
		}


		@Nested final class Items {

			private List<PropertyContainer> items(final BiConsumer<Query, FetchOptions> task) {
				return items(task, entity -> true);
			}

			private List<PropertyContainer> items(final BiConsumer<Query, FetchOptions> task, final Predicate<Entity> predicate) {

				return service(datastore()).exec(service -> {

					final Query query=new Query("Employee");
					final FetchOptions fetch=FetchOptions.Builder.withDefaults();

					task.accept(query, fetch);

					return service.prepare(query).asList(fetch).stream()
							.filter(predicate)
							.map(entity -> {

								final EmbeddedEntity embedded=new EmbeddedEntity();

								embedded.setKey(entity.getKey());
								embedded.setPropertiesFrom(entity);

								return embedded;

							})
							.collect(toList());

				});

			}


			@Test void testUnfiltered() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request(""))

						.accept(response -> assertThat(response)
								.hasStatus(OK)
								.hasShape()
								.hasBody(entity(), entity -> assertThat(entity.getProperty(GAE.contains))
										.isEqualTo(items((query, fetch) -> {}))
								)
						)

				);
			}

			@Test void testSorted() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '_order': ['-office.label', 'seniority' ], '_offset': 10, '_limit': 5 }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperty(GAE.contains))
										.isEqualTo(items((query, fetch) -> {

											query.addSort("office.label", Query.SortDirection.DESCENDING);
											query.addSort("seniority", Query.SortDirection.ASCENDING);

											fetch.offset(10);
											fetch.limit(5);

										}))
								)
						)

				);
			}


			@Test void testDatatype() {}

			@Test void testClazz() {}


			@Test void testMinInclusive() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '>= seniority': 4 }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperty(GAE.contains))
										.isEqualTo(items((query, fetch) -> query.setFilter(
												new FilterPredicate("seniority", GREATER_THAN_OR_EQUAL, 4)
										)))
								)
						)

				);
			}


			@Test void testLike() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '~label': 'ger' }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperty(GAE.contains))
										.isEqualTo(items((query, fetch) -> {}, e ->
												e.getProperty(GAE.label).toString().contains("Ger")
										))
								)
						)

				);
			}


			@Test void testUniversalEmpty() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '!subordinates.id': [] }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperty(GAE.contains))
										.isEqualTo(items((query, fetch) -> {}))
								)
						)

				);
			}

			@Test void testUniversalSingleton() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '!subordinates.id': '/employees/1076' }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperty(GAE.contains))
										.isEqualTo(items((query, fetch) -> query.setFilter(
												new FilterPredicate("subordinates.id", EQUAL, "/employees/1076")
										)))
								)
						)

				);
			}

			@Test void testUniversalMultiple() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '!subordinates.id': ['/employees/1076', '/employees/1056'] }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperty(GAE.contains))
										.isEqualTo(items((query, fetch) -> query.setFilter(
												new CompositeFilter(Query.CompositeFilterOperator.AND, asList(
														new FilterPredicate("subordinates.id", EQUAL, "/employees/1076"),
														new FilterPredicate("subordinates.id", EQUAL, "/employees/1056")
												))
										)))
								)
						)

				);
			}


			@Test void testExistentialEmpty() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ 'title': [] }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperty(GAE.contains))
										.isEqualTo(items((query, fetch) -> {}))
								)
						)

				);
			}

			@Test void testExistentialSingleton() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ 'title': 'President' }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperty(GAE.contains))
										.isEqualTo(items((query, fetch) -> query.setFilter(
												new FilterPredicate("title", EQUAL, "President")
										)))
								)
						)

				);
			}

			@Test void testExistentialMultiple() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ 'title': ['President', 'VP Sales'] }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperty(GAE.contains))
										.isEqualTo(items((query, fetch) -> query.setFilter(
												new FilterPredicate("title", IN, asList("President", "VP Sales"))
										)))
								)
						)

				);
			}

		}

		@Nested final class Terms {

			private <T> List<EmbeddedEntity> terms(final Function<Stream<Entity>, Stream<Object>> expected) {

				return service(datastore()).exec(service -> expected

						.apply(
								stream(service.prepare(new Query()).asIterable().spliterator(), false)
										.filter(e -> e.getKey().getKind().equals("Employee"))
						)

						.collect(groupingBy(e -> e, counting()))

						.entrySet()
						.stream()
						.sorted(((Comparator<Map.Entry<Object, Long>>)
								(x, y) -> -Long.compare(x.getValue(), y.getValue())) // decreasing count
								.thenComparing((x, y) -> GAE.compare(x.getKey(), y.getKey())) // increasing value
						)
						.map(entry -> {

							final EmbeddedEntity embedded=new EmbeddedEntity();

							embedded.setProperty("term", entry.getKey());
							embedded.setProperty("count", entry.getValue());

							return embedded;

						})

						.collect(toList())

				);

			}


			@Test void testBasic() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '_terms': 'title' }"))

						.accept(response -> assertThat(response)
								.hasStatus(OK)
								.hasShape()
								.hasBody(entity(), entity -> assertThat(entity.getProperty(DatastoreRelator.terms))
										.isEqualTo(terms(entities -> entities

												.map(e -> e.getProperty("title"))

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
								.hasBody(entity(), entity -> assertThat(entity.getProperty(DatastoreRelator.terms))
										.isEqualTo(terms(entities -> entities

												.filter(e -> GAE.compare(e.getProperty("seniority"), 3) >= 0)
												.map(e -> e.getProperty("title"))

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
								.hasBody(entity(), entity -> assertThat(entity.getProperty(DatastoreRelator.terms))
										.isEqualTo(terms(entities -> entities

												.map(e -> (PropertyContainer)e.getProperty("office"))
												.map(e -> e.getProperty(GAE.label))

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
								.hasBody(entity(), entity -> assertThat(entity.getProperty(DatastoreRelator.terms))
										.isEqualTo(terms(entities -> entities

												.map(e -> (EmbeddedEntity)e.getProperty("supervisor"))
												.filter(Objects::nonNull)

												.map(e -> { // only id/label retained

													final EmbeddedEntity supervisor=new EmbeddedEntity();

													supervisor.setProperty("id", e.getProperty("id"));
													supervisor.setProperty(GAE.label, e.getProperty("label"));

													return supervisor;

												})

										))
								)
						)

				);
			}

		}

		@Nested final class Stats  {

		}

	}

	@Nested final class Resource {

		@Test void testRelateResource() {
			exec(load(birt()), () -> new DatastoreRelator()

					.handle(new Request()
							.path("/offices/1")
							.shape(and(
									clazz("Office"),
									field(GAE.label, and(required(), datatype(GAE.String)))
							))
					)

					.accept(response -> assertThat(response)
							.hasStatus(OK)
							.hasShape()
							.hasBody(entity(), entity -> assertThat(((Entity)entity).getKey().getName())
									.isEqualTo("/offices/1")
							)
					)

			);
		}

		@Test void testHandleUnknownResources() {
			exec(load(birt()), () -> new DatastoreRelator()

					.handle(new Request()
							.path("/offices/9999")
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
