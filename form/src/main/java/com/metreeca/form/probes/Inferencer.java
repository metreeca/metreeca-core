/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

import com.metreeca.form.shapes.*;
import com.metreeca.form.Shape;
import com.metreeca.form.shapes.*;
import com.metreeca.form.shifts.Step;
import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import java.util.Set;

import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.things.Values.literal;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


/**
 * Shape inferencer.
 *
 * <p>Recursively expands shapes with additional implied constraints.</p>
 */
public final class Inferencer extends Shape.Probe<Shape> {

	@Override protected Shape fallback(final Shape shape) { return shape; }


	@Override public Shape visit(final Hint hint) {
		return And.and(hint, Datatype.datatype(Values.ResoureType));
	}

	@Override public Shape visit(final Group group) {
		return Group.group(group.getShape().accept(this));
	}


	@Override public Shape visit(final All all) {
		return And.and(all, MinCount.minCount(all.getValues().size()));
	}

	@Override public Shape visit(final Any any) {
		return And.and(any, MinCount.minCount(1));
	}

	@Override public Shape visit(final In in) {

		final Set<Value> values=in.getValues();
		final Set<IRI> types=values.stream().map(Values::type).collect(toSet());

		final Shape count=maxCount(values.size());
		final Shape type=types.size() == 1 ? Datatype.datatype(types.iterator().next()) : And.and();

		return And.and(in, count, type);
	}


	@Override public Shape visit(final Datatype datatype) {
		return datatype.getIRI().equals(XMLSchema.BOOLEAN) ? And.and(datatype,
				In.in(literal(false), literal(true)), maxCount(1)
		) : datatype;
	}

	@Override public Shape visit(final Clazz clazz) {
		return And.and(clazz, Datatype.datatype(Values.ResoureType));
	}


	@Override public Shape visit(final Trait trait) {

		final Step step=trait.getStep();
		final Shape shape=trait.getShape().accept(this);

		return step.getIRI().equals(RDF.TYPE) ? And.and(trait(step, And.and(shape, Datatype.datatype(Values.ResoureType))), Datatype.datatype(Values.ResoureType))
				: step.isInverse() ? trait(step, And.and(shape, Datatype.datatype(Values.ResoureType)))
				: And.and(trait(step, shape), Datatype.datatype(Values.ResoureType));
	}

	@Override public Shape visit(final Virtual virtual) {

		return virtual;

		// !!! currently unable to implement as trait inference returns Shape whereas virtual constructor expects Trait

		// !!! return Virtual.virtual(virtual.getTrait().accept((Shape.Probe<Shape>)this), virtual.getShift());
	}

	@Override public Shape visit(final And and) {
		return And.and(and.getShapes().stream().map(s -> s.accept(this)).collect(toList()));
	}

	@Override public Shape visit(final Or or) {
		return Or.or(or.getShapes().stream().map(s -> s.accept(this)).collect(toList()));
	}

	@Override public Shape visit(final Test test) {
		return Test.test(test.getTest().accept(this), test.getPass().accept(this), test.getFail().accept(this));
	}

}
