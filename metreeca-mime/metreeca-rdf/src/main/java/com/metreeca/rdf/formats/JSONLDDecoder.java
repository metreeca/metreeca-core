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

package com.metreeca.rdf.formats;

import com.metreeca.json.Shape;
import com.metreeca.rdf.Values;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.helpers.RDFParserHelper;

import javax.json.*;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.metreeca.json.Shape.pass;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.fields;
import static com.metreeca.rdf.Values.*;
import static com.metreeca.rdf.formats.JSONLDCodecs.aliases;
import static com.metreeca.rdf.formats.JSONLDCodecs.driver;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;


final class JSONLDDecoder {

	private static final Pattern StepPattern=Pattern.compile( // !!! review/remove
			"(?:^|[./])(?<step>(?<alias>\\w+)|(?<inverse>\\^)?((?<naked>\\w+:.*)|<(?<bracketed>\\w+:[^>]*)>))"
	);


	private static <K, V> Map.Entry<K, V> entry(final K key, final V value) {
		return new SimpleImmutableEntry<>(key, value);
	}


	/*
	 * Creates a function mapping from property aliases to property ids, defaulting to the alias if no id is found.
	 */
	private static Function<String, String> keywords(final Shape shape) {

		final Map<String, String> keywords=JSONLDCodecs.keywords(shape)

				.collect(toMap(Map.Entry::getValue, Map.Entry::getKey, (x, y) -> {

					if ( !x.equals(y) ) {
						throw new IllegalArgumentException("conflicting aliases for JSON-LD keywords");
					}

					return x;

				}));

		return alias -> keywords.getOrDefault(alias, alias);
	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final URI base;
	private final Resource focus;
	private final Shape shape;
	private final ParserConfig options;

	private final Function<String, String> keywords;

	JSONLDDecoder(final String base, final Resource focus, final Shape shape, final ParserConfig options) {

		this.base=(base == null) ? null : URI.create(base);
		this.focus=focus;
		this.shape=(shape == null || pass(shape)) ? null : driver(shape);
		this.options=options;

		this.keywords=(shape == null) ? identity() : keywords(shape);

	}



	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Collection<Statement> decode(final Reader reader) throws JsonException {

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		return decode(Json.createReader(reader).readObject());

	}

	public Collection<Statement> decode(final JsonValue json) throws JsonException {

		if ( json == null ) {
			throw new NullPointerException("null json");
		}

		final Collection<Statement> model=new ArrayList<>();

		values(json, shape, focus)
				.values()
				.stream()
				.flatMap(identity())
				.forEachOrdered(model::add);

		return model;
	}


	//// Factories ////////////////////////////////////////////////////////////////////////////////////////////////////

	private String resolve(final String iri) {
		try {
			return base == null ? iri : base.resolve(new URI(iri)).toString();
		} catch ( final URISyntaxException e ) {
			throw new JsonException(String.format("invalid IRI: %s", e.getMessage()));
		}
	}


	private Resource resource(final String id) {
		return id.isEmpty() ? factory().createBNode()
				: id.startsWith("_:") ? factory().createBNode(id.substring(2))
				: factory().createIRI(resolve(id));
	}

	private Resource bnode() {
		return Values.bnode();
	}

	private Resource bnode(final String id) {
		return id.isEmpty() ? Values.bnode()
				: id.startsWith("_:") ? Values.bnode(id.substring(2))
				: Values.bnode(id);
	}

	private IRI iri(final String iri) {
		return iri.isEmpty() ? Values.iri() : Values.iri(resolve(iri));
	}

	private Literal literal(final String text, final IRI type) { return literal(text, null, type);}

	private Literal literal(final String text, final String lang) {
		return literal(text, lang, null);
	}

	private Literal literal(final String text, final String lang, final IRI type) {
		try {

			final ParseErrorCollector listener=new ParseErrorCollector();

			final Literal literal=RDFParserHelper.createLiteral(text, lang, type, options, listener, factory());

			final String errors=Stream.of(listener.getFatalErrors(), listener.getErrors())

					.flatMap(Collection::stream)
					.collect(Collectors.joining("; "));

			if ( !errors.isEmpty() ) {
				throw new JsonException(errors);
			}

			return literal;

		} catch ( final RDFParseException e ) {

			throw new JsonException(e.getMessage());

		}
	}

	private Statement statement(final Resource subject, final IRI predicate, final Value object) {
		return Values.statement(subject, predicate, object);
	}

	private <V> V error(final String message) {
		throw new JsonException(message);
	}


	//// Paths ////////////////////////////////////////////////////////////////////////////////////////////////////////

	List<IRI> path(final String path, final Shape shape) { // !!! remove

		final List<IRI> steps=new ArrayList<>();
		final Matcher matcher=StepPattern.matcher(path);

		int last=0;
		Shape reference=shape;

		while ( matcher.lookingAt() ) {

			final Map<Object, Shape> fields=fields(reference);
			final Map<IRI, String> aliases=aliases(reference);

			final Map<String, IRI> index=new HashMap<>();

			// leading '^' for inverse edges added by Values.Inverse.toString() and Values.format(IRI)

			fields.keySet().stream().map(_ValueParser::_iri).forEach(edge -> {
				index.put(format(edge), edge); // inside angle brackets
				index.put(edge.toString(), edge); // naked IRI
			});

			aliases.forEach((iri, alias) -> index.put(alias, iri));

			final String step=matcher.group("step");
			final IRI iri=index.get(step);

			if ( iri == null ) {
				throw new JsonException("unknown path step ["+step+"]");
			}

			steps.add(iri);
			reference=fields.get(iri);

			matcher.region(last=matcher.end(), path.length());
		}

		if ( last != path.length() ) {
			throw new JsonException("malformed path ["+path+"]");
		}

		return steps;
	}


	//// Values ///////////////////////////////////////////////////////////////////////////////////////////////////////

	private Map<Value, Stream<Statement>> values(
			final JsonValue value, final Shape shape, final Resource focus) {
		return (value instanceof JsonArray

				? value.asJsonArray().stream().map(v -> value(v, shape, focus))
				: Stream.of(value(value, shape, focus))

		).collect(toMap(Map.Entry::getKey, Map.Entry::getValue, Stream::concat));
	}

	Map.Entry<Value, Stream<Statement>> value( // !!! private
			final JsonValue value, final Shape shape, final Resource focus) {
		return value instanceof JsonArray ? value(value.asJsonArray(), shape, focus)
				: value instanceof JsonObject ? value(value.asJsonObject(), shape, focus)
				: value instanceof JsonString ? value((JsonString)value, shape)
				: value instanceof JsonNumber ? value((JsonNumber)value, shape)
				: value.equals(JsonValue.TRUE) ? entry(True, Stream.empty())
				: value.equals(JsonValue.FALSE) ? entry(False, Stream.empty())
				: error("unsupported JSON value <"+value+">");
	}


	private Map.Entry<Value, Stream<Statement>> value(
			final JsonArray array, final Shape shape, final Resource focus) {
		return error("unsupported JSON value <"+array+">");
	}

	private Map.Entry<Value, Stream<Statement>> value(
			final JsonObject object, final Shape shape, final Resource focus) {

		String id=null;
		String value=null;
		String type=null;
		String language=null;

		for (final Map.Entry<String, JsonValue> entry : object.entrySet()) {

			final String label=keywords.apply(entry.getKey());

			final Supplier<String> string=() -> entry.getValue() instanceof JsonString
					? ((JsonString)entry.getValue()).getString()
					: error("'"+entry.getKey()+"' field is not a string");

			if ( label.equals(JSONLDFormat.id) ) {
				id=string.get();
			} else if ( label.equals(JSONLDFormat.value) ) {
				value=string.get();
			} else if ( label.equals(JSONLDFormat.type) ) {
				type=string.get();
			} else if ( label.equals(JSONLDFormat.language) ) {
				language=string.get();
			}

		}

		final Value _value

				=(id != null) ? resource(id)

				: (value != null) ? (type != null) ? literal(value, iri(type))
				: (language != null) ? literal(value, language)
				: literal(value, datatype(shape).map(_ValueParser::_iri).orElse(XSD.STRING))

				: focus != null ? focus : bnode();

		return focus != null && !focus.equals(_value) ? entry(focus, Stream.empty())
				: _value instanceof Resource ? properties(object, shape, (Resource)_value)
				: entry(_value, Stream.empty());

	}

	private Map.Entry<Value, Stream<Statement>> value(
			final JsonString string, final Shape shape) {

		final String text=string.getString();
		final IRI type=datatype(shape).filter(IRI.class::isInstance).map(IRI.class::cast).orElse(XSD.STRING);

		final Value value=ResourceType.equals(type) ? resource(text)
				: BNodeType.equals(type) ? bnode(text)
				: IRIType.equals(type) ? iri(text)
				: literal(text, type);

		return entry(value, Stream.empty());
	}

	private Map.Entry<Value, Stream<Statement>> value(
			final JsonNumber number, final Shape shape) {

		final IRI datatype=datatype(shape).map(_ValueParser::_iri).orElse(null);

		final Literal value

				=XSD.DECIMAL.equals(datatype) ? Values.literal(number.bigDecimalValue())
				: XSD.INTEGER.equals(datatype) ? Values.literal(number.bigIntegerValue())

				: XSD.DOUBLE.equals(datatype) ? Values.literal(number.numberValue().doubleValue())
				: XSD.FLOAT.equals(datatype) ? Values.literal(number.numberValue().floatValue())

				: XSD.LONG.equals(datatype) ? Values.literal(number.numberValue().longValue())
				: XSD.INTEGER.equals(datatype) ? Values.literal(number.numberValue().intValue())
				: XSD.SHORT.equals(datatype) ? Values.literal(number.numberValue().shortValue())
				: XSD.BYTE.equals(datatype) ? Values.literal(number.numberValue().byteValue())

				: number.isIntegral() ? Values.literal(number.bigIntegerValue())
				: Values.literal(number.bigDecimalValue());

		return entry(value, Stream.empty());
	}


	private Map.Entry<Value, Stream<Statement>> properties(
			final JsonObject object, final Shape shape, final Resource source) {

		final Map<Object, Shape> fields=fields(shape);

		return entry(source, object.entrySet().stream().flatMap(field -> {

			final String label=keywords.apply(field.getKey());
			final JsonValue value=field.getValue();

			if ( label.equals(JSONLDFormat.type) && value instanceof JsonString ) {

				return Stream.of(statement(source, RDF.TYPE, iri(((JsonString)value).getString())));

			} else if ( !label.startsWith("@") ) {

				final IRI property=property(label, shape);

				return values(value, fields.get(property), null).entrySet().stream().flatMap(entry -> {

					final Value target=entry.getKey();
					final Stream<Statement> model=entry.getValue();

					final Statement edge=direct(property) ? statement(source, property, target)
							: target instanceof Resource ? statement((Resource)target, inverse(property), source)
							: error(String.format("target for inverse property is not a resource [%s: %s]", label,
							entry));

					return Stream.concat(Stream.of(edge), model);

				});

			} else {

				return Stream.empty();

			}

		}));
	}

	private IRI property(final String label, final Shape shape) {

		final Matcher matcher=StepPattern.matcher(label);

		if ( matcher.matches() ) {

			final String alias=matcher.group("alias");

			final boolean inverse=matcher.group("inverse") != null;
			final String naked=matcher.group("naked");
			final String bracketed=matcher.group("bracketed");

			if ( naked != null ) {

				final IRI iri=iri(naked);

				return inverse ? inverse(iri) : iri;

			} else if ( bracketed != null ) {

				final IRI iri=iri(bracketed);

				return inverse ? inverse(iri) : iri;

			} else if ( shape != null ) {

				final Map<String, IRI> aliases=aliases(shape)
						.entrySet().stream()
						.collect(toMap(Map.Entry::getValue, Map.Entry::getKey));

				final IRI iri=aliases.get(alias);

				if ( iri == null ) {
					error(String.format("undefined property alias [%s]", alias));
				}

				return iri;

			} else {

				return error(String.format("no shape available to resolve property alias [%s]", alias));

			}

		} else {

			return error(String.format("malformed object property [%s]", label));

		}
	}


}
