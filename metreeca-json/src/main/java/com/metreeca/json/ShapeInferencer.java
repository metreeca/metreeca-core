/*
 * Copyright © 2013-2021 Metreeca srl
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
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.Range.range;
import static com.metreeca.json.shapes.When.when;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


final class ShapeInferencer extends Shape.Probe<Shape> {

	@Override public Shape probe(final Shape shape) { return shape; }


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
		final Shape datatype=types.size() == 1 ? datatype(types.iterator().next()) : and();

		return and(range, count, datatype);
	}

	@Override public Shape probe(final Lang lang) {
		return and(lang, datatype(RDF.LANGSTRING));
	}


	@Override public Shape probe(final All all) {
		return and(all, minCount(all.values().size()));
	}

	@Override public Shape probe(final Any any) {
		return and(any, minCount(1));
	}

	@Override public Shape probe(final Localized localized) {
		return and(localized, datatype(RDF.LANGSTRING));
	}


	@Override public Shape probe(final Field field) {

		final IRI iri=field.name();
		final Shape shape=field.shape().map(this);

		return iri.equals(RDF.TYPE) ? and(field.as(and(shape, datatype(IRIType))), datatype(ResourceType))
				: direct(iri) ? and(field.as(shape), datatype(ResourceType))
				: field.as(and(shape, datatype(ResourceType)));
	}


	@Override public Shape probe(final And and) {

		final Shape shape=and(and.shapes().stream().map(s -> s.map(this)).collect(toList()));

		final Boolean localized=shape.map(new Shape.Probe<Boolean>() {

			@Override public Boolean probe(final Localized localized) {
				return true;
			}

			@Override public Boolean probe(final And and) {
				return and.shapes().stream().map(this).anyMatch(b -> b);
			}

			@Override public Boolean probe(final Shape shape) {
				return false;
			}

		});

		final int langs=shape.map(new Shape.Probe<Integer>() {

			@Override public Integer probe(final Lang lang) {
				return lang.tags().size();
			}

			@Override public Integer probe(final And and) {
				return and.shapes().stream().map(this).max(Integer::compare).orElse(0);
			}

			@Override public Integer probe(final Shape shape) {
				return 0;
			}

		});

		return localized && langs > 0 ? and(shape, maxCount(langs)) : shape;
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
