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

package com.metreeca.form.probes;

import com.metreeca.form.Shape;
import com.metreeca.form.shapes.Field;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Any.any;
import static com.metreeca.form.shapes.Clazz.clazz;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.Like.like;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.form.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.form.shapes.MaxLength.maxLength;
import static com.metreeca.form.shapes.MinCount.minCount;
import static com.metreeca.form.shapes.MinExclusive.minExclusive;
import static com.metreeca.form.shapes.MinInclusive.minInclusive;
import static com.metreeca.form.shapes.MinLength.minLength;
import static com.metreeca.form.shapes.When.when;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.Pattern.pattern;
import static com.metreeca.form.shapes.Field.field;

import static org.assertj.core.api.Assertions.assertThat;


final class PrunerTest {

	@Test void testConstraint() {

		assertThat(prune(minCount(1))).as("MinCount").isEqualTo(minCount(1));
		assertThat(prune(maxCount(1))).as("MaxCount").isEqualTo(maxCount(1));

		assertThat(prune(clazz(RDF.NIL))).as("Clazz").isEqualTo(clazz(RDF.NIL));
		assertThat(prune(datatype(RDF.NIL))).as("Type").isEqualTo(datatype(RDF.NIL));

		assertThat(prune(all(RDF.NIL))).as("Universal").isEqualTo(all(RDF.NIL));
		assertThat(prune(any(RDF.NIL))).as("Universal").isEqualTo(any(RDF.NIL));

		assertThat(prune(minInclusive(RDF.NIL))).as("MinInclusive").isEqualTo(minInclusive(RDF.NIL));
		assertThat(prune(maxInclusive(RDF.NIL))).as("MaxInclusive").isEqualTo(maxInclusive(RDF.NIL));
		assertThat(prune(minExclusive(RDF.NIL))).as("MinExclusive").isEqualTo(minExclusive(RDF.NIL));
		assertThat(prune(maxExclusive(RDF.NIL))).as("MaxExclusive").isEqualTo(maxExclusive(RDF.NIL));

		assertThat(prune(pattern("pattern"))).as("Pattern").isEqualTo(pattern("pattern"));
		assertThat(prune(like("pattern"))).as("Like").isEqualTo(like("pattern"));
		assertThat(prune(minLength(1))).as("MinLength").isEqualTo(minLength(1));
		assertThat(prune(maxLength(1))).as("MaxLength").isEqualTo(maxLength(1));

	}

	@Test void testField() {

		assertThat(prune(Field.field(RDF.VALUE))).as("dead").isEqualTo(and());
		assertThat(prune(field(RDF.VALUE, maxCount(1)))).as("live").isEqualTo(field(RDF.VALUE, maxCount(1)));

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
