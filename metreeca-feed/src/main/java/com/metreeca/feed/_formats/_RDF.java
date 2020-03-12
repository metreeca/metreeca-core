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

package com.metreeca.feed._formats;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.stream.Stream;


public final class _RDF {

	public static final class Cell implements Value {

		private final Resource subject;

		private final Model model=new LinkedHashModel();


		public Cell(final Resource subject) {

			if ( subject == null ) {
				throw new NullPointerException("null subject");
			}

			this.subject=subject;
		}


		public Resource focus() {
			return subject;
		}

		public Model model() {
			return model.unmodifiable();
		}


		public Cell insert(final IRI predicate, final Value object) {

			if ( predicate == null ) {
				throw new NullPointerException("null predicate");
			}

			if ( object instanceof Cell ) {

				model.add(subject, predicate, ((Cell)object).subject);
				model.addAll(((Cell)object).model);

			} else if ( object != null ) {

				model.add(subject, predicate, object);

			}

			return this;
		}

		public Cell insert(final IRI predicate, final Value... objects) {

			if ( predicate != null && objects != null ) {
				for (final Value object : objects) {
					insert(predicate, object);
				}
			}

			return this;
		}

		public Cell insert(final IRI predicate, final Optional<? extends Value> object) {

			if ( predicate != null && object != null ) {
				object.ifPresent(value -> insert(predicate, value));
			}

			return this;
		}

		public Cell insert(final IRI predicate, final Stream<? extends Value> objects) {

			if ( predicate != null && objects != null ) {
				objects.forEach(value -> insert(predicate, value));
			}

			return this;
		}


		@Override public String stringValue() {
			return subject.stringValue();
		}

		@Override public String toString() {
			try (final StringWriter writer=new StringWriter()) {

				Rio.write(model, writer, subject.stringValue(), RDFFormat.TURTLE);

				return writer.toString();

			} catch ( final URISyntaxException e ) {
				throw new RuntimeException(e);
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private _RDF() {}

}
