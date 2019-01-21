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
import com.metreeca.form.shapes.Datatype;
import com.metreeca.form.shapes.MinCount;
import com.metreeca.form.shapes.Option;
import com.metreeca.form.shifts.Step;
import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.shapes.Meta.alias;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.shapes.Virtual.virtual;

import static org.assertj.core.api.Assertions.assertThat;


final class OptimizerTest {

	private static final Shape x=Datatype.datatype(RDF.NIL);
	private static final Shape y=MinCount.minCount(1);
	private static final Shape z=maxCount(10);


	@Test void testRetainAliases() { // required by formatters
		assertThat((Object)alias("alias")).as("alias").isEqualTo(optimize(alias("alias")));
	}


	@Test void testOptimizeMinCount() {

		assertThat((Object)MinCount.minCount(100)).as("conjunction").isEqualTo(optimize(and(MinCount.minCount(10), MinCount.minCount(100))));
		assertThat((Object)MinCount.minCount(10)).as("disjunction").isEqualTo(optimize(or(MinCount.minCount(10), MinCount.minCount(100))));

	}

	@Test void testOptimizeMaxCount() {

		assertThat((Object)maxCount(10)).as("conjunction").isEqualTo(optimize(and(maxCount(10), maxCount(100))));
		assertThat((Object)maxCount(100)).as("disjunction").isEqualTo(optimize(or(maxCount(10), maxCount(100))));

	}


	@Test void testOptimizeType() {

		assertThat((Object)Datatype.datatype(Values.IRIType)).as("conjunction / superclass").isEqualTo(optimize(and(Datatype.datatype(Values.IRIType), Datatype.datatype(Values.ResoureType))));
		assertThat((Object)Datatype.datatype(XMLSchema.STRING)).as("conjunction / literal").isEqualTo(optimize(and(Datatype.datatype(Values.LiteralType), Datatype.datatype(XMLSchema.STRING))));
		assertThat((Object)and(Datatype.datatype(Values.ResoureType), Datatype.datatype(XMLSchema.STRING))).as("conjunction / unrelated").isEqualTo(optimize(and(Datatype.datatype(Values.ResoureType), Datatype.datatype(XMLSchema.STRING))));

		assertThat((Object)Datatype.datatype(Values.ResoureType)).as("disjunction / superclass").isEqualTo(optimize(or(Datatype.datatype(Values.IRIType), Datatype.datatype(Values.ResoureType))));
		assertThat((Object)Datatype.datatype(Values.LiteralType)).as("disjunction / literal").isEqualTo(optimize(or(Datatype.datatype(Values.LiteralType), Datatype.datatype(RDF.NIL))));
		assertThat((Object)or(Datatype.datatype(Values.ResoureType), Datatype.datatype(RDF.NIL))).as("disjunction / unrelated").isEqualTo(optimize(or(Datatype.datatype(Values.ResoureType), Datatype.datatype(RDF.NIL))));

	}


	@Test void testOptimizeTraits() {

		assertThat((Object)trait(RDF.VALUE, x)).as("optimize nested shape").isEqualTo(optimize(trait(RDF.VALUE, and(x))));
		assertThat((Object)and()).as("remove dead traits").isEqualTo(optimize(trait(RDF.VALUE, or())));

		assertThat((Object)and(alias("alias"), trait(RDF.VALUE, and(MinCount.minCount(1), maxCount(3))))).as("merge conjunctive traits").isEqualTo(optimize(and(alias("alias"), trait(RDF.VALUE, MinCount.minCount(1)), trait(RDF.VALUE, maxCount(3)))));

		assertThat((Object)or(alias("alias"), trait(RDF.VALUE, or(MinCount.minCount(1), maxCount(3))))).as("merge disjunctive traits").isEqualTo(optimize(or(alias("alias"), trait(RDF.VALUE, MinCount.minCount(1)), trait(RDF.VALUE, maxCount(3)))));

	}

	@Test void testOptimizeVirtuals() {

		assertThat((Object)virtual(trait(RDF.VALUE, x), Step.step(RDF.NIL))).as("optimize nested shape").isEqualTo(optimize(virtual(trait(RDF.VALUE, and(x)), Step.step(RDF.NIL))));

		assertThat((Object)and()).as("remove dead traits").isEqualTo(optimize(virtual(trait(RDF.VALUE, or()), Step.step(RDF.NIL))));

	}


	@Test void testOptimizeConjunctions() {

		assertThat((Object)or()).as("simplify constants").isEqualTo(optimize(and(or(), trait(RDF.TYPE))));
		assertThat((Object)x).as("unwrap singletons").isEqualTo(optimize(and(x)));
		assertThat((Object)x).as("unwrap unique values").isEqualTo(optimize(and(x, x)));
		assertThat((Object)and(x, y)).as("remove duplicates").isEqualTo(optimize(and(x, x, y)));
		assertThat((Object)and(x, y, z)).as("merge nested conjunction").isEqualTo(optimize(and(and(x), and(y, z))));

	}

	@Test void testOptimizeDisjunctions() {

		assertThat((Object)and()).as("simplify constants").isEqualTo(optimize(or(and(), trait(RDF.TYPE))));
		assertThat((Object)x).as("unwrap singletons").isEqualTo(optimize(or(x)));
		assertThat((Object)x).as("unwrap unique values").isEqualTo(optimize(or(x, x)));
		assertThat((Object)or(x, y)).as("remove duplicates").isEqualTo(optimize(or(x, x, y)));
		assertThat((Object)or(x, y, z)).as("merge nested disjunctions").isEqualTo(optimize(or(or(x), or(y, z))));

	}

	@Test void testOptimizeOption() {

		assertThat((Object)x).as("always pass").isEqualTo(optimize(Option.condition(and(), x, y)));
		assertThat((Object)y).as("always fail").isEqualTo(optimize(Option.condition(or(), x, y)));

		assertThat((Object)y).as("identical options").isEqualTo(optimize(Option.condition(x, y, y)));

		assertThat((Object)Option.condition(x, y, z)).as("optimized test shape").isEqualTo(optimize(Option.condition(and(x), y, z)));
		assertThat((Object)Option.condition(x, y, z)).as("optimized pass shape").isEqualTo(optimize(Option.condition(x, and(y), z)));
		assertThat((Object)Option.condition(x, y, z)).as("optimized fail shape").isEqualTo(optimize(Option.condition(x, y, and(z))));

		assertThat((Object)Option.condition(x, y, z)).as("material").isEqualTo(optimize(Option.condition(x, y, z)));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shape optimize(final Shape shape) {
		return shape.accept(new Optimizer());
	}

}
