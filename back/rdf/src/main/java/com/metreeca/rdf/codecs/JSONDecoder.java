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

package com.metreeca.rdf.codecs;

import com.metreeca.rdf.Values;
import com.metreeca.rdf._probes.Inferencer;
import com.metreeca.rdf._probes._Optimizer;
import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Redactor;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.json.*;

import static com.metreeca.rdf.Values.direct;
import static com.metreeca.rdf.Values.inverse;
import static com.metreeca.rdf.Values.BNodeType;
import static com.metreeca.rdf.Values.IRIType;
import static com.metreeca.rdf.Values.ResourceType;
import static com.metreeca.tree.Shape.Convey;
import static com.metreeca.tree.Shape.Mode;
import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Field.fields;

import static java.util.stream.Collectors.toMap;


abstract class JSONDecoder extends JSONCodec {

	private static final JsonString Default=Json.createValue("");

	private static final Pattern EdgePattern
			=Pattern.compile("(?<alias>\\w+)|(?<inverse>\\^)?((?<naked>\\w+:.*)|<(?<bracketed>\\w+:.*)>)");


	private static final Function<Shape, Shape> ShapeCompiler=s -> s
			.map(new Redactor(Mode, Convey)) // remove internal filtering shapes
			.map(new _Optimizer())
			.map(new Inferencer()) // infer implicit constraints to drive json shorthands
			.map(new _Optimizer());


	private static <K, V> Map.Entry<K, V> entry(final K key, final V value) {
		return new AbstractMap.SimpleImmutableEntry<>(key, value);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final URI base;


	protected JSONDecoder(final String base) {
		this.base=(base == null) ? null : URI.create(base);
	}


	//// Factories /////////////////////////////////////////////////////////////////////////////////////////////////////

	protected String resolve(final String iri) {
		try {
			return base == null ? iri : base.resolve(new URI(iri)).toString();
		} catch ( final URISyntaxException e ) {
			throw new JsonException(String.format("invalid IRI: %s", e.getMessage()));
		}
	}


	protected Resource resource(final String id) {
		return id.isEmpty() ? Values.bnode() : id.startsWith("_:") ? Values.bnode(id.substring(2)) : Values.iri(resolve(id));
	}

	protected Resource bnode() {
		return Values.bnode();
	}

	protected Resource bnode(final String id) {
		return id.isEmpty() ? Values.bnode() : id.startsWith("_:") ? Values.bnode(id.substring(2)) : Values.bnode(id);
	}

	protected IRI iri(final String iri) {
		return iri.isEmpty() ? Values.iri() : Values.iri(resolve(iri));
	}

	protected Literal literal(final String text, final IRI type) {
		return Values.literal(text, type);
	}

	protected Literal literal(final String text, final String lang) {
		return Values.literal(text, lang);
	}

	protected Statement statement(final Resource subject, final IRI predicate, final Value object) {
		return Values.statement(subject, predicate, object);
	}

	protected <V> V error(final String message) {
		throw new JsonException(message);
	}


	//// Values ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected Map<Value, Stream<Statement>> values(final JsonValue value, final Shape shape, final Resource focus) {

		final Shape driver=(shape == null) ? null : shape.map(ShapeCompiler);

		return (value instanceof JsonArray

				? value.asJsonArray().stream().map(v -> value(v, driver, focus))
				: Stream.of(value(value, driver, focus))

		).collect(toMap(Map.Entry::getKey, Map.Entry::getValue, Stream::concat));
	}

	protected Map.Entry<Value, Stream<Statement>> value(final JsonValue value, final Shape shape, final Resource focus) {

		final Shape driver=(shape == null) ? null : shape.map(ShapeCompiler);

		return value instanceof JsonArray ? value(value.asJsonArray(), driver, focus)
				: value instanceof JsonObject ? value(value.asJsonObject(), driver, focus)
				: value instanceof JsonString ? value((JsonString)value, driver)
				: value instanceof JsonNumber ? value((JsonNumber)value, driver)
				: value.equals(JsonValue.TRUE) ? entry(Values.True, Stream.empty())
				: value.equals(JsonValue.FALSE) ? entry(Values.False, Stream.empty())
				: error("unsupported JSON value <"+value+">");
	}


	private Map.Entry<Value, Stream<Statement>> value(final JsonArray array, final Shape shape, final Resource focus) {
		return error("unsupported JSON value <"+array+">");
	}

