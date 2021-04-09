/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rest.formats;

import com.metreeca.json.Shape;
import com.metreeca.json.Values;
import com.metreeca.json.shapes.Field;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.*;

import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.Field.labels;
import static com.metreeca.rest.formats.JSONLDInspector.driver;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toMap;

import static javax.json.Json.createObjectBuilder;


/**
 * Shape-driven JSON-LD to RDF decoder.
 *
 * <p>Converts leniently compacted/framed JSON-LD descriptions to RDF models.</p>
 */
final class JSONLDDecoder {

	private final IRI focus;
	private final Shape shape;
	private final Set<Statement> model;

	private final Map<String, String> keywords;

	private final URI base;

	private final Function<String, String> resolver;


	JSONLDDecoder(final IRI focus, final Shape shape, final Map<String, String> keywords) {

		this.focus=focus;
		this.shape=driver(shape);
		this.model=shape.outline(focus);

		this.keywords=keywords;

		this.base=URI.create(focus.stringValue());

		final Map<String, String> labels2keywords=keywords
				.entrySet().stream().collect(toMap(Entry::getValue, Entry::getKey));

		this.resolver=label -> labels2keywords.getOrDefault(label, label);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	Collection<Statement> decode(final JsonObject json) throws JsonException {

		if ( json == null ) {
			throw new NullPointerException("null json");
		}

		final Map<String, String> keywords=keywords(json);

		final String expected=focus.stringValue();
		final String declared=resolve(keywords.getOrDefault("@id", expected));

		if ( !declared.equals(expected) ) {
			error("conflicting object identifiers: expected <%s>, declared <%s>", expected, declared);
		}

		final JsonObject object=keywords.containsKey("@id") ? json
				: createObjectBuilder(json).add("@id", expected).build(); // make sure the root object contains @id

		final Collection<Statement> model=new ArrayList<>(this.model); // include inferred statements

		value(object, shape).getValue().forEachOrdered(model::add);

		return model;
	}


	Stream<Entry<Value, Stream<Statement>>> values(final JsonValue value, final Shape shape) {

		final boolean tagged=JSONLDInspector.tagged(shape);

		final Set<String> langs=tagged ? JSONLDInspector.langs(shape).orElseGet(Collections::emptySet) : emptySet();
		final String lang=langs.size() == 1 ? langs.iterator().next() : "";

		if ( tagged && value instanceof JsonArray && !lang.isEmpty() ) {

			return value.asJsonArray().stream().map(v -> v instanceof JsonString
					? literal((JsonString)v, lang)
					: value(v, shape)
			);

		} else if ( tagged && value instanceof JsonString && !lang.isEmpty() ) {

			return Stream.of(literal((JsonString)value, lang));

		} else if ( tagged && value instanceof JsonObject
				&& value.asJsonObject().keySet().stream().noneMatch(field -> field.startsWith("@"))
		) {

			return literals(value.asJsonObject());

		} else {

			return (value instanceof JsonArray ? value.asJsonArray().stream() : Stream.of(value))
					.map(v -> value(v, shape))
					.collect(toMap(Entry::getKey, Entry::getValue, Stream::concat))
					.entrySet()
					.stream();

		}
	}

	Entry<Value, Stream<Statement>> value(final JsonValue value, final Shape shape) {
		return value instanceof JsonArray ? error("unsupported JSON value <%s>", value.asJsonArray())
				: value instanceof JsonObject ? value(value.asJsonObject(), shape)
				: value instanceof JsonString ? value((JsonString)value, shape)
				: value instanceof JsonNumber ? value((JsonNumber)value, shape)
				: value.equals(JsonValue.TRUE) ? entry(True, Stream.empty())
				: value.equals(JsonValue.FALSE) ? entry(False, Stream.empty())
				: error("unsupported JSON value <%s>", value);
	}


	//// Values ////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Entry<Value, Stream<Statement>> value(final JsonObject object, final Shape shape) {

		final Map<String, String> keywords=keywords(object);

		final String id=keywords.get("@id");
		final String value=keywords.get("@value");
		final String type=keywords.get("@type");
		final String language=keywords.get("@language");

		return (id != null) ? resource(object, shape, resource(id))

				: (value == null) ? resource(object, shape, bnode())

				: (type != null) ? entry(literal(value, iri(type)), Stream.empty())
				: (language != null) ? entry(literal(value, language), Stream.empty())

				: entry(literal(value, JSONLDInspector.datatype(shape).orElse(XSD.STRING)), Stream.empty());
	}

