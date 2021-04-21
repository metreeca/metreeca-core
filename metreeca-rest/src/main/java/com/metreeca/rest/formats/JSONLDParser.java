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

import com.metreeca.json.*;
import com.metreeca.json.queries.Stats;
import com.metreeca.json.queries.Terms;
import com.metreeca.json.shapes.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.io.StringReader;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import javax.json.*;

import static com.metreeca.json.Order.decreasing;
import static com.metreeca.json.Order.increasing;
import static com.metreeca.json.Values.format;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.queries.Items.items;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Field.labels;
import static com.metreeca.json.shapes.Link.link;
import static com.metreeca.rest.Request.search;
import static com.metreeca.rest.Xtream.decode;
import static com.metreeca.rest.formats.JSONLDInspector.driver;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import static javax.json.JsonValue.EMPTY_JSON_ARRAY;
import static javax.json.JsonValue.NULL;

/**
 * JSON-LD query parser.
 */
final class JSONLDParser {

	private static final JsonString EMPTY_JSON_STRING=Json.createValue("");

	private static final java.util.regex.Pattern StepPattern=java.util.regex.Pattern.compile("(?:^|\\.)(\\w+\\b)");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Shape shape;
	private final Shape baseline;
	private final Map<String, String> keywords;

	private final JSONLDDecoder decoder;