	private Map.Entry<Value, Stream<Statement>> value(final JsonObject object, final Shape shape, final Resource focus) {

		final String thiz=thiz(object);
		final String type=type(object);

		final String datatype=type.isEmpty()
				? datatype(shape).map(Values::iri).map(Value::stringValue).orElse("")
				: type;

		final Value value=(thiz.isEmpty() && type.isEmpty() && focus != null) ? focus

				: datatype.isEmpty() ? resource(thiz)

				: datatype.equals(IRIType.stringValue()) ? iri(thiz)
				: datatype.equals(BNodeType.stringValue()) ? bnode(thiz)
				: datatype.equals(ResourceType.stringValue()) ? resource(thiz)

				: datatype.startsWith("@") ? literal(thiz, type.substring(1))

				: literal(thiz, iri(datatype));

		return focus != null && !focus.equals(value) ? entry(focus, Stream.empty())
				: value instanceof Resource ? properties(object, shape, (Resource)value)
				: entry(value, Stream.empty());

	}

	private Map.Entry<Value, Stream<Statement>> value(final JsonString string, final Shape shape) {

		final String text=string.getString();
		final IRI type=datatype(shape).filter(o -> o instanceof IRI).map(o -> (IRI)o).orElse(XMLSchema.STRING);

		final Value value=ResourceType.equals(type) ? resource(text)
				: BNodeType.equals(type) ? bnode(text)
				: IRIType.equals(type) ? iri(text)
				: literal(text, type);

		return entry(value, Stream.empty());
	}

	private Map.Entry<Value, Stream<Statement>> value(final JsonNumber number, final Shape shape) {

		final IRI datatype=datatype(shape).map(Values::iri).orElse(null);

		final Literal value

				=XMLSchema.DECIMAL.equals(datatype) ? Values.literal(number.bigDecimalValue())
				: XMLSchema.INTEGER.equals(datatype) ? Values.literal(number.bigIntegerValue())

				: XMLSchema.DOUBLE.equals(datatype) ? Values.literal(number.numberValue().doubleValue())
				: XMLSchema.FLOAT.equals(datatype) ? Values.literal(number.numberValue().floatValue())

				: XMLSchema.LONG.equals(datatype) ? Values.literal(number.numberValue().longValue())
				: XMLSchema.INTEGER.equals(datatype) ? Values.literal(number.numberValue().intValue())
				: XMLSchema.SHORT.equals(datatype) ? Values.literal(number.numberValue().shortValue())
				: XMLSchema.BYTE.equals(datatype) ? Values.literal(number.numberValue().byteValue())

				: number.isIntegral() ? Values.literal(number.bigIntegerValue())
				: Values.literal(number.bigDecimalValue());

		return entry(value, Stream.empty());
	}


	private Map.Entry<Value, Stream<Statement>> properties(final JsonObject object, final Shape shape, final Resource source) {

		final Map<Object, Shape> fields=fields(shape);

		return entry(source, object.entrySet().stream()
				.filter(field -> !Reserved.contains(field.getKey()))
				.flatMap(field -> {

					final String label=field.getKey();
					final JsonValue value=field.getValue();

					final IRI property=property(label, shape);

					return values(value, fields.get(property), null).entrySet().stream().flatMap(entry -> {

						final Value target=entry.getKey();
						final Stream<Statement> model=entry.getValue();

						final Statement edge=direct(property) ? statement(source, property, target)
								: target instanceof Resource ? statement((Resource)target, inverse(property), source)
								: error(String.format("target for inverse property is not a resource [%s: %s]", label, entry));

						return Stream.concat(Stream.of(edge), model);

					});

				}));
	}

	private IRI property(final String label, final Shape shape) {

		final Matcher matcher=EdgePattern.matcher(label);

		if ( matcher.matches() ) {

			final String alias=matcher.group("alias");

			final boolean inverse=matcher.group("inverse") != null;
			final String naked=matcher.group("naked");
			final String bracketed=matcher.group("bracketed");

			if ( naked != null ) {

				final IRI iri=iri(resolve(naked));

				return inverse ? inverse(iri) : iri;

			} else if ( bracketed != null ) {

				final IRI iri=iri(resolve(bracketed));

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

			return error(String.format("malformed object relation [%s]", label));

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String thiz(final JsonObject object) {
		return Optional.of(object.getOrDefault(This, Default))
				.filter(value -> value instanceof JsonString)
				.map(value -> ((JsonString)value).getString())
				.orElseGet(() -> error("literal '_this' field is not a string"));
	}

	private String type(final JsonObject object) {
		return Optional.of(object.getOrDefault(Type, Default))
				.filter(value -> value instanceof JsonString)
				.map(value -> ((JsonString)value).getString())
				.orElseGet(() -> error("literal '_type' field is not a string"));
	}

}
