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
import com.metreeca.json.shapes.Field;
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
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.metreeca.json.Shape.pass;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.rdf.Values.*;
import static com.metreeca.rdf.formats.JSONLDCodecs.aliases;
import static com.metreeca.rdf.formats.JSONLDCodecs.driver;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static javax.json.Json.createObjectBuilder;


final class JSONLDDecoder {

	private final IRI focus;
	private final Shape shape;
	private final ParserConfig options;

	private final URI base;
	private final Function<String, String> resolver;

	JSONLDDecoder(final IRI focus, final Shape shape, final ParserConfig options) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( options == null ) {
			throw new NullPointerException("null options");
		}

		this.focus=focus;
		this.shape=pass(shape) ? null : driver(shape);
		this.options=options;

		this.base=URI.create(focus.stringValue());

		final Map<String, String> aliases2keywords=JSONLDCodecs.keywords(shape)

				.collect(toMap(Entry::getValue, Entry::getKey, (x, y) -> {

					if ( !x.equals(y) ) {
						throw new IllegalArgumentException("conflicting aliases for JSON-LD keywords");
					}

					return x;

				}));

		this.resolver=alias -> aliases2keywords.getOrDefault(alias, alias);
	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Collection<Statement> decode(final JsonObject json) throws JsonException {

		if ( json == null ) {
			throw new NullPointerException("null json");
		}

		final Map<String, String> keywords=keywords(json);

		final String expected=focus.stringValue();
		final String declared=resolve(keywords.getOrDefault("@id", expected));

		if ( !declared.equals(expected) ) {
			error("conflicting object identifiers: expected <%s>, declared <%s>", expected, declared);
		}

		final JsonObject object=keywords.containsKey("@id") ? // make sure the root object contains @id
				json : createObjectBuilder(json).add("@id", expected).build();

		final Collection<Statement> model=new ArrayList<>();

		value(object, shape).getValue().forEachOrdered(model::add);

		return model;
	}

	public Collection<Statement> decode(final Reader reader) throws JsonException {

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		try ( final JsonReader jsonReader=Json.createReader(reader) ) {
			return decode(jsonReader.readObject());
		}

	}


	//// Values ///////////////////////////////////////////////////////////////////////////////////////////////////////

	private Stream<Entry<Value, Stream<Statement>>> values(final JsonValue value, final Shape shape) {
		return (value instanceof JsonArray

				? value.asJsonArray().stream().map(v -> value(v, shape))
				: Stream.of(value(value, shape))

		).collect(toMap(Entry::getKey, Entry::getValue, Stream::concat)).entrySet().stream();
	}

	/* !!! private */ Entry<Value, Stream<Statement>> value(final JsonValue value, final Shape shape) {
		return value instanceof JsonArray ? error("unsupported JSON value <%s>", value.asJsonArray())
				: value instanceof JsonObject ? value(value.asJsonObject(), shape)
				: value instanceof JsonString ? value((JsonString)value, shape)
				: value instanceof JsonNumber ? value((JsonNumber)value, shape)
				: value.equals(JsonValue.TRUE) ? entry(True, Stream.empty())
				: value.equals(JsonValue.FALSE) ? entry(False, Stream.empty())
				: error("unsupported JSON value <%s>", value);
	}


	private Entry<Value, Stream<Statement>> value(final JsonObject object, final Shape shape) {

		final Map<String, String> keywords=keywords(object);

		final String id=keywords.get("@id");
		final String value=keywords.get("@value");
		final String type=keywords.get("@type");
		final String language=keywords.get("@language");

		return (id != null) ? fields(object, shape, resource(id))

				: (value == null) ? fields(object, shape, bnode())

				: (type != null) ? entry(literal(value, iri(type)), Stream.empty())
				: (language != null) ? entry(literal(value, language), Stream.empty())

				: entry(literal(value, datatype(shape).map(_ValueParser::_iri).orElse(XSD.STRING)), Stream.empty());
	}

	private Entry<Value, Stream<Statement>> value(final JsonString string, final Shape shape) {

		final String text=string.getString();
		final IRI type=datatype(shape).filter(IRI.class::isInstance).map(IRI.class::cast).orElse(XSD.STRING);

		final Value value=ResourceType.equals(type) ? resource(text)
				: BNodeType.equals(type) ? bnode(text)
				: IRIType.equals(type) ? iri(text)
				: literal(text, type);

		return entry(value, Stream.empty());
	}

	private Entry<Value, Stream<Statement>> value(final JsonNumber number, final Shape shape) {

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


	private Entry<Value, Stream<Statement>> fields(final JsonObject object, final Shape shape, final Resource focus) {

		final Map<Object, Shape> fields=Field.fields(shape);

		final Map<String, IRI> aliases=aliases(shape)
				.entrySet()
				.stream()
				.collect(toMap(Entry::getValue, Entry::getKey));

		return entry(focus, object.entrySet().stream().flatMap(field -> {

			final String label=resolver.apply(field.getKey());
			final JsonValue value=field.getValue();

			if ( label.equals("@type") && value instanceof JsonString ) {

				return Stream.of(statement(focus, RDF.TYPE, iri(((JsonString)value).getString())));

			} else if ( !label.startsWith("@") && !value.equals(JsonValue.NULL) ) {

				final IRI property=aliases.computeIfAbsent(label, this::iri);

				return values(value, fields.get(property)).flatMap(entry -> {

					final Value target=entry.getKey();
					final Stream<Statement> model=entry.getValue();

					final Statement edge=direct(property) ? statement(focus, property, target)
							: target instanceof Resource ? statement((Resource)target, inverse(property), focus)
							: error("target for inverse property is not a resource <%s: %s>", label, entry);

					return Stream.concat(Stream.of(edge), model);

				});

			} else {

				return Stream.empty();

			}

		}));
	}


	//// Factories ////////////////////////////////////////////////////////////////////////////////////////////////////

	private Resource resource(final String id) {
		return id.isEmpty() ? factory().createBNode()
				: id.startsWith("_:") ? factory().createBNode(id.substring(2))
				: factory().createIRI(resolve(id));
	}


	private BNode bnode() {
		return Values.bnode();
	}

	private BNode bnode(final String id) {
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


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String resolve(final String iri) {
		try {

			return base.resolve(new URI(iri)).toString();

		} catch ( final URISyntaxException e ) {

			return error("invalid IRI <%s>", e.getMessage());

		}
	}

	private Map<String, String> keywords(final Map<String, JsonValue> object) {
		return object.entrySet().stream()

				.map(e -> entry(resolver.apply(e.getKey()), e.getValue()))
				.filter(e -> e.getKey().startsWith("@"))

				.collect(toMap(

						Entry::getKey,

						e -> e.getValue() instanceof JsonString ? ((JsonString)e.getValue()).getString()
								: error("<'%s'> field is not a string", e.getKey()),

						(x, y) -> x.equals(y) ? x
								: error("conflicting values for JSON-LD keyword <%s> / <%s>", x, y)

				));
	}


	private <K, V> Entry<K, V> entry(final K key, final V value) {
		return new SimpleImmutableEntry<>(key, value);
	}

	private <V> V error(final String format, final Object... args) {
		throw new JsonException(format(format, args));
	}

}
