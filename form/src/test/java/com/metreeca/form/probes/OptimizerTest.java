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

package com.metreeca.form.probes;

import com.metreeca.form.Shape;
import com.metreeca.form.shapes.*;
import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.Meta.alias;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.Trait.trait;

import static org.assertj.core.api.Assertions.assertThat;


final class OptimizerTest {

	private static final Shape x=Datatype.datatype(RDF.NIL);
	private static final Shape y=MinCount.minCount(1);
	private static final Shape z=maxCount(10);


	@Test void testRetainAliases() { // required by formatters
		assertThat(optimize(alias("alias"))).as("alias").isEqualTo(alias("alias"));
	}


	@Test void testOptimizeMinCount() {

		assertThat(optimize(and(MinCount.minCount(10), MinCount.minCount(100)))).as("conjunction").isEqualTo(MinCount.minCount(100));
		assertThat(optimize(or(MinCount.minCount(10), MinCount.minCount(100)))).as("disjunction").isEqualTo(MinCount.minCount(10));

	}

	@Test void testOptimizeMaxCount() {

		assertThat(optimize(and(maxCount(10), maxCount(100)))).as("conjunction").isEqualTo(maxCount(10));
		assertThat(optimize(or(maxCount(10), maxCount(100)))).as("disjunction").isEqualTo(maxCount(100));

	}


	@Test void testOptimizeType() {

		assertThat(optimize(and(Datatype.datatype(Values.IRIType), Datatype.datatype(Values.ResourceType)))).as("conjunction / superclass").isEqualTo(Datatype.datatype(Values.IRIType));
		assertThat(optimize(and(Datatype.datatype(Values.LiteralType), Datatype.datatype(XMLSchema.STRING)))).as("conjunction / literal").isEqualTo(Datatype.datatype(XMLSchema.STRING));
		assertThat(optimize(and(Datatype.datatype(Values.ResourceType), Datatype.datatype(XMLSchema.STRING)))).as("conjunction / unrelated").isEqualTo(and(Datatype.datatype(Values.ResourceType), Datatype.datatype(XMLSchema.STRING)));

		assertThat(optimize(or(Datatype.datatype(Values.IRIType), Datatype.datatype(Values.ResourceType)))).as("disjunction / superclass").isEqualTo(Datatype.datatype(Values.ResourceType));
		assertThat(optimize(or(Datatype.datatype(Values.LiteralType), Datatype.datatype(RDF.NIL)))).as("disjunction / literal").isEqualTo(Datatype.datatype(Values.LiteralType));
		assertThat(optimize(or(Datatype.datatype(Values.ResourceType), Datatype.datatype(RDF.NIL)))).as("disjunction / unrelated").isEqualTo(or(Datatype.datatype(Values.ResourceType), Datatype.datatype(RDF.NIL)));

	}


	@Test void testOptimizeTraits() {

		assertThat(optimize(trait(RDF.VALUE, and(x)))).as("optimize nested shape").isEqualTo(trait(RDF.VALUE, x));
		assertThat(optimize(trait(RDF.VALUE, or()))).as("remove dead traits").isEqualTo(and());

		assertThat(optimize(and(alias("alias"), trait(RDF.VALUE, MinCount.minCount(1)), trait(RDF.VALUE, maxCount(3))))).as("merge conjunctive traits").isEqualTo(and(alias("alias"), trait(RDF.VALUE, and(MinCount.minCount(1), maxCount(3)))));

		assertThat(optimize(or(alias("alias"), trait(RDF.VALUE, MinCount.minCount(1)), trait(RDF.VALUE, maxCount(3))))).as("merge disjunctive traits").isEqualTo(or(alias("alias"), trait(RDF.VALUE, or(MinCount.minCount(1), maxCount(3)))));

	}

	@Test void testOptimizeConjunctions() {

		assertThat(optimize(and(or(), trait(RDF.TYPE)))).as("simplify constants").isEqualTo(or());
		assertThat(optimize(and(x))).as("unwrap singletons").isEqualTo(x);
		assertThat(optimize(and(x, x))).as("unwrap unique values").isEqualTo(x);
		assertThat(optimize(and(x, x, y))).as("remove duplicates").isEqualTo(and(x, y));
		assertThat(optimize(and(and(x), and(y, z)))).as("merge nested conjunction").isEqualTo(and(x, y, z));

	}

	@Test void testOptimizeDisjunctions() {

		assertThat(optimize(or(and(), trait(RDF.TYPE)))).as("simplify constants").isEqualTo(and());
		assertThat(optimize(or(x))).as("unwrap singletons").isEqualTo(x);
		assertThat(optimize(or(x, x))).as("unwrap unique values").isEqualTo(x);
		assertThat(optimize(or(x, x, y))).as("remove duplicates").isEqualTo(or(x, y));
		assertThat(optimize(or(or(x), or(y, z)))).as("merge nested disjunctions").isEqualTo(or(x, y, z));

	}

	@Test void testOptimizeOption() {

		assertThat(optimize(Option.option(and(), x, y))).as("always pass").isEqualTo(x);
		assertThat(optimize(Option.option(or(), x, y))).as("always fail").isEqualTo(y);

		assertThat(optimize(Option.option(x, y, y))).as("identical options").isEqualTo(y);

		assertThat(optimize(Option.option(and(x), y, z))).as("optimized test shape").isEqualTo(Option.option(x, y, z));
		assertThat(optimize(Option.option(x, and(y), z))).as("optimized pass shape").isEqualTo(Option.option(x, y, z));
		assertThat(optimize(Option.option(x, y, and(z)))).as("optimized fail shape").isEqualTo(Option.option(x, y, z));

		assertThat(optimize(Option.option(x, y, z))).as("material").isEqualTo(Option.option(x, y, z));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shape optimize(final Shape shape) {
		return shape.map(new Optimizer());
	}

}
