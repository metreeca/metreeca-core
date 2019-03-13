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

import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.probes.Inferencer;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Redactor;
import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;

import static com.metreeca.form.codecs.BaseCodec.aliases;
import static com.metreeca.form.codecs.JSON.encode;
import static com.metreeca.form.codecs.JSON.field;
import static com.metreeca.form.codecs.JSON.object;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.Field.fields;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.Memoizing.memoizable;
import static com.metreeca.form.things.Codecs.writer;
import static com.metreeca.form.things.Values.direct;
import static com.metreeca.form.things.Values.inverse;

import static java.util.stream.Collectors.toList;


public final class JSONWriter extends AbstractRDFWriter {

	private static final Function<Shape, Shape> ShapeCompiler=memoizable(s -> s
			.map(new Redactor(Form.mode, Form.convey)) // remove internal filtering shapes
			.map(new Optimizer())
			.map(new Inferencer()) // infer implicit constraints to drive json shorthands
			.map(new Optimizer())
	);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String base;
	private final Writer writer;

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

		this.base=root(base);
		this.writer=writer;
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

			final Shape driver=(shape == null) ? null : shape.map(ShapeCompiler);

			final Predicate<Resource> trail=resource -> false;

			final Object json=(focus != null) ? json(focus, driver, trail) : json(model.subjects(), driver, trail);

			try { writer.write(encode(json)); } finally { writer.flush(); }

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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Object json(final Collection<? extends Value> values, final Shape shape, final Predicate<Resource> trail) {
		return shape == null || maxCount(shape).map(limit -> limit > 1).orElse(true)
				? values.stream().map(value -> json(value, shape, trail)).collect(toList())
				: values.isEmpty() ? null : json(values.iterator().next(), shape, trail);
	}


	private Object json(final Value value, final Shape shape, final Predicate<Resource> trail) {
		return value instanceof Resource ? json((Resource)value, shape, trail)
				: value instanceof Literal ? json((Literal)value, shape)
				: null;
	}

	private Object json(final Resource resource, final Shape shape, final Predicate<Resource> trail) { // !!! refactor

		final String id=resource.stringValue();
		final Optional<IRI> datatype=datatype(shape);
		final Map<IRI, Shape> fields=fields(shape);

		if ( datatype.filter(iri -> iri.equals(Form.IRIType)).isPresent() && fields.isEmpty() ) {

			return relativize(id); // inline proved leaf IRI

		} else {

			final Map<Object, Object> object=new LinkedHashMap<>();

			object.put("this", resource instanceof BNode ? "_:"+id : relativize(id));

			if ( !trail.test(resource) ) { // not a back-reference to an enclosing copy of self -> include fields

				final Collection<Resource> references=new ArrayList<>();

				final Predicate<Resource> nestedTrail=reference -> {

					if ( reference.equals(resource) ) {
						references.add(reference); // mark resource as back-referenced
					}

					return reference.equals(resource) || trail.test(reference);

				};

				if ( shape == null ) { // write all direct fields

					for (final IRI predicate : model.filter(resource, null, null).predicates()) {
						object.put(predicate.stringValue(),
								json(model.filter(resource, predicate, null).objects(), null, nestedTrail));
					}

				} else { // write direct/inverse fields as specified by the shape

					final Map<IRI, String> aliases=aliases(shape, JSONCodec.Reserved);

					for (final Map.Entry<IRI, Shape> entry : fields.entrySet()) {

						final IRI predicate=entry.getKey();
						final boolean direct=direct(predicate);

						final Shape nestedShape=entry.getValue();

						final String alias=Optional.ofNullable(aliases.get(entry.getKey()))
								.orElseGet(() -> (direct ? "" : "^")+predicate.stringValue());

						final Collection<? extends Value> values=direct
								? model.filter(resource, predicate, null).objects()
								: model.filter(null, inverse(predicate), resource).subjects();

						if ( !values.isEmpty() ) { // omit null value and empty arrays

							object.put(alias, json(values, nestedShape, nestedTrail));

						}

					}

				}

				datatype // drop id field if proved to be a blank node without back-references
						.filter(type -> type.equals(Form.BNodeType) && references.isEmpty())
						.ifPresent(type -> object.remove("this"));

			}

			return object;
		}
	}

	private Object json(final Literal literal, final Shape shape) {

		final IRI datatype=literal.getDatatype();

		try {

			return datatype.equals(XMLSchema.BOOLEAN) ? json(literal.booleanValue())
					: datatype.equals(XMLSchema.STRING) ? json(literal.stringValue())
					: datatype.equals(XMLSchema.INTEGER) ? json(literal.integerValue())
					: datatype.equals(XMLSchema.DECIMAL) ? json(literal.decimalValue())
					: datatype.equals(XMLSchema.DOUBLE) ? json(literal.doubleValue())
					: datatype.equals(RDF.LANGSTRING) ? json(literal, literal.getLanguage().orElse(""))
					: datatype(shape).isPresent() ? literal.stringValue() // only lexical value if type is known
					: json(literal, datatype);

		} catch ( final IllegalArgumentException ignored ) { // malformed literals
			return json(literal, datatype);
		}
	}


	private Object json(final boolean value) {
		return value;
	}

	private Object json(final String value) {
		return value;
	}

	private Object json(final BigInteger value) {
		return value;
	}

	private Object json(final BigDecimal value) {
		return value;
	}

	private Object json(final double value) {
		return value;
	}


	private Object json(final Literal literal, final String lang) {
		return object(field("text", literal.stringValue()), field("lang", lang));
	}

	private Object json(final Literal literal, final IRI datatype) {
		return object(field("text", literal.stringValue()), field("type", datatype.stringValue()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String root(final CharSequence base) {
		if ( base == null ) { return null; } else {

			final Matcher matcher=Values.IRIPattern.matcher(base);

			return matcher.matches() ? matcher.group("schemeall")+matcher.group("hostall")+"/" : null;

		}
	}

	private String relativize(final String iri) {
		return base != null && iri.startsWith(base) ? iri.substring(base.length()-1) : iri;
	}

}
