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
import com.metreeca.form.shapes.*;
import com.metreeca.form.shifts.Step;
import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import java.util.Set;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.MinCount.minCount;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.Option.condition;
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


	@Override public Shape visit(final Meta meta) {
		return meta.getIRI().equals(Form.Hint) ? and(meta, datatype(Values.ResoureType)) : meta;
	}


	@Override public Shape visit(final All all) {
		return and(all, minCount(all.getValues().size()));
	}

	@Override public Shape visit(final Any any) {
		return and(any, minCount(1));
	}

	@Override public Shape visit(final In in) {

		final Set<Value> values=in.getValues();
		final Set<IRI> types=values.stream().map(Values::type).collect(toSet());

		final Shape count=maxCount(values.size());
		final Shape type=types.size() == 1 ? datatype(types.iterator().next()) : and();

		return and(in, count, type);
	}


	@Override public Shape visit(final Datatype datatype) {
		return datatype.getIRI().equals(XMLSchema.BOOLEAN) ? and(datatype,
				In.in(literal(false), literal(true)), maxCount(1)
		) : datatype;
	}

	@Override public Shape visit(final Clazz clazz) {
		return and(clazz, datatype(Values.ResoureType));
	}


	@Override public Shape visit(final Trait trait) {

		final Step step=trait.getStep();
		final Shape shape=trait.getShape().accept(this);

		return step.getIRI().equals(RDF.TYPE) ? and(trait(step, and(shape, datatype(Values.ResoureType))), datatype(Values.ResoureType))
				: step.isInverse() ? trait(step, and(shape, datatype(Values.ResoureType)))
				: and(trait(step, shape), datatype(Values.ResoureType));
	}

	@Override public Shape visit(final Virtual virtual) {

		return virtual;

		// !!! currently unable to implement as trait inference returns Shape whereas virtual constructor expects Trait

		// !!! return Virtual.virtual(virtual.getTrait().accept((Shape.Probe<Shape>)this), virtual.getShift());
	}

	@Override public Shape visit(final And and) {
		return and(and.getShapes().stream().map(s -> s.accept(this)).collect(toList()));
	}

	@Override public Shape visit(final Or or) {
		return or(or.getShapes().stream().map(s -> s.accept(this)).collect(toList()));
	}

	@Override public Shape visit(final Option option) {
		return condition(
				option.getTest().accept(this),
				option.getPass().accept(this),
				option.getFail().accept(this)
		);
	}

}