	JSONLDParser(final IRI focus, final Shape shape, final Map<String, String> keywords) {

		this.shape=driver(shape);
		this.baseline=shape;
		this.keywords=keywords;

		this.decoder=new JSONLDDecoder(focus, shape, keywords);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	Query parse(final String query) throws JsonException, NoSuchElementException {
		return query.isEmpty() ? items(baseline)
				: query.startsWith("%7B") ? json(decode(query))
				: query.startsWith("{") ? json(query)
				: form(query);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Query json(final String query) {
		return json(Optional
				.of(query)
				.map(v -> Json.createReader(new StringReader(v)).readValue())
				.filter(JsonObject.class::isInstance)
				.map(JsonValue::asJsonObject)
				.orElseGet(() -> error("filter is not an object"))
		);
	}

	private Query json(final JsonObject json) {

		final Shape filter=filter(json);

		final List<IRI> terms=terms(json);
		final List<IRI> stats=stats(json);

		final List<Order> order=order(json);

		final int offset=offset(json);
		final int limit=limit(json);

		final Shape filtered=and(baseline, Guard.filter(filter)); // filtering only >> don't include in results

		return terms != null ? Terms.terms(filtered, terms, offset, limit)
				: stats != null ? Stats.stats(filtered, stats, offset, limit)
				: items(filtered, order, offset, limit);
	}


	private Query form(final String query) {
		return json(Json.createObjectBuilder(search(query).entrySet().stream()

				.collect(toMap(Map.Entry::getKey, this::value))

		).build());
	}


	private Object value(final Map.Entry<String, List<String>> field) {

		final String key=field.getKey();
		final List<String> values=field.getValue();

		return key.equals(".terms") || key.equals(".stats") ? path(values)
				: key.equals(".offset") || key.equals(".limit") ? integer(values)
				: strings(values);

	}


	private Object path(final List<String> values) {
		return values.size() == 1 ? values.get(0) : strings(values);
	}

	private Object integer(final List<String> values) {
		if ( values.size() == 1 ) {

			try {

				return Long.parseLong(values.get(0));

			} catch ( final NumberFormatException e ) {

				return strings(values);

			}

		} else {

			return strings(values);

		}
	}

	private Object strings(final Collection<String> values) {

		final List<String> strings=values.stream()
				.flatMap(value -> Arrays.stream(value.split(",")))
				.collect(toList());

		return strings.size() == 1 ? strings.get(0) : strings;
	}


	//// Query Properties /////////////////////////////////////////////////////////////////////////////////////////////

	private Shape filter(final JsonObject query) {
		return and(query.entrySet().stream()

				.filter(entry -> !entry.getKey().startsWith(".")) // ignore reserved properties

				.filter(entry -> !entry.getValue().equals(NULL))
				.filter(entry -> !entry.getValue().equals(EMPTY_JSON_STRING))
				.filter(entry -> !entry.getValue().equals(EMPTY_JSON_ARRAY))

				.map(entry -> { // longest matches first

					final String key=entry.getKey();
					final JsonValue value=entry.getValue();

					return key.startsWith("^") ? filter(value, shape, key.substring(1), this::datatype)
							: key.startsWith("@") ? filter(value, shape, key.substring(1), this::clazz)
							: key.startsWith("%") ? filter(value, shape, key.substring(1), this::range)

							: key.startsWith(">=") ? filter(value, shape, key.substring(2), this::minInclusive)
							: key.startsWith("<=") ? filter(value, shape, key.substring(2), this::maxInclusive)
							: key.startsWith(">") ? filter(value, shape, key.substring(1), this::minExclusive)
							: key.startsWith("<") ? filter(value, shape, key.substring(1), this::maxExclusive)

							: key.startsWith("$>") ? filter(value, shape, key.substring(2), this::minLength)
							: key.startsWith("$<") ? filter(value, shape, key.substring(2), this::maxLength)
							: key.startsWith("*") ? filter(value, shape, key.substring(1), this::pattern)
							: key.startsWith("~") ? filter(value, shape, key.substring(1), this::like)
							: key.startsWith("'") ? filter(value, shape, key.substring(1), this::stem)

							: key.startsWith("#>") ? filter(value, shape, key.substring(2), this::minCount)
							: key.startsWith("#<") ? filter(value, shape, key.substring(2), this::maxCount)
							: key.startsWith("!") ? filter(value, shape, key.substring(1), this::all)
							: key.startsWith("?") ? filter(value, shape, key.substring(1), this::any)

							: filter(value, shape, key, this::any);

				})

				.collect(toList())
		);
	}


	private Shape filter(final JsonValue value, final Shape shape,
			final String path, final BiFunction<? super JsonValue, ? super Shape, Shape> mapper
	) {
		return filter(value, shape, path(path, shape), mapper);
	}

	private Shape filter(final JsonValue value, final Shape shape,
			final List<IRI> path, final BiFunction<? super JsonValue, ? super Shape, Shape> mapper
	) {

		return Optional

				.ofNullable(shape.map(new Shape.Probe<Shape>() {

					@Override public Shape probe(final Field field) {
						return path.isEmpty() || !field.iri().equals(path.get(0)) ? null : Optional
								.ofNullable(filter(value, field.shape(), path.subList(1, path.size()), mapper))
								.map(s -> field(field.label(), field.iri(), s))
								.orElse(null);
					}

					@Override public Shape probe(final Link link) {
						return Optional
								.ofNullable(filter(value, link.shape(), path, mapper))
								.map(s -> link(link.iri(), s))
								.orElse(null);
					}


					@Override public Shape probe(final When when) {
						throw new UnsupportedOperationException(when.toString());
					}

					@Override public Shape probe(final And and) {
						return probe(and.shapes().stream());
					}

					@Override public Shape probe(final Or or) {
						return probe(or.shapes().stream());
					}


					private Shape probe(final Stream<Shape> shapes) {
						return shapes.map(this).filter(Objects::nonNull).findFirst().orElse(null);
					}

				}))

				.orElseGet(() -> {

					if ( !path.isEmpty() ) {
						throw new NoSuchElementException(format("unknown path step %s", format(path.get(0))));
					}

					return mapper.apply(value, shape);

				});

	}


	private List<IRI> terms(final JsonObject query) {
		return Optional.ofNullable(query.get(".terms"))

				.filter(v -> !v.equals(NULL))

				.map(v -> v instanceof JsonString ? (JsonString)v : error(".terms is not a string"))
				.map(path -> path(path.getString(), shape))

				.orElse(null);
	}

	private List<IRI> stats(final JsonObject query) {
		return Optional.ofNullable(query.get(".stats"))

				.filter(v -> !v.equals(NULL))

				.map(v -> v instanceof JsonString ? (JsonString)v : error(".stats is not a string"))
				.map(path -> path(path.getString(), shape))

				.orElse(null);
	}


	private List<Order> order(final JsonObject query) {
		return Optional.ofNullable(query.get(".order"))

				.filter(v -> !v.equals(NULL))

				.map(this::order)

				.orElse(emptyList());
	}

	private List<Order> order(final JsonValue object) {
		return object instanceof JsonString ? criteria((JsonString)object)
				: object instanceof JsonArray ? criteria((JsonArray)object)
				: error(".order is neither a string nor an array of strings");
	}


	private List<Order> criteria(final JsonArray object) {
		return object.stream()
				.map(v -> v instanceof JsonString ? (JsonString)v : error(".order is not a string"))
				.map(this::criterion)
				.collect(toList());
	}

	private List<Order> criteria(final JsonString object) {
		return singletonList(criterion(object.getString()));
	}


	private Order criterion(final JsonString criterion) {
		return criterion(criterion.getString());
	}

	private Order criterion(final String criterion) {
		return criterion.startsWith("+") ? increasing(path(criterion.substring(1), shape))
				: criterion.startsWith("-") ? decreasing(path(criterion.substring(1), shape))
				: increasing(path(criterion, shape));
	}


	private int offset(final JsonObject query) {
		return Optional.ofNullable(query.get(".offset"))

				.filter(v -> !v.equals(NULL))

				.map(v -> {

					try {

						return v instanceof JsonNumber ? ((JsonNumber)v).intValue()
								: v instanceof JsonString ? Integer.parseInt(((JsonString)v).getString())
								: error(".offset is not a number");

					} catch ( final NumberFormatException e ) {
						return error(".offset is not a number");
					}

				})

				.map(v -> v >= 0 ? v : error("negative offset"))

				.orElse(0);
	}

	private int limit(final JsonObject query) {
		return Optional.ofNullable(query.get(".limit"))

				.filter(v -> !v.equals(NULL))

				.map(v -> {

					try {

						return v instanceof JsonNumber ? ((JsonNumber)v).intValue()
								: v instanceof JsonString ? Integer.parseInt(((JsonString)v).getString())
								: error(".limit is not a number");

					} catch ( final NumberFormatException e ) {
						return error(".limit is not a number");
					}

				})

				.map(v -> v >= 0 ? v : error("negative limit"))

				.orElse(0);
	}


	//// Paths /////////////////////////////////////////////////////////////////////////////////////////////////////////

	private List<IRI> path(final String path, final Shape shape) {

		if ( path.startsWith(".") ) {
			throw new JsonException(format("reserved path <%s>", path));
		}

		final List<String> steps=new ArrayList<>();

		final String trimmed=path.trim();
		final Matcher matcher=StepPattern.matcher(trimmed);

		int last=0;

		while ( matcher.lookingAt() ) {
			steps.add(matcher.group(1));
			matcher.region(last=matcher.end(), trimmed.length());
		}

		if ( last != trimmed.length() ) {
			throw new JsonException(format("malformed path <%s>", trimmed));
		}

		return path(steps, shape).collect(toList());
	}

	private Stream<IRI> path(final List<String> steps, final Shape shape) {
		if ( steps.isEmpty() ) { return Stream.empty(); } else {

			final String head=steps.get(0);
			final List<String> tail=steps.subList(1, steps.size());

			final Map<String, Field> labels=labels(shape, keywords);

			return Optional.ofNullable(labels.get(head))

					.map(field -> Stream.concat(Stream.of(field.iri()), path(tail, field.shape())))

					.orElseThrow(() -> new NoSuchElementException(format("unknown path step <%s>", head)));
		}
	}


	//// Values ////////////////////////////////////////////////////////////////////////////////////////////////////////

	private List<Value> values(final JsonValue value, final Shape shape) {
		return decoder.values(value, shape).map(Map.Entry::getKey).collect(toList());
	}

	private Value value(final JsonValue value, final Shape shape) {
		return decoder.value(value, shape).getKey();
	}


	//// Shapes ////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shape datatype(final JsonValue value, final Shape shape) {
		return value instanceof JsonString
				? Datatype.datatype(iri(((JsonString)value).getString()))
				: error("datatype value is not a string");
	}

	private Shape clazz(final JsonValue value, final Shape shape) {
		return value instanceof JsonString
				? Clazz.clazz(iri(((JsonString)value).getString()))
				: error("class value is not a string");
	}

	private Shape range(final JsonValue value, final Shape shape) {
		return value.getValueType() == JsonValue.ValueType.NULL
				? error("value is null")
				: Range.range(values(value, shape)
		);
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


	private Shape minLength(final JsonValue value, final Shape shape) {
		return value instanceof JsonNumber
				? MinLength.minLength(((JsonNumber)value).intValue())
				: error("length is not a number");
	}

	private Shape maxLength(final JsonValue value, final Shape shape) {
		return value instanceof JsonNumber
				? MaxLength.maxLength(((JsonNumber)value).intValue())
				: error("length is not a number");
	}

	private Shape pattern(final JsonValue value, final Shape shape) {
		return value instanceof JsonString
				? ((JsonString)value).getString().isEmpty() ? and() : Pattern.pattern(((JsonString)value).getString())
				: error("pattern is not a string");
	}

	private Shape like(final JsonValue value, final Shape shape) {
		return value instanceof JsonString
				? ((JsonString)value).getString().isEmpty() ? and() : Like.like(((JsonString)value).getString(), true)
				: error("pattern is not a string");
	}

	private Shape stem(final JsonValue value, final Shape shape) {
		return value instanceof JsonString
				? ((JsonString)value).getString().isEmpty() ? and() : Stem.stem(((JsonString)value).getString())
				: error("pattern is not a string");
	}


	private Shape minCount(final JsonValue value, final Shape shape) {
		return value instanceof JsonNumber
				? MinCount.minCount(((JsonNumber)value).intValue())
				: error("length is not a number");
	}

	private Shape maxCount(final JsonValue value, final Shape shape) {
		return value instanceof JsonNumber
				? MaxCount.maxCount(((JsonNumber)value).intValue())
				: error("length is not a number");
	}

	private Shape all(final JsonValue value, final Shape shape) {
		return value.getValueType() == JsonValue.ValueType.NULL
				? error("value is null")
				: All.all(values(value, shape)
		);
	}

	private Shape any(final JsonValue value, final Shape shape) {
		return value.getValueType() == JsonValue.ValueType.NULL
				? error("value is null")
				: Any.any(values(value, shape));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private <V> V error(final String message) {
		throw new JsonException(message);
	}

}
