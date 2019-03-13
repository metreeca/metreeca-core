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

import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.shapes.Field;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Any.any;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Guard.guard;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.Meta.alias;
import static com.metreeca.form.shapes.Meta.label;
import static com.metreeca.form.shapes.MinCount.minCount;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.When.when;

import static org.assertj.core.api.Assertions.assertThat;


final class OptimizerTest {

	private static final Shape x=datatype(RDF.NIL);
	private static final Shape y=minCount(1);
	private static final Shape z=maxCount(10);


	@Test void testOptimizeMeta() {

		assertThat(optimize(and(label("label"), label("label"))))
				.as("collapse duplicated metadata")
				.isEqualTo(label("label"));

	}

	@Test void testRetainAliases() { // required by formatters
		assertThat(optimize(alias("alias")))
				.as("alias")
				.isEqualTo(alias("alias"));
	}


	@Test void testOptimizeDatatype() {

		assertThat(optimize(and(datatype(Form.IRIType), datatype(Form.ResourceType))))
				.as("conjunction / superclass")
				.isEqualTo(datatype(Form.IRIType));

		assertThat(optimize(and(datatype(Form.LiteralType), datatype(XMLSchema.STRING))))
				.as("conjunction / literal")
				.isEqualTo(datatype(XMLSchema.STRING));

		assertThat(optimize(and(datatype(Form.ResourceType), datatype(XMLSchema.STRING))))
				.as("conjunction / unrelated")
				.isEqualTo(and(datatype(Form.ResourceType), datatype(XMLSchema.STRING)));

		assertThat(optimize(or(datatype(Form.IRIType), datatype(Form.ResourceType))))
				.as("disjunction / superclass")
				.isEqualTo(datatype(Form.ResourceType));

		assertThat(optimize(or(datatype(Form.LiteralType), datatype(RDF.NIL))))
				.as("disjunction / literal")
				.isEqualTo(datatype(Form.LiteralType));

		assertThat(optimize(or(datatype(Form.ResourceType), datatype(RDF.NIL))))
				.as("disjunction / unrelated")
				.isEqualTo(or(datatype(Form.ResourceType), datatype(RDF.NIL)));

	}


	@Test void testOptimizeMinCount() {

		assertThat(optimize(and(minCount(10), minCount(100)))).as("conjunction").isEqualTo(minCount(100));
		assertThat(optimize(or(minCount(10), minCount(100)))).as("disjunction").isEqualTo(minCount(10));

	}

	@Test void testOptimizeMaxCount() {

		assertThat(optimize(and(maxCount(10), maxCount(100)))).as("conjunction").isEqualTo(maxCount(10));
		assertThat(optimize(or(maxCount(10), maxCount(100)))).as("disjunction").isEqualTo(maxCount(100));

	}


	@Test void testOptimizeAll() {

		assertThat(optimize(all())).as("empty").isEqualTo(and());

	}

	@Test void testOptimizeAny() {

		assertThat(optimize(any())).as("empty").isEqualTo(or());

	}



	@Test void testOptimizeFields() {

		assertThat(optimize(field(RDF.VALUE, and(x))))
				.as("optimize nested shape")
				.isEqualTo(field(RDF.VALUE, x));

		assertThat(optimize(field(RDF.VALUE, or())))
				.as("remove dead fields")
				.isEqualTo(and());


		assertThat(optimize(and(alias("alias"), field(RDF.VALUE, minCount(1)), field(RDF.VALUE, maxCount(3)))))
				.as("merge conjunctive fields")
				.isEqualTo(and(alias("alias"), field(RDF.VALUE, and(minCount(1), maxCount(3)))));

		assertThat(optimize(or(alias("alias"), field(RDF.VALUE, minCount(1)), field(RDF.VALUE, maxCount(3)))))
				.as("merge disjunctive fields")
				.isEqualTo(or(alias("alias"), field(RDF.VALUE, or(minCount(1), maxCount(3)))));

	}


	@Test void testOptimizeAnd() {

		assertThat(optimize(and(or(), Field.field(RDF.TYPE)))).as("simplify constants").isEqualTo(or());
		assertThat(optimize(and(x))).as("unwrap singletons").isEqualTo(x);
		assertThat(optimize(and(x, x))).as("unwrap unique values").isEqualTo(x);
		assertThat(optimize(and(x, x, y))).as("remove duplicates").isEqualTo(and(x, y));
		assertThat(optimize(and(and(x), and(y, z)))).as("merge nested conjunction").isEqualTo(and(x, y, z));

	}

	@Test void testOptimizeOr() {

		assertThat(optimize(or(and(), Field.field(RDF.TYPE)))).as("simplify constants").isEqualTo(and());
		assertThat(optimize(or(x))).as("unwrap singletons").isEqualTo(x);
		assertThat(optimize(or(x, x))).as("unwrap unique values").isEqualTo(x);
		assertThat(optimize(or(x, x, y))).as("remove duplicates").isEqualTo(or(x, y));
		assertThat(optimize(or(or(x), or(y, z)))).as("merge nested disjunctions").isEqualTo(or(x, y, z));

	}

	@Test void testOptimizeWhen() {

		assertThat(optimize(when(and(), x, y))).as("always pass").isEqualTo(x);
		assertThat(optimize(when(or(), x, y))).as("always fail").isEqualTo(y);

		final Shape x=guard(RDF.VALUE, RDF.NIL); // !!! remove when filtering constraints are accepted as tests

		assertThat(optimize(when(x, y, y))).as("identical options").isEqualTo(y);

		assertThat(optimize(when(and(x), y, z))).as("optimized test shape").isEqualTo(when(x, y, z));

		assertThat(optimize(when(x, and(y), z))).as("optimized pass shape").isEqualTo(when(x, y, z));
		assertThat(optimize(when(x, y, and(z)))).as("optimized fail shape").isEqualTo(when(x, y, z));

		assertThat(optimize(when(x, y, z))).as("material").isEqualTo(when(x, y, z));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shape optimize(final Shape shape) {
		return shape.map(new Optimizer());
	}

}
