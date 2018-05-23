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
import com.metreeca.link.handlers.Container;
import com.metreeca.link.handlers.Resource;
import com.metreeca.link.handlers.Updater;
import com.metreeca.spec.Shape;
import com.metreeca.spec.Spec;
import com.metreeca.spec.codecs.ShapeCodec;
import com.metreeca.spec.things.Values;
import com.metreeca.spec.things._Cell;
import com.metreeca.tray.Tool;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.metreeca.spec.Shape.*;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Trait.trait;
import static com.metreeca.spec.things.Maps.entry;
import static com.metreeca.spec.things.Maps.map;
import static com.metreeca.spec.things.Values.iri;
import static com.metreeca.spec.things.Values.literal;
import static com.metreeca.spec.things.Values.statement;
import static com.metreeca.spec.things._Cell.Missing;
import static com.metreeca.spec.things._Cell.cell;
import static com.metreeca.tray.Tool.Loader;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toMap;


/**
 * Legacy index with soft-port support // !!! don't remove
 */
public final class Index {

	public static final String ResourcesSuffix=" / Items"; // !!! remove/migrate


	public static final Tool<Index> Tool=tools -> {

		final Index index=new Index(tools);

		tools.get(_Server.Tool).hook(index::load);

		return index;
	};


	private static boolean matches(final String x, final String y) {
		return x.equals(y) || y.endsWith("/") && x.startsWith(y);
	}

	private static String normalize(final String path) {
		return path.endsWith("?") || path.endsWith("/") || path.endsWith("/*") ? path.substring(0, path.length()-1) : path;
	}


	private final Loader tools;
	private final _Server server;
	private final Graph graph;

	private final Map<String, _Handler> handlers=new TreeMap<>(Comparator
			.comparingInt(String::length).reversed() // longest paths first
			.thenComparing(String::compareTo)); // then alphabetically


	private final Map<String, Map<IRI, Value>> properties=new HashMap<>();

	private final BiConsumer<String, String> sync=this::sync; // unique identity to prevent repeated scheduling


	public Map<String, Map<IRI, Value>> entries() {
		return unmodifiableMap(properties);
	}


	public Index(final Loader tools) {
		this.tools=tools;
		this.server=tools.get(_Server.Tool);
		this.graph=tools.get(Graph.Tool);
	}


	public _Handler get(final String path) { // !!! optimize?

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		synchronized ( handlers ) {

			final String normalized=normalize(path);

			return handlers.entrySet().stream()
					.filter(entry -> matches(normalized, entry.getKey()))
					.map(Map.Entry::getValue)
					.findFirst()
					.orElse(null);
		}
	}


	/**
	 * Inserts a soft port.
	 */
	public Index insert(final _Cell specs) {

		if ( specs == null ) {
			throw new NullPointerException("null specs");
		}

		return exec(index -> {

			final String port=specs.value().orElseThrow(Missing).stringValue();
			final String uuid=port.substring(port.lastIndexOf('/')+1);

			final Value label=specs.forward(RDFS.LABEL).value().orElseThrow(Missing);
			final Value notes=specs.forward(RDFS.COMMENT).value().orElse(null);

			final Value root=specs.forward(Link.root).value().orElseThrow(Missing);

			final String path=specs.forward(Link.path).string().orElseThrow(Missing);
			final String spec=specs.forward(Link.spec).string().orElseThrow(Missing);

			final Optional<String> create=specs.forward(Link.create).string();
			final Optional<String> update=specs.forward(Link.update).string();
			final Optional<String> delete=specs.forward(Link.delete).string();
			final Optional<String> mutate=specs.forward(Link.mutate).string();

			final Shape shape=new ShapeCodec().decode(spec); // !!! test ldp:contains presence and split if found

			final Shape digest=shape.accept(view(Spec.digest));
			final Shape detail=shape.accept(view(Spec.detail));

			final boolean collection=path.endsWith("/");
			final boolean container=path.endsWith("?");

			if ( collection || container ) {

				final Container handler=new Container(tools, and(
						trait(RDFS.LABEL, verify(required(), only(label))),
						trait(RDFS.COMMENT, verify(optional(), notes != null ? only(notes) : and())),
						trait(LDP.CONTAINS, digest)
				));

				final Map<String, String> hooks=new HashMap<>();

				create.ifPresent(hook -> hooks.put(_Request.POST, hook));
				mutate.ifPresent(hook -> hooks.put(_Request.ANY, hook));

				insert(path, hooks.isEmpty() ? handler : new Updater(tools, handler, hooks), map(
						entry(RDFS.LABEL, label),
						entry(RDFS.COMMENT, notes),
						entry(Link.uuid, literal(uuid)),
						entry(Link.soft, Values.True),
						entry(Link.root, root)
				));

			}

			if ( collection ) { // ancillary resource port

				final _Handler handler=new Resource(tools, detail);

				final Map<String, String> hooks=new HashMap<>();

				update.ifPresent(hook -> hooks.put(_Request.PUT, hook));
				delete.ifPresent(hook -> hooks.put(_Request.DELETE, hook));
				mutate.ifPresent(hook -> hooks.put(_Request.ANY, hook));

				insert(path+"*", hooks.isEmpty() ? handler : new Updater(tools, handler, hooks), map(
						entry(RDFS.LABEL, Values.literal(label.stringValue()+ResourcesSuffix)),
						entry(RDFS.ISDEFINEDBY, specs.value().orElseThrow(Missing)),
						entry(Link.soft, Values.False),
						entry(Link.root, Values.False)
				));

			} else if ( !container ) { // real resource port

				final _Handler handler=new Resource(tools, detail);

				final Map<String, String> hooks=new HashMap<>();

				create.ifPresent(hook -> hooks.put(_Request.POST, hook));
				update.ifPresent(hook -> hooks.put(_Request.PUT, hook));
				delete.ifPresent(hook -> hooks.put(_Request.DELETE, hook));
				mutate.ifPresent(hook -> hooks.put(_Request.ANY, hook));

				insert(path, hooks.isEmpty() ? handler : new Updater(tools, handler, hooks), map(
						entry(RDFS.LABEL, label),
						entry(RDFS.COMMENT, notes),
						entry(Link.uuid, literal(uuid)),
						entry(Link.soft, Values.True),
						entry(Link.root, root)
				));

			}

			return index;

		});
	}

