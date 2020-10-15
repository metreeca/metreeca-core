/*
 * Copyright © 2013-2020 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.json;

import com.metreeca.json.shapes.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.util.Set;

import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.Range.range;
import static com.metreeca.json.shapes.When.when;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


final class ShapeInferencer extends Shape.Probe<Shape> {

	@Override public Shape probe(final Shape shape) { return shape; }


	@Override public Shape probe(final Meta meta) {
		return meta.label().equals("hint") ? and(meta, datatype(ResourceType)) : meta;
	}


	@Override public Shape probe(final Datatype datatype) {
		return datatype.iri().equals(XSD.BOOLEAN) ? and(datatype,
				range(literal(false), literal(true)), maxCount(1)
		) : datatype;
	}

	@Override public Shape probe(final Clazz clazz) {
		return and(clazz, datatype(ResourceType));
	}

	@Override public Shape probe(final Range range) {

		final Set<Value> values=range.values();
		final Set<IRI> types=values.stream().map(Values::type).collect(toSet());

		final Shape count=maxCount(values.size());
		final Shape type=types.size() == 1 ? datatype(types.iterator().next()) : and();

		return and(range, count, type);
	}


	@Override public Shape probe(final All all) {
		return and(all, minCount(all.values().size()));
	}

	@Override public Shape probe(final Any any) {
		return and(any, minCount(1));
	}


	@Override public Shape probe(final Field field) {

		final IRI iri=field.name();
		final Shape shape=field.shape().map(this);

		return iri.equals(RDF.TYPE) ? and(field(iri, and(shape, datatype(ResourceType))), datatype(IRIType))
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
