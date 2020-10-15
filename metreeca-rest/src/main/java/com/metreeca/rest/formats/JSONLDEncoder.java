/*
 * Copyright © 2013-2020 Metreeca srl
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
import com.metreeca.json.shapes.Field;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import javax.json.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;

import static com.metreeca.json.Values.*;
import static com.metreeca.rest.formats.JSONLDCodec.*;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toCollection;


final class JSONLDEncoder {

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

		final Map<String, String> keywords2aliases=keywords;

		this.aliaser=keyword -> keywords2aliases.getOrDefault(keyword, keyword);
	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	JsonObject encode(final Collection<Statement> model) {

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		return resource(focus, shape, model, resource -> false).asJsonObject();
	}

	JsonValue encode(final Value value) {
		return value(value, shape, emptySet(), resource -> true);
	}


	//// Values ///////////////////////////////////////////////////////////////////////////////////////////////////////

	private JsonValue values(
			final Collection<? extends Value> values, final Shape shape,
			final Collection<Statement> model, final Predicate<Resource> trail
	) {

		if ( maxCount(shape).map(limit -> limit == 1).orElse(false) ) { // single subject

			return values.isEmpty()
					? JsonValue.EMPTY_JSON_OBJECT
					: value(values.iterator().next(), shape, model, trail);

		} else { // multiple subjects

			final JsonArrayBuilder array=Json.createArrayBuilder();

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
				: null;

	}


	//// Resources ////////////////////////////////////////////////////////////////////////////////////////////////////

	private JsonValue resource(
			final Resource resource, final Shape shape,
			final Collection<Statement> model, final Predicate<Resource> trail
	) { // !!! refactor

		final Object datatype=datatype(shape).orElse(null);
		final Map<String, Field> fields=fields(shape, keywords);

		final boolean inlineable=IRIType.equals(datatype)
				|| BNodeType.equals(datatype)
				|| ResourceType.equals(datatype);

		final String id=id(resource);

		if ( trail.test(resource) ) { // a back-reference to an enclosing copy of self -> omit fields

			return inlineable
					? Json.createValue(id)
					: Json.createObjectBuilder().add(aliaser.apply("@id"), id).build();

		} else if ( inlineable && resource instanceof IRI && fields.isEmpty() ) { // inline proved leaf IRI

			return Json.createValue(id);

		} else {

			final JsonObjectBuilder object=Json.createObjectBuilder().add(aliaser.apply("@id"), id);

			final Collection<Resource> references=new ArrayList<>();

			final Predicate<Resource> nestedTrail=reference -> {

				if ( reference.equals(resource) ) {
					references.add(reference); // mark resource as back-referenced
				}

				return reference.equals(resource) || trail.test(reference);

			};


			for (final Map.Entry<String, Field> entry : fields.entrySet()) {

				final String alias=entry.getKey();
				final Field field=entry.getValue();

				final IRI predicate=field.name();
				final Shape nestedShape=field.shape();

				final boolean direct=direct(predicate);

				final Collection<? extends Value> values=direct
						? objects(model, resource, predicate)
						: subjects(model, resource, inverse(predicate));

				if ( !values.isEmpty() ) { // omit null value and empty arrays

					object.add(alias, values(values, nestedShape, model, nestedTrail));

				}

			}

			if ( resource instanceof BNode && references.isEmpty() ) { // no back-references > drop id
				object.remove(aliaser.apply("@id"));
			}

			if ( context ) {
				context(resource.equals(focus) ? keywords : emptyMap(), fields).ifPresent(context ->
						object.add("@context", context)
				);
			}

			return object.build();

		}

	}

	private Optional<JsonObject> context(final Map<String, String> keywords, final Map<String, Field> fields) {
		if ( keywords.isEmpty() && fields.isEmpty() ) { return Optional.empty(); } else {

			final JsonObjectBuilder context=Json.createObjectBuilder();

			keywords.forEach((keyword, alias) ->

					context.add(alias, keyword)

			);

			fields.forEach((alias, field) -> {

				final IRI iri=field.name();
				final String value=iri.stringValue();

				context.add(alias, direct(iri)
						? Json.createValue(value)
						: Json.createObjectBuilder().add("@reverse", value).build()
				);

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
					: datatype(shape).isPresent() ? literal(literal.stringValue()) // only lexical if type is known
					: literal(literal, datatype);

		} catch ( final IllegalArgumentException ignored ) { // malformed literals
			return literal(literal, datatype);
		}
	}


	private JsonValue literal(final boolean value) {
		return value ? JsonValue.TRUE : JsonValue.FALSE;
	}

	private JsonValue literal(final String value) {
		return Json.createValue(value);
	}

	private JsonValue literal(final BigInteger value) {
		return Json.createValue(value);
	}

	private JsonValue literal(final BigDecimal value) {
		return Json.createValue(value);
	}


	private JsonValue literal(final Value literal, final String lang) {
		return Json.createObjectBuilder()
				.add(aliaser.apply("@value"), literal.stringValue())
				.add(aliaser.apply("@language"), lang)
				.build();
	}

	private JsonValue literal(final Value literal, final IRI datatype) {
		return Json.createObjectBuilder()
				.add(aliaser.apply("@value"), literal.stringValue())
				.add(aliaser.apply("@type"), datatype.stringValue())
				.build();
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

}
