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

package com.metreeca.rdf4j.assets;

import com.metreeca.core.*;
import com.metreeca.json.Shape;
import com.metreeca.json.probes.Traverser;
import com.metreeca.json.shapes.*;

import org.eclipse.rdf4j.model.*;

import java.util.*;
import java.util.stream.Stream;

import static com.metreeca.rdf.Values.direct;
import static com.metreeca.rdf.Values.inverse;
import static com.metreeca.rdf.Values.iri;
import static com.metreeca.rdf.formats.RDFFormat.iri;
import static com.metreeca.rdf.formats.RDFFormat.rdf;
import static com.metreeca.rest.assets.Engine.shape;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


final class GraphTrimmer extends GraphProcessor {

	<M extends Message<M>> Either<MessageException, M> trim(final M message) {
		return message.body(rdf()).map(rdf -> message.body(rdf(),
				trim(iri(message.item()), convey(message.attribute(shape())), rdf)
		));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Collection<Statement> trim(final Value resource, final Shape shape, final Collection<Statement> model) {
		return shape.map(new TrimmerProbe(singleton(resource), model)).collect(toList());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TrimmerProbe extends Traverser<Stream<Statement>> {

		private final Set<Value> focus;
		private final Collection<Statement> model;


		private TrimmerProbe(final Set<Value> focus, final Collection<Statement> model) {
			this.focus=focus;
			this.model=model;
		}


		@Override public Stream<Statement> probe(final Shape shape) {
			return Stream.empty();
		}


		@Override public Stream<Statement> probe(final Field field) {

			final IRI iri=iri(field.getName());
			final Shape shape=field.getShape();

			final IRI inverse=inverse(iri);

			final List<Statement> statements=model.stream()
					.filter(direct(iri)
							? s -> focus.contains(s.getSubject()) && s.getPredicate().equals(iri)
							: s -> focus.contains(s.getObject()) && s.getPredicate().equals(inverse)
					)
					.collect(toList());

			final Set<Value> focus=statements.stream()
					.map(direct(iri) ? Statement::getObject : Statement::getSubject)
					.collect(toSet());

			return Stream.concat(
					statements.stream(),
					shape.map(new TrimmerProbe(focus, model))
			);
		}


		@Override public Stream<Statement> probe(final And and) {
			return and.getShapes().stream().flatMap(s -> s.map(this));
		}

		@Override public Stream<Statement> probe(final Or or) {
			return or.getShapes().stream().flatMap(s -> s.map(this));
		}

		@Override public Stream<Statement> probe(final When when) {
			return Stream.of(when.getPass(), when.getFail()).flatMap(s -> s.map(this));
		}

	}

}
