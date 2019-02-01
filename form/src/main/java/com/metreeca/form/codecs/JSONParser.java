/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.form.codecs;


import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.probes.Inferencer;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Redactor;
import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.json.JsonException;
import javax.json.stream.JsonParsingException;

import static com.metreeca.form.codecs.BaseCodec.aliases;
import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.Field.fields;
import static com.metreeca.form.things.Values.direct;
import static com.metreeca.form.things.Values.inverse;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;


public final class JSONParser extends AbstractRDFParser {

	private static final Pattern EdgePattern=Pattern
			.compile("(?<alias>\\w+)|(?<inverse>\\^)?((?<naked>\\w+:.*)|<(?<bracketed>\\w+:.*)>)");


	private final DecimalFormat DoubleFormat=new DecimalFormat("0.0##E0", DecimalFormatSymbols.getInstance(Locale.ROOT));


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


	@Override public void parse(final InputStream in, final String baseURI)
			throws IOException, RDFParseException, RDFHandlerException {

		if ( in == null ) {
			throw new NullPointerException("null input stream");
		}

		if ( baseURI == null ) {
			throw new NullPointerException("null base URI");
		}

		parse(new InputStreamReader(in, "UTF-8"), baseURI);
	}

	@Override public void parse(final Reader reader, final String baseURI)
			throws RDFParseException, RDFHandlerException {

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		if ( baseURI == null ) {
			throw new NullPointerException("null base URI");
		}

		final Resource focus=getParserConfig().get(JSONCodec.Focus);
		final Shape shape=getParserConfig().get(JSONCodec.Shape);

		final Shape driver=(shape == null) ? null : shape

				.map(new Redactor(Form.mode, Form.verify)) // remove internal filtering shapes
				.map(new Optimizer())
				.map(new Inferencer()) // infer implicit constraints to drive json shorthands
				.map(new Optimizer());

		if ( rdfHandler != null ) {
			rdfHandler.startRDF();
		}

		try {

			parse(JSON.decode(reader), baseURI, focus, driver).count(); // consume values

		} catch ( final JsonParsingException e ) {

			throw new RDFParseException(e.getMessage(),
					e.getLocation().getLineNumber(), e.getLocation().getColumnNumber());

		} catch ( final JsonException e ) {

			throw new RDFParseException(e.getMessage(), e);

		}

		if ( rdfHandler != null ) {
			rdfHandler.endRDF();
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Stream<Value> parse(final Object json, final String base, final Resource focus, final Shape shape) {
		return json == null ? Stream.empty()

				: json instanceof Collection<?> ? parse((Collection<?>)json, base, focus, shape)
				: json instanceof Map<?, ?> ? parse((Map<?, ?>)json, base, focus, shape)

				: focus != null ? Stream.empty()

				: parse(json, base, shape);
	}


	private Stream<Value> parse(final Collection<?> array, final String base, final Resource focus, final Shape shape) {
		return array.stream().flatMap(object -> parse(object, base, focus, shape));
	}

	private Stream<Value> parse(final Map<?, ?> object, final String base, final Resource focus, final Shape shape) { // !!! refactor

		final Resource source=resource(object, base) // non-null id
				.orElseGet(() -> blank(object) // null id
						.orElseGet(() -> resource(shape) // known resource
								.orElseGet(() -> blank(shape) // proved bnode
										.orElse(focus)))); // implicit source

		if ( focus != null && !focus.equals(source) ) {

			return Stream.empty(); // !!! or error()?

		} else if ( source != null ) { // resource

			for (final Map.Entry<?, ?> entry : object.entrySet()) {

				final String label=entry.getKey().toString();
				final Object value=entry.getValue();

				if ( !label.equals("this") ) {

					final IRI property=property(label, base, shape);
					final Stream<Value> targets=parse(value, base, null, fields(shape).get(property));

					if ( rdfHandler != null ) {
						targets.forEachOrdered(target -> {

							if ( direct(property) ) {

								rdfHandler.handleStatement(createStatement(source, property, target));

							} else if ( target instanceof Resource ) {

								rdfHandler.handleStatement(createStatement((Resource)target, inverse(property), source));

							} else {

								error(format("target for inverse property is not a resource [%s: %s]", label, target));
							}

						});
					}

				}
			}

			return Stream.of(source);

		} else if ( !object.isEmpty() ) { // literal

			final Object text=object.get("text");
			final Object type=object.get("type");
			final Object lang=object.get("lang");

			if ( text instanceof String ) {

				return type instanceof String ? Stream.of(createLiteral((String)text, null, createIRI(base, (String)type)))
						: lang instanceof String ? Stream.of(createLiteral((String)text, (String)lang, null))
						: error("literal type/lang fields are missing or not strings");

			} else {

				return error("literal text field is missing or not a string");

			}

		} else {
			return Stream.empty();
		}

	}


	private Stream<Value> parse(final Object json, final String base, final Shape shape) {

		final IRI type=datatype(shape).orElse(null);

		return json instanceof Boolean ? parse((Boolean)json, type)

				: json instanceof BigDecimal ? parse((BigDecimal)json, type)
				: json instanceof BigInteger ? parse((BigInteger)json, type)
				: json instanceof Long ? parse((Long)json, type)
				: json instanceof Integer ? parse((Integer)json, type)
				: json instanceof Short ? parse((Short)json, type)
				: json instanceof Byte ? parse((Byte)json, type)

				: json instanceof Double ? parse((Double)json, type)
				: json instanceof Float ? parse((Float)json, type)

				: parse(json.toString(), base, type);
	}

	// !!! complete type conversions (JSON.decode will emit only BigDecimal/BigInteger/Double)

	private Stream<Value> parse(final Boolean value, final IRI type) {
		return Stream.of(createLiteral(value.toString(), null, XMLSchema.BOOLEAN));
	}

	private Stream<Value> parse(final BigDecimal value, final IRI type) {
		return type == null || type.equals(XMLSchema.DECIMAL)
				? Stream.of(createLiteral(value.toPlainString(), null, XMLSchema.DECIMAL))
				: XMLSchema.DOUBLE.equals(type) ? parse(value.doubleValue(), null)
				: error("unable to promote big decimal value to "+Values.format(type));
	}

	private Stream<Value> parse(final BigInteger value, final IRI type) {
		return type == null || type.equals(XMLSchema.INTEGER)
				? Stream.of(createLiteral(value.toString(), null, XMLSchema.INTEGER))
				: XMLSchema.DECIMAL.equals(type) ? parse(new BigDecimal(value), null)
				: XMLSchema.DOUBLE.equals(type) ? parse(value.doubleValue(), null)
				: error("unable to promote big integer value to "+Values.format(type));
	}

	private Stream<Value> parse(final Long value, final IRI type) {
		return Stream.of(createLiteral(value.toString(), null, XMLSchema.LONG));
	}

	private Stream<Value> parse(final Integer value, final IRI type) {
		return Stream.of(createLiteral(value.toString(), null, XMLSchema.INT));
	}

	private Stream<Value> parse(final Short value, final IRI type) {
		return Stream.of(createLiteral(value.toString(), null, XMLSchema.SHORT));
	}

	private Stream<Value> parse(final Byte value, final IRI type) {
		return Stream.of(createLiteral(value.toString(), null, XMLSchema.BYTE));
	}

	private Stream<Value> parse(final Double value, final IRI type) {
		return type == null || type.equals(XMLSchema.DOUBLE)
				? Stream.of(createLiteral(DoubleFormat.format(value), null, XMLSchema.DOUBLE))
				: XMLSchema.DECIMAL.equals(type) ? parse(new BigDecimal(value), null)
				: error("unable to promote double value to "+Values.format(type));
	}

	private Stream<Value> parse(final Float value, final IRI type) {
		return Stream.of(createLiteral(DoubleFormat.format(value), null, XMLSchema.FLOAT));
	}


	private Stream<Value> parse(final String string, final String base, final IRI type) {
		return Stream.of(type == null ? createLiteral(string, null, null)
				: type.equals(Values.ResourceType) ? createResource(base, string)
				: type.equals(Values.IRIType) ? createIRI(base, string)
				: type.equals(Values.BNodeType) ? createNode(string.startsWith("_:") ? string.substring(2) : string)
				: createLiteral(string, null, type));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Optional<Resource> resource(final Map<?, ?> object, final String base) {
		return Optional.ofNullable(object.get("this"))
				.map(id -> id instanceof String ? (String)id
						: error("'this' identifier field is not a string"))
				.map(id -> createResource(base, id));
	}

	private Optional<Resource> blank(final Map<?, ?> object) {
		return object.containsKey("this")
				? Optional.of(createNode())
				: Optional.empty();
	}

	private Optional<Resource> blank(final Shape shape) {
		return datatype(shape)
				.filter(type -> type.equals(Values.BNodeType))
				.map(type -> createNode());
	}

	private Optional<Resource> resource(final Shape shape) {
		return all(shape)
				.filter(values -> values.size() == 1)
				.map(values -> values.iterator().next())
				.filter(value -> value instanceof Resource)
				.map(value -> (Resource)value);
	}


	private IRI property(final String label, final String base, final Shape shape) {

		final Matcher matcher=EdgePattern.matcher(label);

		if ( matcher.matches() ) {

			final String alias=matcher.group("alias");

			final boolean inverse=matcher.group("inverse") != null;
			final String naked=matcher.group("naked");
			final String bracketed=matcher.group("bracketed");

			if ( naked != null ) {

				final IRI iri=createIRI(base, naked);

				return inverse ? inverse(iri) : iri;

			} else if ( bracketed != null ) {

				final IRI iri=createIRI(base, bracketed);

				return inverse ? inverse(iri) : iri;

			} else if ( shape != null ) {

				final Map<String, IRI> aliases=aliases(shape, JSONCodec.Reserved)
						.entrySet().stream()
						.collect(toMap(Map.Entry::getValue, Map.Entry::getKey));

				final IRI iri=aliases.get(alias);

				if ( iri == null ) {
					error(format("undefined property alias [%s]", alias));
				}

				return iri;

			} else {

				return error(format("no shape available to resolve property alias [%s]", alias));
			}

		} else {

			return error(format("malformed object relation [%s]", label));

		}
	}


	private Resource createResource(final String base, final String id) {
		return id.isEmpty() ? createNode()
				: id.startsWith("_:") ? createNode(id.substring(2))
				: createIRI(base, id);
	}

	private IRI createIRI(final String base, final String id) {
		return createURI(URI.create(base).resolve(id).toString());
	}


	private <T> T error(final String message) {

		// !!! report error location
		// !!! associate errors with setting and report through reportError()

		reportFatalError(message);

		return null; // unreachable
	}

}
