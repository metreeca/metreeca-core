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

package com.metreeca.tree.probes;

import com.metreeca.tree.Shape;

import org.junit.jupiter.api.Test;

import static com.metreeca.tree.shapes.All.all;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Any.any;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.Guard.guard;
import static com.metreeca.tree.shapes.MaxCount.maxCount;
import static com.metreeca.tree.shapes.Meta.alias;
import static com.metreeca.tree.shapes.Meta.label;
import static com.metreeca.tree.shapes.MinCount.minCount;
import static com.metreeca.tree.shapes.Or.or;
import static com.metreeca.tree.shapes.Type.type;
import static com.metreeca.tree.shapes.When.when;

import static org.assertj.core.api.Assertions.assertThat;


final class OptimizerTest {

	private static final Shape x=type("nil");
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

		assertThat(optimize(field("value", and(x))))
				.as("optimize nested shape")
				.isEqualTo(field("value", x));

		assertThat(optimize(field("value", or())))
				.as("remove dead fields")
				.isEqualTo(and());


		assertThat(optimize(and(alias("alias"), field("value", minCount(1)), field("value", maxCount(3)))))
				.as("merge conjunctive fields")
				.isEqualTo(and(alias("alias"), field("value", and(minCount(1), maxCount(3)))));

		assertThat(optimize(or(alias("alias"), field("value", minCount(1)), field("value", maxCount(3)))))
				.as("merge disjunctive fields")
				.isEqualTo(or(alias("alias"), field("value", or(minCount(1), maxCount(3)))));

	}


	@Test void testOptimizeAnd() {

		assertThat(optimize(and(or(), field("type", and())))).as("simplify constants").isEqualTo(or());
		assertThat(optimize(and(x))).as("unwrap singletons").isEqualTo(x);
		assertThat(optimize(and(x, x))).as("unwrap unique values").isEqualTo(x);
		assertThat(optimize(and(x, x, y))).as("remove duplicates").isEqualTo(and(x, y));
		assertThat(optimize(and(and(x), and(y, z)))).as("merge nested conjunction").isEqualTo(and(x, y, z));

	}

	@Test void testOptimizeOr() {

		assertThat(optimize(or(and(), field("type", and())))).as("simplify constants").isEqualTo(and());
		assertThat(optimize(or(x))).as("unwrap singletons").isEqualTo(x);
		assertThat(optimize(or(x, x))).as("unwrap unique values").isEqualTo(x);
		assertThat(optimize(or(x, x, y))).as("remove duplicates").isEqualTo(or(x, y));
		assertThat(optimize(or(or(x), or(y, z)))).as("merge nested disjunctions").isEqualTo(or(x, y, z));

	}

	@Test void testOptimizeWhen() {

		assertThat(optimize(when(and(), x, y))).as("always pass").isEqualTo(x);
		assertThat(optimize(when(or(), x, y))).as("always fail").isEqualTo(y);

		final Shape x=guard("value", "nil"); // !!! remove when filtering constraints are accepted as tests

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
