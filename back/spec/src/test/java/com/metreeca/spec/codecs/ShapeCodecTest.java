/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.spec.codecs;

import com.metreeca.spec.Shape;
import com.metreeca.spec.shapes.*;
import com.metreeca.spec.shifts.Step;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static com.metreeca.jeep.rdf.Values.literal;
import static com.metreeca.spec.shapes.Alias.alias;
import static com.metreeca.spec.shapes.All.all;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Any.any;
import static com.metreeca.spec.shapes.Default.dflt;
import static com.metreeca.spec.shapes.Group.group;
import static com.metreeca.spec.shapes.Hint.hint;
import static com.metreeca.spec.shapes.Like.like;
import static com.metreeca.spec.shapes.MaxCount.maxCount;
import static com.metreeca.spec.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.spec.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.spec.shapes.MinExclusive.minExclusive;
import static com.metreeca.spec.shapes.MinInclusive.minInclusive;
import static com.metreeca.spec.shapes.Notes.notes;
import static com.metreeca.spec.shapes.Or.or;
import static com.metreeca.spec.shapes.Pattern.pattern;
import static com.metreeca.spec.shapes.Placeholder.placeholder;
import static com.metreeca.spec.shapes.Test.test;
import static com.metreeca.spec.shapes.Trait.trait;
import static com.metreeca.spec.shapes.Virtual.virtual;
import static com.metreeca.spec.shapes.When.when;

import static org.junit.Assert.assertEquals;


public final class ShapeCodecTest {

	@Test public void testAnnotations() {

		assertCoded("alias", alias("alias"));
		assertCoded("label", Label.label("label"));
		assertCoded("notes", notes("notes"));
		assertCoded("placeholder", placeholder("placeholder"));
		assertCoded("default", dflt(literal("default")));
		assertCoded("default", hint(RDF.NIL));
		assertCoded("group", group(MinCount.minCount(10)));

	}

	@Test public void testGlobals() {

		assertCoded("minCount", MinCount.minCount(10));
		assertCoded("maxCount", maxCount(10));
		assertCoded("universal", all(RDF.VALUE));
		assertCoded("existential", any(RDF.VALUE));
		assertCoded("range", In.in(RDF.VALUE));

	}

	@Test public void testLocals() {

		assertCoded("type", Datatype.datatype(RDF.NIL));
		assertCoded("class", Clazz.clazz(RDF.NIL));
		assertCoded("minExclusive", minExclusive(literal(10)));
		assertCoded("maxExclusive", maxExclusive(literal(10)));
		assertCoded("minInclusive", minInclusive(literal(10)));
		assertCoded("maxInclusive", maxInclusive(literal(10)));
		assertCoded("pattern", pattern("expression"));
		assertCoded("pattern with flags", pattern("expression", "flags"));
		assertCoded("like", like("keywords"));
		assertCoded("minLength", MinLength.minLength(10));
		assertCoded("maxLength", MaxLength.maxLength(10));

	}

	@Test public void testStructurals() {

		assertCoded("direct trait", trait(RDF.VALUE));
		assertCoded("inverse trait", trait(Step.step(RDF.VALUE, true)));
		assertCoded("shaped trait", trait(RDF.VALUE, MinCount.minCount(10)));

		assertCoded("virtual trait", virtual(trait(RDF.VALUE, MinCount.minCount(10)), Step.step(RDF.NIL)));

	}

	@Test public void testLogicals() {

		assertCoded("empty conjunction", and());
		assertCoded("singleton conjunction", and(MinCount.minCount(1)));
		assertCoded("proper conjunction", and(MinCount.minCount(1), MinCount.minCount(10)));

		assertCoded("empty disjunction", or());
		assertCoded("singleton disjunction", or(MinCount.minCount(1)));
		assertCoded("proper disjunction", or(MinCount.minCount(1), MinCount.minCount(10)));

		assertCoded("one-way option", test(and(), MinCount.minCount(1)));
		assertCoded("two-way option", test(and(), MinCount.minCount(1), MinCount.minCount(10)));

		assertCoded("singleton condition", when(RDF.VALUE, literal(1)));
		assertCoded("proper condition", when(RDF.VALUE, literal(1), literal(10)));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void assertCoded(final String message, final Shape shape) {

		final ShapeCodec codec=new ShapeCodec();
		final Collection<Statement> model=new ArrayList<>();

		try {

			assertEquals(message, shape, codec.decode(codec.encode(shape, model), model));

		} finally {
			Rio.write(model, System.out, RDFFormat.TURTLE);
		}
	}

}
