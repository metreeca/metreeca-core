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
import com.metreeca.form.shapes.*;
import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import javax.json.*;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Field.fields;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Values.direct;
import static com.metreeca.form.things.Values.format;
import static com.metreeca.form.things.Values.inverse;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


public abstract class JSONDecoder extends JSONCodec {

	private static final java.util.regex.Pattern StepPatten
			=java.util.regex.Pattern.compile("(?:^|[./])(\\^?(?:\\w+:.*|\\w+|<[^>]*>))");

	private static final java.util.regex.Pattern EdgePattern
			=java.util.regex.Pattern.compile("(?<alias>\\w+)|(?<inverse>\\^)?((?<naked>\\w+:.*)|<(?<bracketed>\\w+:.*)>)");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final URI base;


	protected JSONDecoder(final String base) {
		this.base=(base == null) ? null : URI.create(base);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected Resource bnode(final String id) {
		return Values.bnode(id);
	}

	protected IRI iri(final String iri) {
		return Values.iri(iri);
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


	//// Shapes ////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Shape shape(final JsonObject object, final Shape shape) {
		return and(object
				.entrySet()
				.stream()
				.map(entry -> shape(entry.getKey(), entry.getValue(), shape))
				.collect(toList())
		);
	}


	private Shape shape(final String key, final JsonValue value, final Shape shape) {

		switch ( key ) {

			case "^": return datatype(value);
			case "@": return clazz(value);

			case ">": return minExclusive(value, shape);
			case "<": return maxExclusive(value, shape);
			case ">=": return minInclusive(value, shape);
			case "<=": return maxInclusive(value, shape);

			case ">#": return minLength(value);
			case "#<": return maxLength(value);
			case "*": return pattern(value);
			case "~": return like(value);

			case ">>": return minCount(value);
			case "<<": return maxCount(value);
			case "!": return all(value, shape);
			case "?": return any(value, shape);

			default:

				return field(
						path(key, shape), value instanceof JsonObject ? (JsonObject)value
								: Json.createObjectBuilder().add("?", value).build(),
						shape
				);

		}

	}


	private Shape datatype(final JsonValue value) {
		return value instanceof JsonString
				? Datatype.datatype(iri(resolve(((JsonString)value).getString())))
				: error("datatype IRI is not a string");
	}


	private Shape clazz(final JsonValue value) {
		return value instanceof JsonString
				? Clazz.clazz(iri(resolve(((JsonString)value).getString())))
				: error("class IRI is not a string");
	}


	private Shape minExclusive(final JsonValue value, final Shape shape) {
		return value != null
				? MinExclusive.minExclusive(value(value, shape).getKey())
				: error("value is null");
	}

	private Shape maxExclusive(final JsonValue value, final Shape shape) {
		return value != null
				? MaxExclusive.maxExclusive(value(value, shape).getKey())
				: error("value is null");
	}

	private Shape minInclusive(final JsonValue value, final Shape shape) {
		return value != null
				? MinInclusive.minInclusive(value(value, shape).getKey())
				: error("value is null");
	}

	private Shape maxInclusive(final JsonValue value, final Shape shape) {
		return value != null
				? MaxInclusive.maxInclusive(value(value, shape).getKey())
				: error("value is null");
	}


	private Shape minLength(final JsonValue value) {
		return value instanceof JsonNumber
				? MinLength.minLength(((JsonNumber)value).intValue())
				: error("length is not a number");
	}

	private Shape maxLength(final JsonValue value) {
		return value instanceof JsonNumber
				? MaxLength.maxLength(((JsonNumber)value).intValue())
				: error("length is not a number");
	}

	private Shape pattern(final JsonValue value) {
		return value instanceof JsonString
				? ((JsonString)value).getString().isEmpty() ? and() : Pattern.pattern(((JsonString)value).getString())
				: error("pattern is not a string");
	}

	private Shape like(final JsonValue value) {
		return value instanceof JsonString
				? ((JsonString)value).getString().isEmpty() ? and() : Like.like(((JsonString)value).getString())
				: error("pattern is not a string");
	}


	private Shape minCount(final JsonValue value) {
		return value instanceof JsonNumber
				? MinCount.minCount(((JsonNumber)value).intValue())
				: error("length is not a number");
	}

	private Shape maxCount(final JsonValue value) {
		return value instanceof JsonNumber
				? MaxCount.maxCount(((JsonNumber)value).intValue())
				: error("length is not a number");
	}

	private Shape all(final JsonValue value, final Shape shape) {
		if ( value.getValueType() == JsonValue.ValueType.NULL ) { return error("value is null"); } else {

			final Collection<Value> values=values(value, shape).keySet();

			return values.isEmpty() ? and() : All.all(values);
		}
	}

	private Shape any(final JsonValue value, final Shape shape) {
		if ( value.getValueType() == JsonValue.ValueType.NULL ) { return error("value is null"); } else {

			final Collection<Value> values=values(value, shape).keySet();

			return values.isEmpty() ? and() : Any.any(values);
		}
	}


	private Shape field(final List<IRI> path, final JsonObject object, final Shape shape) {
		if ( path.isEmpty() ) { return shape(object, shape); } else {

			final Map<IRI, Shape> fields=fields(shape); // !!! optimize (already explored during path parsing)

			final IRI head=path.get(0);
			final List<IRI> tail=path.subList(1, path.size());

			return Field.field(head, field(tail, object, fields.get(head)));
		}
	}


	//// Paths /////////////////////////////////////////////////////////////////////////////////////////////////////////

	public List<IRI> path(final String path, final Shape shape) {

		final Collection<String> steps=new ArrayList<>();

		final Matcher matcher=StepPatten.matcher(path);

		final int length=path.length();

		int last=0;

		while ( matcher.lookingAt() ) {
			steps.add(matcher.group(1));
			matcher.region(last=matcher.end(), length);
		}

		if ( last != length ) {
			throw new IllegalArgumentException("malformed path ["+path+"]");
		}

		return path(steps, shape);
	}


	private List<IRI> path(final Iterable<String> steps, final Shape shape) {

		final List<IRI> edges=new ArrayList<>();

		Shape reference=shape;

		for (final String step : steps) {

			final Map<IRI, com.metreeca.form.Shape> fields=fields(reference);
			final Map<IRI, String> aliases=aliases(reference);

			final Map<String, IRI> index=new HashMap<>();

			// leading '^' for inverse edges added by Values.Inverse.toString() and Values.format(IRI)

			for (final IRI edge : fields.keySet()) {
				index.put(format(edge), edge); // inside angle brackets
				index.put(edge.toString(), edge); // naked IRI
			}

			for (final Map.Entry<IRI, String> entry : aliases.entrySet()) {
				index.put(entry.getValue(), entry.getKey());
			}

			final IRI edge=index.get(step);

			if ( edge == null ) {
				throw new NoSuchElementException("unknown path step ["+step+"]");
			}

			edges.add(edge);
			reference=fields.get(edge);

		}

		return edges;

	}


	//// Values ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Map<Value, Stream<Statement>> values(final JsonValue value, final Shape shape) {
		return (value instanceof JsonArray

				? value.asJsonArray().stream().map(v -> value(v, shape))
				: Stream.of(value(value, shape))

		).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	public Map.Entry<Value, Stream<Statement>> value(final JsonValue value, final Shape shape) {
		return value instanceof JsonArray ? value(value.asJsonArray(), shape)
				: value instanceof JsonObject ? value(value.asJsonObject(), shape)
				: value instanceof JsonString ? value((JsonString)value, shape)
				: value instanceof JsonNumber ? value((JsonNumber)value, shape)
				: value.equals(JsonValue.TRUE) ? entry(Values.True, Stream.empty())
				: value.equals(JsonValue.FALSE) ? entry(Values.False, Stream.empty())
				: error("unsupported JSON value <"+value+">");

	}


	private Map.Entry<Value, Stream<Statement>> value(final JsonArray array, final Shape shape) {
		return error("unsupported JSON value <"+array+">");
	}

	private Map.Entry<Value, Stream<Statement>> value(final JsonObject object, final Shape shape) {

		final IRI datatype=Datatype.datatype(shape).orElse(null);

		return Form.IRIType.equals(datatype) ? iri(object, shape)
				: Form.BNodeType.equals(datatype) ? bnode(object, shape)
				: Form.ResourceType.equals(datatype) || object.containsKey("this") ? resource(object, shape)
				: literal(object, shape);
	}

	private Map.Entry<Value, Stream<Statement>> value(final JsonString string, final Shape shape) {

		final IRI datatype=Datatype.datatype(shape).orElse(XMLSchema.STRING);

		final String lexical=string.getString();
		final boolean underscore=lexical.startsWith("_:");

		final Value value

				=Form.ResourceType.equals(datatype) ?
				underscore ? bnode(lexical.substring(2)) : iri(resolve(lexical))

				: Form.BNodeType.equals(datatype) ?
				bnode(underscore ? lexical.substring(2) : lexical)

				: Form.IRIType.equals(datatype) ?
				Values.iri(underscore ? lexical : resolve(lexical))

				: Values.literal(lexical, datatype);

		return entry(value, Stream.empty());
	}

	private Map.Entry<Value, Stream<Statement>> value(final JsonNumber number, final Shape shape) {

		final IRI datatype=Datatype.datatype(shape).orElse(null);

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


	//// Resources /////////////////////////////////////////////////////////////////////////////////////////////////////

	private Map.Entry<Value, Stream<Statement>> resource(final JsonObject object, final Shape shape) {

		final JsonValue iri=object.get("this");

		return iri instanceof JsonString && !((JsonString)iri).getString().startsWith("_:") ?
				iri(object, shape) : bnode(object, shape);
	}

	private Map.Entry<Value, Stream<Statement>> bnode(final JsonObject object, final Shape shape) {
		return properties(object, shape, Optional.ofNullable(object.get("this"))
				.filter(id -> id instanceof JsonString)
				.map(id -> ((JsonString)id).getString())
				.map(id -> bnode(id.startsWith("_:") ? id.substring(2) : id))
				.orElseGet(() -> error("'this' identifier field is not a string"))
		);
	}

	private Map.Entry<Value, Stream<Statement>> iri(final JsonObject object, final Shape shape) {
		return properties(object, shape, Optional.ofNullable(object.get("this"))
				.filter(id -> id instanceof JsonString)
				.map(id -> ((JsonString)id).getString())
				.map(this::resolve)
				.map(this::iri)
				.orElseGet(() -> error("'this' identifier field is not a string"))
		);
	}


	private Map.Entry<Value, Stream<Statement>> properties(final JsonObject object, final Shape shape, final Resource source) {

		final Map<IRI, Shape> fields=fields(shape);

		return entry(source, object.entrySet().stream()
				.filter(field -> !field.getKey().equals("this"))
				.flatMap(field -> {

					final String label=field.getKey();
					final JsonValue value=field.getValue();

					final IRI property=property(label, shape);

					return values(value, fields.get(property)).entrySet().stream().flatMap(entry -> {

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


	//// Literals //////////////////////////////////////////////////////////////////////////////////////////////////////

	private Map.Entry<Value, Stream<Statement>> literal(final JsonObject object, final Shape shape) {

		final JsonValue text=object.get("text");
		final JsonValue type=object.get("type");
		final JsonValue lang=object.get("lang");

		return text instanceof JsonString

				? type instanceof JsonString ? typed((JsonString)text, (JsonString)type)
				: lang instanceof JsonString ? tagged((JsonString)text, (JsonString)lang)
				: error("literal type/lang fields are missing or not strings")

				: error("literal text field is missing or not a string");
	}

	private Map.Entry<Value, Stream<Statement>> typed(final JsonString text, final JsonString type) {
		return entry(literal(text.getString(), iri(type.getString())), Stream.empty());
	}

	private Map.Entry<Value, Stream<Statement>> tagged(final JsonString text, final JsonString lang) {
		return entry(literal(text.getString(), lang.getString()), Stream.empty());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String resolve(final String iri) {
		return base == null ? iri : base.resolve(iri).toString(); // !!! null base => absolute IRI
	}

}
