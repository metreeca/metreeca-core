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
import com.metreeca.tree.shapes.*;

import java.util.Set;

import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.MaxCount.maxCount;
import static com.metreeca.tree.shapes.MinCount.minCount;
import static com.metreeca.tree.shapes.Or.or;
import static com.metreeca.tree.shapes.When.when;

import static java.util.stream.Collectors.toList;


/**
 * Shape inferencer.
 *
 * <p>Recursively expands shapes with additional implied constraints.</p>
 */
public final class Inferencer extends Inspector<Shape> {

	@Override public Shape probe(final Shape shape) { return shape; }


	//@Override public Shape probe(final Meta meta) {
	//	return meta.getValue().equals(Shape.Hint) ? and(meta, type(Form.ResourceType)) : meta;
	//}


	//@Override public Shape probe(final Type type) {
	//	return type.getType().equals(XMLSchema.BOOLEAN) ? and(type,
	//			In.in(literal(false), literal(true)), maxCount(1)
	//	) : type;
	//}

	//@Override public Shape probe(final Kind kind) {
	//	return and(kind, type(Form.ResourceType));
	//}


	@Override public Shape probe(final All all) {
		return and(all, minCount(all.getValues().size()));
	}

	@Override public Shape probe(final Any any) {
		return and(any, minCount(1));
	}

	@Override public Shape probe(final In in) {

		final Set<Object> values=in.getValues();
		//final Set<IRI> types=values.stream().map(Values::type).collect(toSet());

		final Shape count=maxCount(values.size());
		//final Shape type=types.size() == 1 ? type(types.iterator().next()) : and();

		return and(in, count/*, type*/);
	}


	@Override public Shape probe(final Field field) {

		final String name=field.getName();
		final Shape shape=field.getShape().map(this);

		return field(name, shape);

		//return name.equals(RDF.TYPE) ? and(field(name, and(shape, datatype(Form.ResourceType))), datatype(Form.ResourceType))
		//		: field(name, and(shape, type(Form.ResourceType)));

	}


	@Override public Shape probe(final And and) {
		return and(and.getShapes().stream().map(s -> s.map(this)).collect(toList()));
	}

	@Override public Shape probe(final Or or) {
		return or(or.getShapes().stream().map(s -> s.map(this)).collect(toList()));
	}

	@Override public Shape probe(final When when) {
		return when(
				when.getTest().map(this),
				when.getPass().map(this),
				when.getFail().map(this)
		);
	}

}
