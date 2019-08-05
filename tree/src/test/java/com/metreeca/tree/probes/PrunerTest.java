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
import com.metreeca.tree.shapes.Clazz;
import com.metreeca.tree.shapes.Field;

import org.junit.jupiter.api.Test;

import static com.metreeca.tree.shapes.All.all;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Any.any;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.Like.like;
import static com.metreeca.tree.shapes.MaxCount.maxCount;
import static com.metreeca.tree.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.tree.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.tree.shapes.MaxLength.maxLength;
import static com.metreeca.tree.shapes.MinCount.minCount;
import static com.metreeca.tree.shapes.MinExclusive.minExclusive;
import static com.metreeca.tree.shapes.MinInclusive.minInclusive;
import static com.metreeca.tree.shapes.MinLength.minLength;
import static com.metreeca.tree.shapes.Or.or;
import static com.metreeca.tree.shapes.Pattern.pattern;
import static com.metreeca.tree.shapes.Type.type;
import static com.metreeca.tree.shapes.When.when;

import static org.assertj.core.api.Assertions.assertThat;


final class PrunerTest {

	@Test void testConstraint() {

		assertThat(prune(minCount(1))).as("MinCount").isEqualTo(minCount(1));
		assertThat(prune(maxCount(1))).as("MaxCount").isEqualTo(maxCount(1));

		assertThat(prune(Clazz.clazz("nil"))).as("Clazz").isEqualTo(Clazz.clazz("nil"));
		assertThat(prune(type("nil"))).as("Type").isEqualTo(type("nil"));

		assertThat(prune(all("nil"))).as("Universal").isEqualTo(all("nil"));
		assertThat(prune(any("nil"))).as("Universal").isEqualTo(any("nil"));

		assertThat(prune(minInclusive("nil"))).as("MinInclusive").isEqualTo(minInclusive("nil"));
		assertThat(prune(maxInclusive("nil"))).as("MaxInclusive").isEqualTo(maxInclusive("nil"));
		assertThat(prune(minExclusive("nil"))).as("MinExclusive").isEqualTo(minExclusive("nil"));
		assertThat(prune(maxExclusive("nil"))).as("MaxExclusive").isEqualTo(maxExclusive("nil"));

		assertThat(prune(pattern("pattern"))).as("Pattern").isEqualTo(pattern("pattern"));
		assertThat(prune(like("pattern"))).as("Like").isEqualTo(like("pattern"));
		assertThat(prune(minLength(1))).as("MinLength").isEqualTo(minLength(1));
		assertThat(prune(maxLength(1))).as("MaxLength").isEqualTo(maxLength(1));

	}

	@Test void testField() {

		assertThat(prune(Field.field("value"))).as("dead").isEqualTo(and());
		assertThat(prune(field("value", maxCount(1)))).as("live").isEqualTo(field("value", maxCount(1)));

	}

	@Test void testConjunction() {

		assertThat(prune(and())).as("empty").isEqualTo(and());
		assertThat(prune(and(and()))).as("dead").isEqualTo(and());

		assertThat(prune(and(maxCount(1)))).as("live").isEqualTo(and(maxCount(1)));

	}

	@Test void testDisjunction() {

		assertThat(prune(or())).as("empty").isEqualTo(and());
		assertThat(prune(or(or()))).as("dead").isEqualTo(and());

		assertThat(prune(or(maxCount(1)))).as("live").isEqualTo(or(maxCount(1)));

	}

	@Test void testOptions() {

		assertThat(prune(or())).as("empty").isEqualTo(and());
		assertThat(prune(or(or()))).as("dead").isEqualTo(and());

		// test shape is not pruned
		assertThat(prune(when(or(), maxCount(1), or()))).as("live").isEqualTo(when(or(), maxCount(1), and()));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shape prune(final Shape shape) {
		return shape.map(new Pruner());
	}

}
