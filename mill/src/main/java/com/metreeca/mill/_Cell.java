/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.mill;

import com.metreeca.spec.things.Values;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

import java.io.*;
import java.util.*;

import static com.metreeca.spec.things.Values.format;
import static com.metreeca.spec.things.Values.statement;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableCollection;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;


public final class _Cell { // !!! migrate/merge to jeep

	public static _Cell cell(final Resource focus) {
		return new _Cell(focus, emptySet());
	}

	public static _Cell cell(final Resource focus, final Statement... model) {
		return new _Cell(focus, asList(model));
	}

	public static _Cell cell(final Resource focus, final Collection<Statement> model) {
		return new _Cell(focus, model);
	}


	// !!! factor to Cache (returning memoized dependencies among metadata)

	public static Collection<_Cell> decode(final Reader reader) throws IOException {

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		final Map<IRI, Collection<Statement>> items=new LinkedHashMap<>();

		Rio.createParser(RDFFormat.TRIG).setRDFHandler(new AbstractRDFHandler() {

			@Override public void handleStatement(final Statement statement) {

				if ( statement.getContext() == null ) { // decode focus for possibly empty model

					if ( statement.getPredicate().equals(RDF.TYPE) && statement.getObject().equals(RDFS.RESOURCE) ) {
						items.computeIfAbsent((IRI)statement.getSubject(), iri -> new ArrayList<>());
					}

				} else {

					items.compute((IRI)statement.getContext(), (focus, statements) -> {

						final Collection<Statement> merged=statements != null ? statements : new ArrayList<>();

						merged.add(statement);

						return merged;

					});

				}
			}

		}).parse(reader, Values.User);

		return items.entrySet().stream()
				.map(entry -> cell(entry.getKey(), entry.getValue()))
				.collect(toList());
	}

	public static Reader encode(final Iterable<_Cell> items) { // !!! stream writing

		if ( items == null ) {
			throw new NullPointerException("null items");
		}

		final StringWriter buffer=new StringWriter();

		final RDFWriter writer=Rio.createWriter(RDFFormat.TRIG, buffer);

		writer.startRDF();

		for (final _Cell cell : items) {

			final Resource focus=cell.focus();
			final Collection<Statement> model=cell.model();

			writer.handleStatement(statement(focus, RDF.TYPE, RDFS.RESOURCE)); // encode focus for possibly empty model

			for (final Statement statement : model) {
				writer.handleStatement(statement(
						statement.getSubject(), statement.getPredicate(), statement.getObject(), focus));
			}

		}

		writer.endRDF();

		return new StringReader(buffer.toString());
	}


	private final Resource focus;
	private final Collection<Statement> model;


	private _Cell(final Resource focus, final Collection<Statement> model) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		if ( model.contains(null) ) {
			throw new NullPointerException("null model statement");
		}

		this.focus=focus;
		this.model=model;
	}


	public Resource focus() {
		return focus;
	}

	public Collection<Statement> model() {
		return unmodifiableCollection(model);
	}


	@Override public String toString() {
		return format(focus)+" {"+model
				.stream()
				.filter(edge -> focus.equals(edge.getSubject()) || focus.equals(edge.getObject()))
				.map(Values::format)
				.collect(joining("\n\t", "\n\t", "\n"))+"}";
	}

}