	/**
	 * Removes a soft port.
	 */
	public Index remove(final _Cell specs) {

		if ( specs == null ) {
			throw new NullPointerException("null specs");
		}

		return exec(index -> {

			final String path=specs.forward(Link.path).string().orElseThrow(Missing);

			final boolean collection=path.endsWith("/");

			remove(path);

			if ( collection ) { remove(path+"*"); }

			return index;

		});
	}


	public Index insert(final String path, final _Handler handler) {
		return insert(path, handler, emptyMap());
	}

	public Index insert(final String path, final _Handler handler, final Map<IRI, Value> properties) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( path.isEmpty() ) { // !!! test pattern
			throw new IllegalArgumentException("illegal path ["+path+"]");
		}

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		if ( properties == null ) {
			throw new NullPointerException("null properties");
		}

		if ( properties.containsKey(null) ) {
			throw new NullPointerException("null property key");
		}

		return exec(index -> {

			if ( index.properties.containsKey(path) ) {
				throw new IllegalStateException("path is already mapped {"+path+"}");
			}

			index.handlers.put(normalize(path), handler);

			index.properties.put(path, unmodifiableMap(properties.entrySet().stream()
					.filter(entry -> entry.getValue() != null) // ignore null property values
					.collect(toMap(Map.Entry::getKey, Map.Entry::getValue))));

			index.server.hook(index.sync);

			return index;

		});
	}

	public Index remove(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return exec(index -> {

			if ( !index.properties.containsKey(path) ) {
				throw new IllegalStateException("path is not mapped {"+path+"}");
			}

			index.handlers.remove(normalize(path));
			index.properties.remove(path);

			index.server.hook(index.sync);

			return index;

		});
	}


	/**
	 * Executes a task within an isolated transaction.
	 *
	 * @param task the task to be executed
	 * @param <R>  the type of the value returned by {@code task}
	 *
	 * @return the value returned by {@code task}
	 */
	public <R> R exec(final Function<Index, R> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		synchronized ( handlers ) { return task.apply(this); }
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Index load(final String alternate, final String canonical) { // load soft ports
		return exec(_index -> graph.map(alternate, canonical).browse(connection -> { // !!! notify observers only at end of index txn

			final StatementCollector collector=new StatementCollector(); // retrieve soft port specs from repository

			connection.prepareGraphQuery(

					"prefix link: <tag:com.metreeca,2016:link/terms#>\n"
							+"\n"
							+"construct where { ?port a link:Port; link:soft true; ?p ?o }"

			).evaluate(collector);

			cell(collector.getStatements()) // create soft ports
					.insert(Link.Port)
					.reverse(RDF.TYPE)
					.cells()
					.forEach(this::insert);

			return this;

		}));
	}

	private Index sync(final String alternate, final String canonical) { // update cached port properties
		return exec(_index -> graph.map(alternate, canonical).update(connection -> {

			connection.prepareUpdate( // purge cache

					"prefix link: <tag:com.metreeca,2016:link/terms#>\n"
							+"\n"
							+"delete where { ?s a link:Port; link:soft false; ?p ?o }"

			).execute();

			final Collection<Statement> model=new LinkedHashModel(); // update cache

			for (final Map.Entry<String, Map<IRI, Value>> entry : entries().entrySet()) {

				final String path=entry.getKey();
				final Map<IRI, Value> properties=entry.getValue();

				if ( !path.startsWith("/!/") ) { // ignore meta ports

					final Value uuid=properties.getOrDefault(Link.uuid, Values.uuid(path));
					final Value root=properties.getOrDefault(Link.root, Values.False);
					final Value soft=properties.getOrDefault(Link.soft, Values.False);

					final IRI port=iri(canonical+"!/ports/", uuid.stringValue());

					for (final Map.Entry<IRI, Value> meta : properties.entrySet()) { // before system-managed values
						model.add(statement(port, meta.getKey(), meta.getValue()));
					}

					model.add(statement(port, RDF.TYPE, Link.Port));

					model.add(statement(port, Link.path, literal(path)));
					model.add(statement(port, Link.uuid, uuid));
					model.add(statement(port, Link.root, root));
					model.add(statement(port, Link.soft, soft));

				}
			}

			connection.add(model);

			return this;

		}));
	}

}
