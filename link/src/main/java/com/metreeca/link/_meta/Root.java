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

package com.metreeca.link._meta;

import com.metreeca.link.*;
import com.metreeca.link._junk._Request;
import com.metreeca.link._junk._Response;
import com.metreeca.link._junk._Transfer;
import com.metreeca.link.handlers._Dispatcher;
import com.metreeca.spec.Shape;
import com.metreeca.spec.things.Values;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.*;

import java.util.*;
import java.util.function.BiConsumer;

import static com.metreeca.spec.Shape.optional;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Datatype.datatype;
import static com.metreeca.spec.shapes.Trait.trait;
import static com.metreeca.spec.things.Maps.entry;
import static com.metreeca.spec.things.Maps.map;
import static com.metreeca.spec.things.Values.iri;
import static com.metreeca.spec.things.Values.literal;
import static com.metreeca.spec.things.Values.statement;

import static java.util.Arrays.asList;


/**
 * System metadata.
 */
public final class Root implements _Service { // !!! migrate to Builder

	private static final String Label="System";

	private static final Shape shape=and(
			trait(VOID.ROOT_RESOURCE, and(
					trait(RDFS.LABEL, and(optional(), datatype(XMLSchema.STRING))),
					trait(RDFS.COMMENT, and(optional(), datatype(XMLSchema.STRING))))
			)
	);

	private static final List<IRI> RootProperties=asList(RDFS.LABEL, RDFS.COMMENT); // !! derive from shape


	private Index index;


	@Override public void load(final Tool.Loader tools) { // !!! in constructor

		index=tools.get(Index.Tool).insert("/", new _Dispatcher(map(entry(_Request.GET, this::get)

		)), map(

				entry(RDFS.LABEL, literal(Label))

		));
	}


	private void get(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

		response.setStatus(_Response.OK);

		final IRI target=iri(request.getTarget());

		final Collection<Statement> model=new ArrayList<>();

		model.add(statement(target, RDF.TYPE, VOID.DATASET));

		index.entries().entrySet().stream() // list root resources
				.filter(entry -> Values.True.equals(entry.getValue().get(Link.root)))
				.forEachOrdered(entry -> {

					final String path=entry.getKey();
					final Map<IRI, Value> properties=entry.getValue();

					final IRI root=iri(request.getBase()+path.substring(1));

					model.add(statement(target, VOID.ROOT_RESOURCE, root));

					for (final IRI property : RootProperties) {

						final Value value=properties.get(property);

						if ( value != null ) {
							model.add(statement(root, property, value));
						}
					}

				});

		new _Transfer(request, response).model(model, shape);

		sink.accept(request, response);
	}

}
