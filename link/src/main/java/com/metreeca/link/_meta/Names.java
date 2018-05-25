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
import com.metreeca.link.handlers._Dispatcher;
import com.metreeca.spec.Shape;
import com.metreeca.spec.Spec;
import com.metreeca.spec.probes.Outliner;
import com.metreeca.spec.things.Values;
import com.metreeca.spec.things._Cell;
import com.metreeca.tray.Tool;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Setup;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.repository.RepositoryResult;

import java.util.*;
import java.util.function.BiConsumer;

import static com.metreeca.link._Handler.sysadm;
import static com.metreeca.link._Handler.unauthorized;
import static com.metreeca.link._Handler.unsupported;
import static com.metreeca.spec.Shape.empty;
import static com.metreeca.spec.Shape.required;
import static com.metreeca.spec.shapes.All.all;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Datatype.datatype;
import static com.metreeca.spec.shapes.Pattern.pattern;
import static com.metreeca.spec.shapes.Trait.trait;
import static com.metreeca.spec.things.Maps.entry;
import static com.metreeca.spec.things.Maps.map;
import static com.metreeca.spec.things.Values.*;

import static java.util.stream.Collectors.toMap;


/**
 * RDF namespace catalog.
 */
public final class Names implements _Service {

	private static final Shape NamesShape=and(trait(Link.Entry, and(datatype(Values.BNodeType),
			trait(Link.Key, and(required(), datatype(XMLSchema.STRING), pattern("\\w*"))),
			trait(Link.Value, and(required(), datatype(Values.IRIType)))
	)));


	private Graph graph;


	@Override public void load(final Tool.Loader tools) {

		final Setup setup=tools.get(Setup.Tool);

		this.graph=tools.get(Graph.Tool);

		tools.get(Index.Tool).insert("/!/names", new _Dispatcher(map(

				entry(_Request.GET, sysadm(this::get)), entry(_Request.PUT, sysadm(this::put))

		)), map(

				entry(RDFS.LABEL, literal("RDF Namespace Catalog"))

		));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void get(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

		request.map(graph).browse(connection -> {

			final IRI target=iri(request.getTarget());

			final Collection<Statement> model=new ArrayList<>();

			try (final RepositoryResult<Namespace> namespaces=connection.getNamespaces()) {
				while ( namespaces.hasNext() ) {

					final Namespace namespace=namespaces.next();

					final BNode entry=bnode();

					model.add(statement(target, Link.Entry, entry));
					model.add(statement(entry, Link.Key, literal(namespace.getPrefix())));
					model.add(statement(entry, Link.Value, iri(namespace.getName())));

				}
			}

			response.setStatus(_Response.OK);

			new _Transfer(request, response).model(model, NamesShape);

			sink.accept(request, response);

			return null;

		});

	}

	private void put(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

		final Shape updating=NamesShape.accept(Shape.task(Spec.update));
		final Shape authorized=updating.accept(Shape.role(request.getRoles()));

		if ( empty(updating) ) {

			unsupported(tools, request, response, sink);

		} else if ( empty(authorized) ) {

			unauthorized(tools, request, response, sink);

		} else {

			final IRI target=iri(request.getTarget());
			final Shape shape=and(all(target), authorized);

			final Collection<Statement> model=new ArrayList<>();

			model.addAll(new _Transfer(request, response).model(shape)); // add user-submitted statements
			model.addAll(shape.accept(Shape.mode(Spec.verify)).accept(new Outliner())); // add implied statements

			final Map<String, String> namespaces=new TreeMap<>(_Cell.cell(model, target)

					.forward(Link.Entry)
					.cells()
					.stream()

					.collect(toMap(
							item -> item.forward(Link.Key).value().map(Value::stringValue).orElse(""),
							item -> item.forward(Link.Value).value().map(Value::stringValue).orElse("")
					)));

			request.map(graph).update(connection -> {

				connection.clearNamespaces();

				for (final Map.Entry<String, String> namespace : namespaces.entrySet()) {
					connection.setNamespace(namespace.getKey(), namespace.getValue());
				}

				return null;

			});

		}

	}

}
