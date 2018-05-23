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
import com.metreeca.link.handlers.Dispatcher;
import com.metreeca.spec.shapes.And;
import com.metreeca.spec.things.Values;
import com.metreeca.tray.Tool;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Setup;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.math.BigInteger;
import java.util.*;
import java.util.function.BiConsumer;

import static com.metreeca.spec.Shape.required;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Datatype.datatype;
import static com.metreeca.spec.shapes.Pattern.pattern;
import static com.metreeca.spec.shapes.Trait.trait;
import static com.metreeca.spec.things.Maps.entry;
import static com.metreeca.spec.things.Maps.map;
import static com.metreeca.spec.things.Values.*;


/**
 * System status and configuration.
 */
public final class Status implements _Service {

	private static final String Label="System Status";


	private static final OperatingSystemMXBean system=ManagementFactory.getOperatingSystemMXBean();
	private static final RuntimeMXBean runtime=ManagementFactory.getRuntimeMXBean();
	private static final Runtime memory=Runtime.getRuntime();


	private static final And DictionaryShape=and(required(), datatype(Values.BNodeType),
			trait(Link.Entry, and(datatype(Values.BNodeType),
					trait(Link.Key, and(required(), datatype(XMLSchema.STRING), pattern("\\w*"))),
					trait(Link.Value, and(required(), datatype(XMLSchema.STRING)))
			)));

	private static final And StatusShape=and(

			trait(RDFS.LABEL, and(required(), datatype(XMLSchema.STRING))),

			trait(Link.ServerVersion, and(required(), datatype(XMLSchema.STRING))),

			trait(Link.SystemArchitecture, and(required(), datatype(XMLSchema.STRING))),
			trait(Link.SystemName, and(required(), datatype(XMLSchema.STRING))),
			trait(Link.SystemVersion, and(required(), datatype(XMLSchema.STRING))),
			trait(Link.SystemProcessors, and(required(), datatype(XMLSchema.STRING))),

			trait(Link.RuntimeSpecName, and(required(), datatype(XMLSchema.STRING))),
			trait(Link.RuntimeSpecVendor, and(required(), datatype(XMLSchema.STRING))),
			trait(Link.RuntimeSpecVersion, and(required(), datatype(XMLSchema.STRING))),

			trait(Link.RuntimeVMName, and(required(), datatype(XMLSchema.STRING))),
			trait(Link.RuntimeVMVendor, and(required(), datatype(XMLSchema.STRING))),
			trait(Link.RuntimeVMVersion, and(required(), datatype(XMLSchema.STRING))),

			trait(Link.Backend, and(required(), datatype(XMLSchema.STRING))),

			trait(Link.RuntimeMemoryTotal, and(required(), datatype(XMLSchema.INTEGER))),
			trait(Link.RuntimeMemoryUsage, and(required(), datatype(XMLSchema.INTEGER))),

			trait(Link.Setup, DictionaryShape),
			trait(Link.Properties, DictionaryShape)
	);


	private Graph graph;


	@Override public void load(final Tool.Loader tools) {

		final Setup setup=tools.get(Setup.Tool);

		this.graph=tools.get(Graph.Tool);

		tools.get(Index.Tool).insert("/!/", new Dispatcher(map(

				entry(_Request.GET, _Handler.sysadm(this::get))

		)), map(

				entry(RDFS.LABEL, literal(Label))

		));
	}

	private void get(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) { // !!! refactor

		response.setStatus(_Response.OK);

		final IRI target=iri(request.getTarget());

		final Collection<Statement> model=new ArrayList<>();

		model.add(statement(target, RDFS.LABEL, literal(Label)));

		model.add(statement(target, Link.ServerVersion, literal(Link.Token)));

		model.add(statement(target, Link.SystemArchitecture, literal(system.getArch())));
		model.add(statement(target, Link.SystemName, literal(system.getName())));
		model.add(statement(target, Link.SystemVersion, literal(system.getVersion())));
		model.add(statement(target, Link.SystemProcessors, literal(system.getAvailableProcessors())));

		model.add(statement(target, Link.RuntimeSpecName, literal(runtime.getSpecName())));
		model.add(statement(target, Link.RuntimeSpecVendor, literal(runtime.getSpecVendor())));
		model.add(statement(target, Link.RuntimeSpecVersion, literal(runtime.getSpecVersion())));

		model.add(statement(target, Link.RuntimeVMName, literal(runtime.getVmName())));
		model.add(statement(target, Link.RuntimeVMVendor, literal(runtime.getVmVendor())));
		model.add(statement(target, Link.RuntimeVMVersion, literal(runtime.getVmVersion())));

		model.add(statement(target, Link.Backend, literal(request.map(graph).info())));

		model.add(statement(target, Link.RuntimeMemoryTotal,
				literal(BigInteger.valueOf(memory.maxMemory()))));

		model.add(statement(target, Link.RuntimeMemoryUsage,
				literal(BigInteger.valueOf(memory.totalMemory()-memory.freeMemory()))));


		final BNode setupTerm=bnode(); // server setup

		model.add(statement(target, Link.Setup, setupTerm));

		final Setup setup=tools.get(Setup.Tool);

		for (final Map.Entry<String, String> property : setup.properties().entrySet()) {

			final String label=property.getKey();
			final String value=property.getValue();

			final BNode entry=bnode();

			model.add(statement(setupTerm, Link.Entry, entry));
			model.add(statement(entry, Link.Key, literal(label)));
			model.add(statement(entry, Link.Value, literal(value)));
		}


		final BNode propertiesTerm=bnode(); // system properties

		model.add(statement(target, Link.Properties, propertiesTerm));

		final Properties properties=System.getProperties();

		for (final String property : properties.stringPropertyNames()) {

			final BNode entry=bnode();

			model.add(statement(propertiesTerm, Link.Entry, entry));
			model.add(statement(entry, Link.Key, literal(property)));
			model.add(statement(entry, Link.Value, literal(properties.getProperty(property))));
		}

		new _Transfer(request, response).model(model, StatusShape);

		sink.accept(request, response);
	}

}
