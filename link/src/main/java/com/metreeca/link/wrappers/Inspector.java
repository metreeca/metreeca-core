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

package com.metreeca.link.wrappers;

import com.metreeca.link.*;
import com.metreeca.spec.Shape;
import com.metreeca.spec.Spec;
import com.metreeca.spec.codecs.ShapeCodec;
import com.metreeca.spec.probes.Inferencer;
import com.metreeca.spec.probes.Optimizer;
import com.metreeca.spec.things.Lists;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.LDP;

import java.util.ArrayList;
import java.util.Collection;

import static com.metreeca.spec.Shape.*;
import static com.metreeca.spec.things.Values.iri;
import static com.metreeca.spec.things.Values.statement;

import static java.lang.String.format;


/**
 * Shape inspector.
 *
 * <p>Associates linked data resources managed by the wrapped handler with the {@linkplain Shape shape} model
 * constraining their representation and driving their lifecycle.</p>
 *
 * <p>The associated shape may be inspected as an RDF model retrievable from the location advertised through the {@code
 * Link rel="ldp:constrainedBY} header.</p>
 */
public final class Inspector implements Wrapper {

	private static final String SpecsQuery="specs";


	public static Inspector inspector() {
		return new Inspector();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shape shape;


	/**
	 * Configures the linked data shape for this inspector.
	 *
	 * @param shape the shape to be associated to the linked data resources managed by the wrapped handler
	 *
	 * @return this inspector
	 *
	 * @throws NullPointerException if {@code shape} is {@code null}
	 */
	public Inspector shape(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		this.shape=shape.accept(new Optimizer());

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return (request, response) -> {
			if ( request.method().equals(Request.GET) && request.query().equals(SpecsQuery) ) {

				// !!! check resource existence (HEAD request? What about virtual containers?)

				final IRI focus=request.focus();
				final IRI specs=iri(focus+"?"+SpecsQuery);

				final Collection<Statement> model=new ArrayList<>();

				model.add(statement(focus, LDP.CONSTRAINED_BY, specs));

				final Shape shape=this.shape
						.accept(role(request.roles()))
						.accept(mode(Spec.verify))
						.accept(new Inferencer())
						.accept(new Optimizer());

				final ShapeCodec codec=new ShapeCodec();

				// !!! extract/manage container creation shape
				// final Shape create=traits(shape)
				//		.getOrDefault(step(LDP.CONTAINS), and()) // extract resource creation shape
				//		.accept(Shape.task(Spec.create));


				for (final IRI task : Lists.list(Spec.relate, Spec.update, Spec.delete)) {

					final Shape spec=shape.accept(task(task));

					if ( !empty(spec) ) {
						model.add(statement(specs, task, codec.encode(spec, model)));
					}

				}

				response.status(Response.OK).rdf(model); /* !!! declare SpecsShape */

			} else {

				handler.handle(

						writer -> writer.copy(request)

								.done(),

						reader -> response.copy(reader)

								.header("Link", "", // !!! review header concatenation
										format("<%s?%s>; rel=\"%s\"", request.focus(), SpecsQuery, LDP.CONSTRAINED_BY))

								.done()

				);

			}
		};
	}

}
