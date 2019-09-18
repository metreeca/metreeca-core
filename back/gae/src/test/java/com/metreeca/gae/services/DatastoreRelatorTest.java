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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.metreeca.gae.GAE.get;
import static com.metreeca.gae.formats.EntityFormat.entity;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;


final class DatastoreRelatorTest extends GAETestBase {

	private Shape employee() {
		return and(

				clazz("Employee"),

				convey().then(

						field(GAE.label, and(required(), datatype(GAE.String))),

						field("code", and(required(), datatype(GAE.String))),
						field("forename", and(required(), datatype(GAE.String))),
						field("surname", and(required(), datatype(GAE.String))),
						field("email", and(required(), datatype(GAE.String))),

						field("title", and(required(), datatype(GAE.String))),
						field("seniority", and(required(), datatype(GAE.Integral))),

						field("office", and(required(), clazz("Office"), relate().then(
								field(GAE.label, and(required(), datatype(GAE.String)))
						))),

						field("supervisor", and(optional(), clazz("Employee"), relate().then(
								field(GAE.label, and(required(), datatype(GAE.String)))
						))),

						field("subordinates", and(multiple(), clazz("Employee"), relate().then(
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

			private Map<String, Object> items(final Predicate<Entity> filter) {
				return items(filter, 0, 0, comparing(Entity::getKey));
			}

			private Map<String, Object> items(final Predicate<Entity> filter,
					final int offset, final int limit, final Comparator<Entity> order
			) {

				final Stream<EmbeddedEntity> a=birt().stream()
						.filter(e -> e.getKey().getKind().equals("Employee"))
						.filter(filter)
						.sorted(order)
						.map(GAE::embed);

				final Stream<EmbeddedEntity> b=(offset > 0) ? a.skip(offset) : a;
				final Stream<EmbeddedEntity> c=(limit > 0) ? b.limit(limit) : b;


				final PropertyContainer expected=new EmbeddedEntity();

				expected.setProperty(GAE.contains, c.collect(toList()));

				return expected.getProperties();
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
										.isEqualTo(items(e
												-> get(e, 0L, "seniority") > 3
												&& get(e, new EmbeddedEntity(), "office").getKey().equals(GAE.key("/offices/1", "Office"))
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

												comparing(e -> (String)get(e, "office.label")).reversed()
												.thenComparingLong(e -> (long)get(e, "seniority"))

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
										.isEqualTo(items(e -> true, 0, 0, comparing(Entity::getKey)))
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
										.isEqualTo(items(e -> get(e, 0L, "seniority") >= 2
												&& get(e, "", "code").compareTo("1370") >= 0
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
										.isEqualTo(items(e -> get(e, 0L, "seniority") >= 2, 0, 0,

												comparing(e -> get(e, "", "code"))

										))
								)
						)

				);
			}


			@Test void testDatatype() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '^seniority': 'Integral' }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> e.getProperty("seniority") instanceof Long))
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
										.isEqualTo(items(e -> (long)e.getProperty("seniority") > 3))
								)
						)

				);
			}

