/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.engines;

import com.metreeca.form.*;
import com.metreeca.form.queries.Edges;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;
import com.metreeca.form.shapes.*;
import com.metreeca.form.things.Values;
import com.metreeca.form.things.ValuesTest;

import org.assertj.core.api.Assertions;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static com.metreeca.form.Order.decreasing;
import static com.metreeca.form.Order.increasing;
import static com.metreeca.form.Shape.filter;
import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Any.any;
import static com.metreeca.form.shapes.Clazz.clazz;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Like.like;
import static com.metreeca.form.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.form.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.form.shapes.Pattern.pattern;
import static com.metreeca.form.things.Lists.concat;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Values.inverse;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.ValuesTest.construct;
import static com.metreeca.form.things.ValuesTest.item;
import static com.metreeca.form.things.ValuesTest.term;
import static com.metreeca.form.truths.ModelAssert.assertThat;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.stream.Collectors.toSet;


final class ShapedRetrieverTest {

	private static final IRI meta=iri(Form.Namespace, "meta");


	private final Supplier<RepositoryConnection> sandbox=ValuesTest.sandbox(ValuesTest.large());


	//// Edges /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testEdgesEmptyShape() {

		assertThat(edges(and()))
				.as("empty focus")
				.isEmpty();

	}

	@Test void testEdgesEmptyResultSet() {

		assertThat(edges(field(RDF.TYPE, all(RDF.NIL))))
				.as("empty focus")
				.isEmpty();

	}

	@Test void testEdgesEmptyProjection() {

		final Map<Resource, Collection<Statement>> matches=edges(clazz(term("Product")));

		assertThat(matches.keySet())
				.as("matching focus")
				.isEqualTo(focus(

				"select * where { ?product a :Product }"

		));

		assertThat(model(matches))
				.as("empty model")
				.isEmpty();

	}

	@Test void testEdgesMatching() {

		final Map<Resource, Collection<Statement>> matches=edges(field(RDF.TYPE, all(term("Product"))));

		assertThat(matches.keySet())
				.as("matching focus")
				.isEqualTo(focus("select * where { ?product a :Product }"));

		assertThat(model(matches))
				.as("matching model")
				.isIsomorphicTo(model("construct where { ?product a :Product }"));

	}

	@Test void testEdgesSorting() {

		final String query="construct { ?product a :Product }"
				+" where { ?product a :Product; rdfs:label ?label; :line ?line }";

		final Shape shape=field(RDF.TYPE, all(term("Product")));

		// convert to lists to assert ordering

		Assertions.assertThat(list(model(query+" order by ?product"))).as("default (on value)").isEqualTo(list(model(edges(shape))));

		Assertions.assertThat(list(model(query+" order by ?label"))).as("custom increasing").isEqualTo(list(model(edges(shape, increasing(RDFS.LABEL)))));

		Assertions.assertThat(list(model(query+" order by desc(?label)"))).as("custom decreasing").isEqualTo(list(model(edges(shape, decreasing(RDFS.LABEL)))));

		Assertions.assertThat(list(model(query+" order by ?line ?label"))).as("custom combined").isEqualTo(list(model(edges(shape, increasing(term("line")), increasing(RDFS.LABEL)))));

		Assertions.assertThat(list(model(query+" order by desc(?product)"))).as("custom on root").isEqualTo(list(model(edges(shape, decreasing()))));

	}


	//// Stats /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testStatsEmptyResultSet() {

		final Map<Resource, Collection<Statement>> matches=stats(field(RDF.TYPE, all(RDF.NIL)));

		assertThat(set(meta)).as("meta focus").isEqualTo(matches.keySet());

		assertThat(model(matches))
				.isIsomorphicTo(model("construct { form:meta form:count 0 } where {}"));
	}

	@Test void testStatsEmptyProjection() {

		final Map<Resource, Collection<Statement>> matches=stats(clazz(term("Product")));

		assertThat(model(matches))
				.isIsomorphicTo(model(""
						+"\n"
						+"construct { \n"
						+"\n"
						+"\tform:meta \n"
						+"\t\tform:count ?count; form:min ?min; form:max ?max;\n"
						+"\t\tform:stats rdfs:Resource.\n"
						+"\t\n"
						+"\trdfs:Resource \n"
						+"\t\tform:count ?count; form:min ?min; form:max ?max.\n"
						+"\n"
						+"} where {\n"
						+"\n"
						+"\tselect (count(?p) as ?count) (min(?p) as ?min) (max(?p) as ?max) {\n"
						+"\n"
						+"\t\t?p a :Product\n"
						+"\n"
						+"}\n"
						+"\n"
						+"}"));

	}

	@Test void testStatsRootConstraints() {

		final Map<Resource, Collection<Statement>> matches=stats(all(item("employees/1370")), term("account"));

		assertThat(model(matches))
				.isIsomorphicTo(model(""
						+"\n"
						+"construct { \n"
						+"\n"
						+"\tform:meta \n"
						+"\t\tform:count ?count; form:min ?min; form:max ?max;\n"
						+"\t\tform:stats rdfs:Resource.\n"
						+"\t\n"
						+"\trdfs:Resource \n"
						+"\t\tform:count ?count; form:min ?min; form:max ?max.\n"
						+"\n"
						+"} where {\n"
						+"\n"
						+"\tselect (count(?account) as ?count) (min(?account) as ?min) (max(?account) as ?max) {\n"
						+"\n"
						+"\t\t<employees/1370> :account ?account\n"
						+"\n"
						+"\t}\n"
						+"\n"
						+"}"));

	}


	//// Items /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testItemsEmptyResultSet() {

		final Map<Resource, Collection<Statement>> matches=items(field(RDF.TYPE, all(RDF.NIL)));

		assertThat(matches.keySet()).isEqualTo(set(meta));

		assertThat(model(matches))
				.isIsomorphicTo(model("construct {} where {}"));
	}

	@Test void testItemsEmptyProjection() {

		final Map<Resource, Collection<Statement>> matches=items(clazz(term("Product")));

		assertThat(model(matches))
				.isIsomorphicTo(model("construct { \n"
						+"\n"
						+"\tform:meta form:items [\n"
						+"\t\tform:value ?product;\n"
						+"\t\tform:count 1\n"
						+"\t].\n"
						+"\t\n"
						+"\t?product rdfs:label ?label; rdfs:comment ?comment.\n"
						+"\n"
						+"} where {\n"
						+"\n"
						+"\t?product a :Product; \n"
						+"\t\trdfs:label ?label;\n"
						+"\t\trdfs:comment ?comment.\n"
						+"\n"
						+"}"));

	}

	@Test void testItemsRootConstraints() {

		final Map<Resource, Collection<Statement>> matches=items(all(item("employees/1370")), term("account"));

		assertThat(model(matches))
				.isIsomorphicTo(model(""
						+"\n"
						+"construct { \n"
						+"\n"
						+"\tform:meta form:items [\n"
						+"\t\tform:value ?account;\n"
						+"\t\tform:count 1\n"
						+"\t].\n"
						+"\t\n"
						+"\t?account rdfs:label ?label.\n"
						+"\n"
						+"} where {\n"
						+"\n"
						+"\t<employees/1370> :account ?account.\n"
						+"\t\n"
						+"\t?account rdfs:label ?label.\n"
						+"\n"
						+"}"));

	}


	//// Constraints ///////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testClassConstraint() {
		assertThat(model(edges(and(
				clazz(term("Product")),
				field(RDF.TYPE)
		))))
				.isIsomorphicTo(model("construct where { ?product a :Product }"));
	}

	@Test void testDirectUniversalConstraint() {
		assertThat(model(edges(field(
				term("product"),
				all(item("products/S10_2016"), item("products/S24_2022"))
		))))
				.isIsomorphicTo(model("construct { ?root :product ?product } where {\n"
						+"\t?root :product ?product, <products/S10_2016>, <products/S24_2022>\n"
						+"}"));
	}

	@Test void testInverseUniversalConstraint() {
		assertThat(model(edges(field(inverse(term("customer")), all(item("products/S10_2016"), item("products/S24_2022"))))))
				.isIsomorphicTo(model("construct { ?product :customer ?customer } where {\n"
						+"\t?customer ^:customer ?product, <products/S10_2016>, <products/S24_2022>.\n"
						+"}"
				));
	}

	@Test void testRootUniversalConstraint() {
		assertThat(model(edges(and(
				all(item("products/S18_2248"), item("products/S24_3969")),
				field(RDF.TYPE)
		))))
				.isIsomorphicTo(model("construct { ?product a ?type } where {\n"
						+"\n"
						+"\tvalues ?product { <products/S18_2248> <products/S24_3969> }\n"
						+"\t\n"
						+"\t?product a ?type\n"
						+"\t\n"
						+"}"));
	}

	@Test void testSingletonUniversalConstraint() {
		assertThat(model(edges(field(term("product"), all(item("products/S10_2016"))))))
				.isIsomorphicTo(model("construct { ?customer :product ?product } where {\n"
						+"\t?customer :product ?product, <products/S10_2016>\n"
						+"}"));
	}

	@Test void testExistentialConstraint() {
		assertThat(model(edges(field(term("product"), any(item("products/S18_2248"), item("products/S24_3969"))))))
				.isIsomorphicTo(model("construct { ?item :product ?product } where {\n"
						+"\t?item :product ?product, ?value filter (?value in (<products/S18_2248>, <products/S24_3969>))\n"
						+"}"));
	}

	@Test void testSingletonExistentialConstraint() {
		assertThat(model(edges(and(
				any(item("products/S18_2248")),
				field(RDFS.LABEL)
		))))
				.isIsomorphicTo(model("construct where { <products/S18_2248> rdfs:label ?label }"));
	}

	@Test void testMinExclusiveConstraint() { // 100.17 is the exact sell price of 'The Titanic'
		assertThat(model(edges(field(term("sell"), MinExclusive.minExclusive(Values.literal(BigDecimal.valueOf(100.17)))))))
				.isIsomorphicTo(model("construct { \n"
						+"\n"
						+"\t?product :sell ?sell.\n"
						+"\t \n"
						+"} where { \n"
						+"\n"
						+"\t?product :sell ?sell filter (?sell > 100.17)\n"
						+"\t\n"
						+"}"));
	}

	@Test void testMaxExclusiveConstraint() { // 100.17 is the exact sell price of 'The Titanic'
		assertThat(model(edges(field(term("sell"), maxExclusive(Values.literal(BigDecimal.valueOf(100.17)))))))
				.isIsomorphicTo(model("construct { \n"
						+"\n"
						+"\t?product :sell ?sell.\n"
						+"\t \n"
						+"} where { \n"
						+"\n"
						+"\t?product :sell ?sell filter (?sell < 100.17)\n"
						+"\t\n"
						+"}"));
	}

	@Test void testMinInclusiveConstraint() {
		assertThat(model(edges(field(term("sell"), MinInclusive.minInclusive(Values.literal(BigDecimal.valueOf(100)))))))
				.isIsomorphicTo(model("construct { \n"
						+"\n"
						+"\t?product :sell ?sell.\n"
						+"\t \n"
						+"} where { \n"
						+"\n"
						+"\t?product :sell ?sell filter (?sell >= 100)\n"
						+"\t\n"
						+"}"));
	}

	@Test void testMaxInclusiveConstraint() {
		assertThat(model(edges(field(term("sell"), maxInclusive(Values.literal(BigDecimal.valueOf(100)))))))
				.isIsomorphicTo(model("construct { \n"
						+"\n"
						+"\t?product :sell ?sell.\n"
						+"\t \n"
						+"} where { \n"
						+"\n"
						+"\t?product :sell ?sell filter (?sell <= 100)\n"
						+"\t\n"
						+"}"));
	}

	@Test void testPattern() {
		assertThat(model(edges(field(RDFS.LABEL, pattern("\\bferrari\\b", "i")))))
				.isIsomorphicTo(model("construct { \n"
						+"\n"
						+"\t?item rdfs:label ?label\n"
						+"\t \n"
						+"} where { \n"
						+"\n"
						+"\t?item rdfs:label ?label filter regex(?label, '\\\\bferrari\\\\b', 'i')\n"
						+"\n"
						+"}"));
	}

	@Test void testLike() {
		assertThat(model(edges(field(RDFS.LABEL, like("alf ro")))))
				.isIsomorphicTo(model("construct { \n"
						+"\n"
						+"\t?item rdfs:label ?label\n"
						+"\t \n"
						+"} where { \n"
						+"\n"
						+"\t?item rdfs:label ?label filter regex(?label, '\\\\bromeo\\\\b', 'i')\n"
						+"\n"
						+"}"));
	}

	@Test void testMinLength() {
		assertThat(model(edges(field(term("sell"), MinLength.minLength(5)))))
				.isIsomorphicTo(model("construct { \n"
						+"\n"
						+"\t?item birt:sell ?sell\n"
						+"\t \n"
						+"} where { \n"
						+"\n"
						+"\t?item birt:sell ?sell filter (strlen(str(?sell)) >= 5)\n"
						+"\n"
						+"}"));
	}

	@Test void testMaxLength() {
		assertThat(model(edges(field(term("sell"), MaxLength.maxLength(5)))))
				.isIsomorphicTo(model("construct { \n"
						+"\n"
						+"\t?item birt:sell ?sell\n"
						+"\t \n"
						+"} where { \n"
						+"\n"
						+"\t?item birt:sell ?sell filter (strlen(str(?sell)) <= 5)\n"
						+"\n"
						+"}"));
	}


	//// Layout ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testUseIndependentPatternsAndFilters() {
		assertThat(model(edges(and(
				field(term("employee")),
				When.when(filter(), field(term("employee"), any(item("employees/1002"), item("employees/1188"))))
		))))
				.isIsomorphicTo(model("construct {\n"
						+"\n"
						+"\t?office :employee ?employee\n"
						+"\t\n"
						+"} where {\n"
						+"\n"
						+"\t?office :employee ?employee, ?x filter (?x in (<employees/1002>, <employees/1188>))\n"
						+"\n"
						+"}"));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Map<Resource, Collection<Statement>> edges(final Shape shape, final Order... orders) {
		return process(Edges.edges(shape, list(orders), 0, 0));
	}

	private Map<Resource, Collection<Statement>> stats(final Shape shape, final IRI... path) {
		return process(Stats.stats(shape, list(path)));
	}

	private Map<Resource, Collection<Statement>> items(final Shape shape, final IRI... path) {
		return process(Items.items(shape, list(path)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Collection<Statement> model(final Map<Resource, Collection<Statement>> matches) {
		return matches.values().stream().reduce(list(), (x, y) -> concat(x, y));
	}

	private Map<Resource, Collection<Statement>> process(final Query query) {
		try (final RepositoryConnection connection=sandbox.get()) {
			return new ShapedRetriever(connection).process(meta, query);
		}
	}

	private Set<Value> focus(final String query) {
		try (final RepositoryConnection connection=sandbox.get()) {
			return ValuesTest.select(connection, query)
					.stream()
					.flatMap(tuple -> tuple.values().stream())
					.collect(toSet());
		}
	}

	private Collection<Statement> model(final String query) {
		try (final RepositoryConnection connection=sandbox.get()) {
			return construct(connection, query);
		}
	}

}
