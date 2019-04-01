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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;

import javax.json.*;

import static com.metreeca.form.codecs.BaseCodec.aliases;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Field.fields;
import static com.metreeca.form.things.Values.format;
import static com.metreeca.form.things.Values.iri;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;


public final class JSONDecoder implements JSONCodec {

	private static final java.util.regex.Pattern StepPatten
			=java.util.regex.Pattern.compile("(?:^|[./])(\\^?(?:\\w+:.*|\\w+|<[^>]*>))");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final URI base;


	public JSONDecoder(final String base) {
		this.base=(base == null) ? null : URI.create(base);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public <V> V error(final String message) {
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
						value instanceof JsonObject ? (JsonObject)value
								: Json.createObjectBuilder().add("?", value).build(),
						shape,
						path(key, shape)
				);

		}

	}


	private Shape datatype(final JsonValue value) {
		return value instanceof JsonString
				? Datatype.datatype(iri(((JsonString)value).getString()))
				: error("datatype IRI is not a string");
	}

	private Shape clazz(final JsonValue value) {
		return value instanceof JsonString
				? Clazz.clazz(iri(((JsonString)value).getString()))
				: error("class IRI is not a string");
	}


	private Shape minExclusive(final JsonValue value, final Shape shape) {
		return value != null
				? MinExclusive.minExclusive(value(value, shape))
				: error("value is null");
	}

	private Shape maxExclusive(final JsonValue value, final Shape shape) {
		return value != null
				? MaxExclusive.maxExclusive(value(value, shape))
				: error("value is null");
	}

	private Shape minInclusive(final JsonValue value, final Shape shape) {
		return value != null
				? MinInclusive.minInclusive(value(value, shape))
				: error("value is null");
	}

	private Shape maxInclusive(final JsonValue value, final Shape shape) {
		return value != null
				? MaxInclusive.maxInclusive(value(value, shape))
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

			final Collection<Value> values=values(value, shape);

			return values.isEmpty() ? and() : All.all(values);
		}
	}

	private Shape any(final JsonValue value, final Shape shape) {
		if ( value.getValueType() == JsonValue.ValueType.NULL ) { return error("value is null"); } else {

			final Collection<Value> values=values(value, shape);

			return values.isEmpty() ? and() : Any.any(values);
		}
	}


	private Shape field(final JsonObject object, final Shape shape, final List<IRI> path) {
		if ( path.isEmpty() ) { return shape(object, shape); } else {

			final Map<IRI, Shape> fields=fields(shape); // !!! optimize (already explored during path parsing)

			final IRI head=path.get(0);
			final List<IRI> tail=path.subList(1, path.size());

			return Field.field(head, field(object, fields.get(head), tail));
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

	public Collection<Value> values(final JsonValue value, final Shape shape) {
		return value instanceof JsonArray
				? value.asJsonArray().stream().map(o -> value(o, shape)).collect(toList())
				: singleton(value(value, shape));
	}

	public Value value(final JsonValue value, final Shape shape) {
		return Datatype.datatype(shape)

				.map(datatype -> datatype.equals(Form.IRIType) && value instanceof JsonString ?
						iri(resolve(((JsonString)value).getString())) : value(value)
				)

				.orElseGet(() -> value(value));
	}


	private Value value(final JsonValue value) {
		return value instanceof JsonObject ? resource((JsonObject)value)
				: value instanceof JsonString ? literal((JsonString)value)
				: value instanceof JsonNumber ? literal((JsonNumber)value)
				: value.equals(JsonValue.TRUE) ? Values.literal(true)
				: value.equals(JsonValue.FALSE) ? Values.literal(false)
				: error("unsupported JSON value <"+value+">");
	}

	private IRI resource(final JsonObject object) {

		final JsonValue self=object.get("this");

		return self instanceof JsonString
				? iri(resolve(((JsonString)self).getString()))
				: error("this is not a string");
	}

	private Literal literal(final JsonString value) {
		return Values.literal(value.getString());
	}

	private Literal literal(final JsonNumber number) {
		return number.isIntegral()
				? Values.literal(number.bigIntegerValue())
				: Values.literal(number.bigDecimalValue());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String resolve(final String iri) {
		return base == null ? iri : base.resolve(iri).toString(); // !!! null base => absolute IRI
	}

}
