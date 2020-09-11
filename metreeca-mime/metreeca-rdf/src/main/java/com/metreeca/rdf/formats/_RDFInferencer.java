/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.rdf.formats;

import com.metreeca.json.Shape;
import com.metreeca.json.probes.Inspector;
import com.metreeca.json.shapes.*;
import com.metreeca.rdf.Values;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.util.Set;

import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.In.in;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.When.when;
import static com.metreeca.rdf.Values.*;
import static com.metreeca.rdf.formats._RDFCasts._iri;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


/**
 * Shape inferencer.
 *
 * <p>Recursively expands shapes with additional implied constraints.</p>
 */
final class _RDFInferencer extends Inspector<Shape> {

	@Override public Shape probe(final Shape shape) { return shape; }


	@Override public Shape probe(final Meta meta) {
		return meta.label().equals(Shape.Hint) ? and(meta, datatype(ResourceType)) : meta;
	}


	@Override public Shape probe(final Datatype datatype) {
		return datatype.id().equals(XSD.BOOLEAN) ? and(datatype,
				in(literal(false), literal(true)), maxCount(1)
		) : datatype;
	}

	@Override public Shape probe(final Clazz clazz) {
		return and(clazz, datatype(ResourceType));
	}


	@Override public Shape probe(final All all) {
		return and(all, minCount(all.values().size()));
	}

	@Override public Shape probe(final Any any) {
		return and(any, minCount(1));
	}

	@Override public Shape probe(final In in) {

		final Set<Object> values=in.values();
		final Set<Object> types=values.stream().map(_RDFCasts::_value).map(Values::type).collect(toSet());

		final Shape count=maxCount(values.size());
		final Shape type=types.size() == 1 ? datatype(types.iterator().next()) : and();

		return and(in, count, type);
	}


	@Override public Shape probe(final Field field) {

		final IRI iri=_iri(field.name());
		final Shape shape=field.shape().map(this);

		return iri.equals(RDF.TYPE) ? and(field(iri, and(shape, datatype(ResourceType))), datatype(ResourceType))
				: direct(iri) ? and(field(iri, shape), datatype(ResourceType))
				: field(iri, and(shape, datatype(ResourceType)));
	}


	@Override public Shape probe(final And and) {
		return and(and.shapes().stream().map(s -> s.map(this)).collect(toList()));
	}

	@Override public Shape probe(final Or or) {
		return or(or.shapes().stream().map(s -> s.map(this)).collect(toList()));
	}

	@Override public Shape probe(final When when) {
		return when(
				when.test().map(this),
				when.pass().map(this),
				when.fail().map(this)
		);
	}

}