			@Test void testMaxExclusive() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '< seniority': 3 }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> (long)e.getProperty("seniority") < 3))
								)
						)

				);
			}

			@Test void testMinInclusive() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '>= seniority': 3 }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> (long)e.getProperty("seniority") >= 3))
								)
						)

				);
			}

			@Test void testMaxInclusive() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '<= seniority': 3 }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> (long)e.getProperty("seniority") <= 3))
								)
						)

				);
			}


			@Test void testMinLength() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '$> forename': 5 }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> get(e, "", "forename").length() >= 5))
								)
						)

				);
			}

			@Test void testMaxLength() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '$< forename': 5 }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> get(e, "", "forename").length() <= 5))
								)
						)

				);
			}

			@Test void testPattern() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '*label': 'M??y' }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e ->
												e.getProperty(GAE.label).toString().equals("Mary"))))
						)

				);
			}

			@Test void testLike() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '~label': 'ger' }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e ->
												e.getProperty(GAE.label).toString().contains("Ger"))))
						)

				);
			}


			@Test void testMinCount() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '#> subordinates': 2 }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> get(e, emptyList(), "subordinates").size() >= 2))
								)
						)

				);
			}

			@Test void testMaxCount() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '#< subordinates': 2 }"))

						.accept(response -> assertThat(response)
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(items(e -> get(e, emptyList(), "subordinates").size() <= 2))
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
										.isEqualTo(items(e -> singleton("Yoshimi Kato").containsAll(
												get(e, emptyList(), "subordinates.label")
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
										.isEqualTo(items(e -> asList("Jeff Firrelli", "Mary Patterson").containsAll(
												get(e, emptyList(), "subordinates.label")
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
										.isEqualTo(items(e -> get(e, Collections.<EmbeddedEntity>emptyList(), "subordinates")
												.stream()
												.map(EmbeddedEntity::getKey)
												.collect(toList())
												.contains(GAE.key("/employees/1076", "Employee"))
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
										.isEqualTo(items(e -> get(e, Collections.<EmbeddedEntity>emptyList(), "subordinates")
												.stream()
												.map(EmbeddedEntity::getKey)
												.collect(toList())
												.containsAll(asList(
														GAE.key("/employees/1076", "Employee"),
														GAE.key("/employees/1056", "Employee")
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
												get(e, "", "title")
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
												get(e, "", "title")
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

											final Key office=((EmbeddedEntity)e.getProperty("office")).getKey();
											final Key office1=GAE.key("/offices/1", "Office");
											final Key office2=GAE.key("/offices/2", "Office");

											return office.equals(office1) || office.equals(office2);

										}))
								)
						)

				);
			}


			@Test void testOrInequalities() {
				exec(load(birt()), () -> assertThatThrownBy(() -> new DatastoreRelator()

						.handle(new Request()
								.path("/employees/")
								.shape(and(employee(), or(
										field("seniority", minInclusive(3)),
										field("code", maxInclusive("1500"))
								)))
						)

				).isInstanceOf(UnsupportedOperationException.class));
			}

		}

		@Nested final class Terms {

			private Map<String, Object> terms(final Function<Entity, Object> path, final Predicate<Entity> filter) {

				final List<Entity> employees=birt().stream()
						.filter(e -> e.getKey().getKind().equals("Employee"))
						.filter(filter)
						.collect(toList());

				final List<Object> values=employees.stream()
						.map(path)
						.filter(Objects::nonNull)
						.collect(toList());

				final Map<Object, Long> terms=values.stream()
						.collect(groupingBy(t -> t, counting()));


				final PropertyContainer expected=new EmbeddedEntity();

				expected.setProperty(GAE.terms, terms.entrySet().stream()

						.sorted(Comparator.<Map.Entry<Object, Long>>
								comparingLong(Map.Entry::getValue).reversed()
								.thenComparing(Map.Entry::getKey, GAE::compare)
						)

						.map(entry -> {

							final EmbeddedEntity term=new EmbeddedEntity();

							term.setProperty(GAE.value, entry.getKey());
							term.setProperty(GAE.count, entry.getValue());

							return term;

						})

						.collect(toList())
				);

				return expected.getProperties();
			}


			@Test void testBasic() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '_terms': 'title' }"))

						.accept(response -> assertThat(response)
								.hasStatus(OK)
								.hasShape()
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(terms(
												e -> e.getProperty("title"),
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
												e -> e.getProperty("title"),
												e -> get(e, 0L, "seniority") >= 3
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
												e -> get(e, "office.label"),
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
												e -> e.getProperty("supervisor"),
												e -> true
										))
								)
						)

				);
			}

		}

		@Nested final class Stats {

			private Map<String, Object> stats(final Function<Entity, Object> path, final Predicate<Entity> filter) {

				final List<Entity> employees=birt().stream()
						.filter(e -> e.getKey().getKind().equals("Employee"))
						.filter(filter)
						.collect(toList());

				final List<Object> values=employees.stream()
						.map(path)
						.filter(Objects::nonNull)
						.collect(toList());


				final PropertyContainer expected=new EmbeddedEntity();

				final long count=values.size();
				final Object min=values.stream().min(GAE::compare).orElse(null);
				final Object max=values.stream().max(GAE::compare).orElse(null);

				expected.setProperty(GAE.count, count);
				expected.setProperty(GAE.min, min);
				expected.setProperty(GAE.max, max);


				final EmbeddedEntity stats=new EmbeddedEntity();

				stats.setProperty(GAE.type, Optional.ofNullable(GAE.type(min)).orElse(GAE.type(max)));
				stats.setProperty(GAE.count, count);
				stats.setProperty(GAE.min, min);
				stats.setProperty(GAE.max, max);

				expected.setProperty(GAE.stats, singletonList(stats));

				return expected.getProperties();
			}


			@Test void testBasic() {
				exec(load(birt()), () -> new DatastoreRelator()

						.handle(request("{ '_stats': 'seniority' }"))

						.accept(response -> assertThat(response)
								.hasStatus(OK)
								.hasShape()
								.hasBody(entity(), entity -> assertThat(entity.getProperties())
										.isEqualTo(stats(
												e -> e.getProperty("seniority"),
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
												e -> e.getProperty("seniority"),
												e -> get(e, new EmbeddedEntity(), "office").getKey().equals(GAE.key("/offices/1", "Office"))
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
												e -> get(e, "office.label"),
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
												e -> e.getProperty("supervisor"),
												e -> true
										))
								)
						)

				);
			}

		}

	}

	@Nested final class Resource {

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
									.isEqualTo(birt().stream()
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
