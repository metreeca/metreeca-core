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
import com.metreeca.json.shapes.*;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import javax.json.*;

import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.Field.aliases;
import static com.metreeca.rest.formats.JSONLDInspector.driver;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.*;

import static javax.json.Json.*;


/**
 * Shape-driven RDF to JSON-LD encoder.
 *
 * <p>Converts RDF models to strictly compacted/framed JSON-LD descriptions.</p>
 */
final class JSONLDEncoder {

	private static final Collection<IRI> InternalTypes=new HashSet<>(asList(ValueType, ResourceType, LiteralType));


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final IRI focus;
	private final Shape shape;

	private final Map<String, String> keywords;
	private final boolean context;

	private final String root;

	private final Function<String, String> aliaser;


	JSONLDEncoder(final IRI focus, final Shape shape, final Map<String, String> keywords, final boolean context) {

		this.focus=focus;
		this.shape=driver(shape);

		this.keywords=keywords;
		this.context=context;

		this.root=Optional.of(focus.stringValue())
				.map(IRIPattern::matcher)
				.filter(Matcher::matches)
				.map(matcher -> Optional.ofNullable(matcher.group("schemeall")).orElse("")
						+Optional.ofNullable(matcher.group("hostall")).orElse("")
						+"/"
				)
				.orElse("/");

		this.aliaser=keyword -> keywords.getOrDefault(keyword, keyword);
	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	JsonObject encode(final Collection<Statement> model) {

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		return resource(focus, shape, model, resource -> false).asJsonObject();
	}


	//// Values ///////////////////////////////////////////////////////////////////////////////////////////////////////

	private JsonValue values(
			final Collection<? extends Value> values, final Shape shape,
			final Collection<Statement> model, final Predicate<Resource> trail
	) {

		final int maxCount=Optional.ofNullable(shape.map(new MaxCountProbe())).orElse(Integer.MAX_VALUE);

		if ( JSONLDInspector.tagged(shape) ) { // tagged literals

			return taggeds(values, shape, maxCount == 1);

		} else if ( maxCount == 1 ) { // single value

			return value(values.iterator().next(), shape, model, trail); // values required to be not empty

		} else { // multiple values

			final JsonArrayBuilder array=createArrayBuilder();

			values.stream().map(value -> value(value, shape, model, trail)).forEach(array::add);

			return array.build();

		}

	}

	private JsonValue value(
			final Value value, final Shape shape,
			final Collection<Statement> model, final Predicate<Resource> trail
	) {

		return value instanceof Resource ? resource((Resource)value, shape, model, trail)
				: value instanceof Literal ? literal((Literal)value, shape)
				: null; // unexpected

	}


	//// Resources ////////////////////////////////////////////////////////////////////////////////////////////////////

	private JsonValue resource(
			final Resource resource, final Shape shape,
			final Collection<Statement> model, final Predicate<Resource> trail
	) { // !!! refactor

		final Object datatype=JSONLDInspector.datatype(shape).orElse(null);
		final Map<String, Field> aliases=aliases(shape, keywords);

		final boolean inlineable=IRIType.equals(datatype)
				|| BNodeType.equals(datatype)
				|| ResourceType.equals(datatype);

		final String id=id(resource);

		if ( trail.test(resource) ) { // a back-reference to an enclosing copy of self -> omit fields

			return inlineable
					? createValue(id)
					: createObjectBuilder().add(aliaser.apply("@id"), id).build();

		} else if ( inlineable && resource instanceof IRI && aliases.isEmpty() ) { // inline proved leaf IRI

			return createValue(id);

		} else {

			final JsonObjectBuilder object=createObjectBuilder().add(aliaser.apply("@id"), id);

			final Collection<Resource> references=new ArrayList<>();

			final Predicate<Resource> nestedTrail=reference -> {

				if ( reference.equals(resource) ) {
					references.add(reference); // mark resource as back-referenced
				}

				return reference.equals(resource) || trail.test(reference);

			};


			for (final Map.Entry<String, Field> entry : aliases.entrySet()) {

				final String alias=entry.getKey();
				final Field field=entry.getValue();

				final IRI predicate=field.name();
				final Shape nestedShape=field.shape();

				final boolean direct=field.direct();

				final Collection<? extends Value> values=direct
						? objects(model, resource, predicate)
						: subjects(model, resource, predicate);

				if ( !values.isEmpty() ) { // omit null value and empty arrays

					object.add(alias, values(values, nestedShape, model, nestedTrail));

				}

			}

			if ( resource instanceof BNode && references.isEmpty() ) { // no back-references > drop id
				object.remove(aliaser.apply("@id"));
			}

			if ( context ) {
				context(resource.equals(focus) ? keywords : emptyMap(), aliases).ifPresent(context ->
						object.add("@context", context)
				);
			}

			return object.build();

		}

	}

	private Optional<JsonObject> context(final Map<String, String> keywords, final Map<String, Field> fields) {
		if ( keywords.isEmpty() && fields.isEmpty() ) { return Optional.empty(); } else {

			final JsonObjectBuilder context=createObjectBuilder();

			keywords.forEach((keyword, alias) ->

					context.add(alias, keyword)

			);

			fields.forEach((alias, field) -> {

				final IRI name=field.name();
				final Shape shape=field.shape();

				final boolean direct=field.direct();
				final String iri=name.stringValue();

				final Optional<IRI> datatype=JSONLDInspector.datatype(shape);

				if ( datatype.filter(IRIType::equals).isPresent() ) {

					context.add(alias, createObjectBuilder()
							.add(direct ? "@id" : "@reverse", iri)
							.add("@type", "@id")
					);

				} else if ( datatype.filter(RDF.LANGSTRING::equals).isPresent() ) {

					final Set<String> langs=JSONLDInspector.langs(shape).orElseGet(Collections::emptySet);

					context.add(alias, langs.size() == 1

							? createObjectBuilder()
							.add(direct ? "@id" : "@reverse", iri)
							.add("@language", langs.iterator().next())

							: createObjectBuilder()
							.add(direct ? "@id" : "@reverse", iri)
							.add("@container", "@language")
					);

				} else if ( datatype.filter(type -> !InternalTypes.contains(type)).isPresent() ) {

					context.add(alias, createObjectBuilder()
							.add(direct ? "@id" : "@reverse", iri)
							.add("@type", datatype.get().stringValue())
					);

				} else if ( direct ) {

					context.add(alias, iri);

				} else {

					context.add(alias, createObjectBuilder()
							.add("@reverse", iri)
					);

				}

			});

			return Optional.of(context.build());

		}
	}


	//// Literals /////////////////////////////////////////////////////////////////////////////////////////////////////

	private JsonValue literal(final Literal literal, final Shape shape) {

		final IRI datatype=literal.getDatatype();

		try {

			return datatype.equals(XSD.BOOLEAN) ? literal(literal.booleanValue())
					: datatype.equals(XSD.STRING) ? literal(literal.stringValue())
					: datatype.equals(XSD.INTEGER) ? literal(literal.integerValue())
					: datatype.equals(XSD.DECIMAL) ? literal(literal.decimalValue())
					: datatype.equals(RDF.LANGSTRING) ? literal(literal, literal.getLanguage().orElse(""))
					: JSONLDInspector.datatype(shape).isPresent() ? literal(literal.stringValue()) // only lexical if
					// type is known
					: literal(literal, datatype);

		} catch ( final IllegalArgumentException ignored ) { // malformed literals
			return literal(literal, datatype);
		}
	}


	private JsonValue literal(final boolean value) {
		return value ? JsonValue.TRUE : JsonValue.FALSE;
	}

	private JsonValue literal(final String value) {
		return createValue(value);
	}

	private JsonValue literal(final BigInteger value) {
		return createValue(value);
	}

	private JsonValue literal(final BigDecimal value) {
		return createValue(value);
	}


	private JsonValue literal(final Value literal, final String lang) {
		return createObjectBuilder()
				.add(aliaser.apply("@value"), literal.stringValue())
				.add(aliaser.apply("@language"), lang)
				.build();
	}

	private JsonValue literal(final Value literal, final IRI datatype) {
		return createObjectBuilder()
				.add(aliaser.apply("@value"), literal.stringValue())
				.add(aliaser.apply("@type"), datatype.stringValue())
				.build();
	}


	//// Tagged Literals //////////////////////////////////////////////////////////////////////////////////////////////

	private JsonValue taggeds(final Collection<? extends Value> values, final Shape shape, final boolean scalar) {

		// !!! refactor

		final boolean localized=JSONLDInspector.localized(shape);
		final Set<String> langs=JSONLDInspector.langs(shape).orElseGet(Collections::emptySet);

		final Map<String, List<String>> langToStrings=values.stream()
				.map(Literal.class::cast) // datatype already checked by values()
				.collect(groupingBy(
						literal -> literal.getLanguage().orElse(""), // datatype already checked by values()
						LinkedHashMap::new,
						mapping(Value::stringValue, toList())
				));

		if ( langs.size() == 1 ) { // known language

			final List<String> strings=langToStrings.values().iterator().next(); // values required to be non empty

			if ( localized || scalar ) { // single value

				return createValue(strings.get(0));

			} else { // multiple values

				return createArrayBuilder(strings).build();

			}

		} else { // multiple languages

			final JsonObjectBuilder builder=createObjectBuilder();

			langToStrings.forEach((lang, strings) -> {

				if ( localized || scalar ) { // single value

					builder.add(lang, createValue(strings.get(0)));

				} else { // multiple values

					builder.add(lang, createArrayBuilder(strings));

				}

			});

			return builder.build();

		}

	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String id(final Resource resource) {
		return resource instanceof BNode ? "_:"+resource.stringValue() : relativize(resource.stringValue());
	}

	private String relativize(final String iri) {
		return iri.startsWith(root) ? iri.substring(root.length()-1) : iri;
	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Set<Resource> subjects(final Collection<Statement> model, final Value resource, final Value predicate) {
		return model.stream()
				.filter(pattern(null, predicate, resource))
				.map(Statement::getSubject)
				.collect(toCollection(LinkedHashSet::new));
	}

	private Set<Value> objects(final Collection<Statement> model, final Value resource, final Value predicate) {
		return model.stream()
				.filter(pattern(resource, predicate, null))
				.map(Statement::getObject)
				.collect(toCollection(LinkedHashSet::new));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class MaxCountProbe extends Shape.Probe<Integer> {

		@Override public Integer probe(final MaxCount maxCount) {
			return maxCount.limit();
		}

		@Override public Integer probe(final And and) {
			return reduce(and.shapes().stream(), Math::min);
		}

		@Override public Integer probe(final Or or) {
			return reduce(or.shapes().stream(), Math::max);
		}

		@Override public Integer probe(final When when) {
			return reduce(Stream.of(when.pass(), when.fail()), Math::max);
		}


		private Integer reduce(final Stream<Shape> shapeStream, final BinaryOperator<Integer> operator) {
			return shapeStream
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.reduce(operator)
					.orElse(null);
		}

	}

}