	private Entry<Value, Stream<Statement>> value(final JsonString string, final Shape shape) {

		final String text=string.getString();
		final IRI type=
				JSONLDInspector.datatype(shape).filter(IRI.class::isInstance).map(IRI.class::cast).orElse(XSD.STRING);

		final Value value=ResourceType.equals(type) ? resource(text)
				: BNodeType.equals(type) ? bnode(text)
				: IRIType.equals(type) ? iri(text)
				: literal(text, type);

		return entry(value, Stream.empty());
	}

	private Entry<Value, Stream<Statement>> value(final JsonNumber number, final Shape shape) {

		final IRI datatype=JSONLDInspector.datatype(shape).orElse(null);

		final Literal value

				=XSD.DECIMAL.equals(datatype) ? Values.literal(number.bigDecimalValue())
				: XSD.INTEGER.equals(datatype) ? Values.literal(number.bigIntegerValue())

				: XSD.DOUBLE.equals(datatype) ? Values.literal(number.numberValue().doubleValue(), false)
				: XSD.FLOAT.equals(datatype) ? Values.literal(number.numberValue().floatValue())

				: XSD.LONG.equals(datatype) ? Values.literal(number.numberValue().longValue())
				: XSD.INTEGER.equals(datatype) ? Values.literal(number.numberValue().intValue())
				: XSD.SHORT.equals(datatype) ? Values.literal(number.numberValue().shortValue())
				: XSD.BYTE.equals(datatype) ? Values.literal(number.numberValue().byteValue())

				: number.isIntegral() ? Values.literal(number.bigIntegerValue())
				: Values.literal(number.bigDecimalValue());

		return entry(value, Stream.empty());
	}


	private Entry<Value, Stream<Statement>> resource(final JsonObject object, final Shape shape, final Resource focus) {

		final Map<String, Field> labels=labels(shape, keywords);

		return entry(focus, object.entrySet().stream().flatMap(entry -> {

			final String label=resolver.apply(entry.getKey());
			final JsonValue value=entry.getValue();

			if ( label.equals("@type") && value instanceof JsonString ) {

				return Stream.of(statement(focus, RDF.TYPE, iri(((JsonString)value).getString())));

			} else if ( !label.startsWith("@") && !value.equals(JsonValue.NULL) ) {

				final Field field=labels.get(label);

				if ( field == null ) {
					return error("unknown property label <%s>", label);
				}

				return values(value, field.shape()).flatMap(pair -> {

					final Value target=pair.getKey();
					final Stream<Statement> model=pair.getValue();

					final Statement edge=traverse(field.iri(),

							iri -> statement(focus, iri, target),

							iri -> target instanceof Resource
									? statement((Resource)target, iri, focus)
									: error("target for inverse property is not a resource <%s: %s>", label, pair)

					);

					return Stream.concat(Stream.of(edge), model);

				});

			} else {

				return Stream.empty();

			}

		}));
	}


	//// Tagged Literals ///////////////////////////////////////////////////////////////////////////////////////////////

	private Stream<Entry<Value, Stream<Statement>>> literals(final JsonObject json) {
		return json.entrySet().stream().flatMap(entry -> {

			final String lang=entry.getKey();
			final JsonValue value=entry.getValue();

			if ( lang.isEmpty() ) {
				error("empty language tag");
			}

			return (value instanceof JsonArray ? value.asJsonArray().stream() : Stream.of(value))
					.map(v -> v instanceof JsonString ? v : error("<%s> is not a string", v))
					.map(v -> literal((JsonString)v, lang));

		});
	}

	private Entry<Value, Stream<Statement>> literal(final JsonString json, final String lang) {
		return entry(literal(json.getString(), lang), Stream.empty());
	}


	//// Factories /////////////////////////////////////////////////////////////////////////////////////////////////////

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


	private Literal literal(final String text, final IRI type) { return Values.literal(text, type);}

	private Literal literal(final String text, final String lang) {
		return Values.literal(text, lang);
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
								: error("<%s> field is not a string", e.getKey()),

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
