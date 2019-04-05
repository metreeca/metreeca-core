/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.form.codecs;

import com.metreeca.form.Shape;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.JsonWriterFactory;

import static com.metreeca.form.things.Codecs.writer;

import static java.util.Collections.singletonMap;

import static javax.json.stream.JsonGenerator.PRETTY_PRINTING;


public final class JSONWriter extends AbstractRDFWriter {

	private static final JsonWriterFactory writers=Json.createWriterFactory(singletonMap(PRETTY_PRINTING, true));


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Writer writer;
	private final String base;

	private final Model model=new LinkedHashModel();


	public JSONWriter(final OutputStream stream) {
		this(stream, null);
	}

	public JSONWriter(final OutputStream stream, final String base) {
		this(writer(stream), base);
	}


	public JSONWriter(final Writer writer) {
		this(writer, null);
	}

	public JSONWriter(final Writer writer, final String base) {

		if ( writer == null ) {
			throw new NullPointerException("null writer");
		}

		this.writer=writer;
		this.base=base;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public RDFFormat getRDFFormat() {
		return JSONCodec.JSONFormat;
	}

	@Override public Collection<RioSetting<?>> getSupportedSettings() {

		final Collection<RioSetting<?>> settings=super.getSupportedSettings();

		settings.add(JSONCodec.Focus);
		settings.add(JSONCodec.Shape);

		return settings;
	}


	@Override public void startRDF() {}

	@Override public void endRDF() throws RDFHandlerException {
		try {

			final Resource focus=getWriterConfig().get(JSONCodec.Focus);
			final Shape shape=getWriterConfig().get(JSONCodec.Shape);

			final JsonValue json=new JSONEncoder(base) {}.json(model, shape, focus);

			try {

				writers.createWriter(writer).write(json);

			} finally {

				writer.flush();

			}

		} catch ( final IOException e ) {

			throw new RDFHandlerException("IO exception while writing RDF", e);

		} finally {
			model.clear();

		}
	}

	@Override public void handleNamespace(final String prefix, final String uri) { }

	@Override public void handleComment(final String comment) { }

	@Override public void handleStatement(final Statement statement) {
		model.add(statement.getSubject(), statement.getPredicate(), statement.getObject()); // ignore context
	}

}
