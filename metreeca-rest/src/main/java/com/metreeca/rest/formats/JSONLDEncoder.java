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

package com.metreeca.rest.formats;

import com.metreeca.json.Shape;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;

import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.fields;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.Meta.aliases;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;


final class JSONLDEncoder extends JSONLDCodec {

	private static final JsonWriterFactory JsonWriters=Json
			.createWriterFactory(singletonMap(JsonGenerator.PRETTY_PRINTING, true));


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final IRI focus;
	private final Shape shape;

	private final String root;

	private final Predicate<String> aliased;
	private final Function<String, String> aliaser;


	JSONLDEncoder(final IRI focus, final Shape shape) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		this.focus=focus;
		this.shape=driver(shape);

		this.root=Optional.of(focus.stringValue())
				.map(IRIPattern::matcher)
				.filter(Matcher::matches)
				.map(matcher -> matcher.group("schemeall")+matcher.group("hostall")+"/")
				.orElse("/");

		final Map<String, String> keywords2aliases=keywords(shape)

				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (x, y) -> {

					if ( !x.equals(y) ) {
						throw new IllegalArgumentException("conflicting aliases for JSON-LD keywords");
					}

					return x;

				}));

		this.aliased=keywords2aliases::containsValue;
		this.aliaser=keyword -> keywords2aliases.getOrDefault(keyword, keyword);
	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	<W extends Writer> W encode(final W writer, final Collection<Statement> model) {

		if ( writer == null ) {
			throw new NullPointerException("null writer");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		try ( final JsonWriter jsonWriter=JsonWriters.createWriter(writer) ) {

			jsonWriter.writeObject(encode(model));

			return writer;

		}
	}

	JsonObject encode(final Collection<Statement> model) {

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		return json(model, shape, focus, resource -> false).asJsonObject();
	}


	//// Values ///////////////////////////////////////////////////////////////////////////////////////////////////////

	private JsonValue json(final Collection<Statement> model,
			final Shape shape, final Collection<? extends Value> values, final Predicate<Resource> trail) {

		if ( maxCount(shape).map(limit -> limit == 1).orElse(false) ) { // single subject

			return values.isEmpty()
					? JsonValue.EMPTY_JSON_OBJECT
					: json(model, shape, values.iterator().next(), trail);

		} else { // multiple subjects

			final JsonArrayBuilder array=Json.createArrayBuilder();

			values.stream().map(value -> json(model, shape, value, trail)).forEach(array::add);

			return array.build();

		}

	}

	private JsonValue json(final Collection<Statement> model,
			final Shape shape, final Value value, final Predicate<Resource> trail) {

		return value instanceof Resource ? json(model, shape, (Resource)value, trail)
				: value instanceof Literal ? json((Literal)value, shape)
				: null;

	}


	//// Resources ////////////////////////////////////////////////////////////////////////////////////////////////////

	private JsonValue json(final Collection<Statement> model,
			final Shape shape, final Resource resource, final Predicate<Resource> trail) { // !!! refactor

		final Object datatype=datatype(shape).orElse(null);
		final Map<IRI, Shape> fields=fields(shape);

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

			final Map<IRI, String> aliases=aliases(shape);

			for (final Map.Entry<IRI, Shape> entry : fields.entrySet()) {

				final IRI predicate=entry.getKey();
				final boolean direct=direct(predicate);

				final Shape nestedShape=entry.getValue();

				final String alias=Optional
						.ofNullable(aliases.get(predicate))
						.filter(aliased.negate()) // keyword aliases override field aliases
						.orElseGet(() -> (direct ? "" : "^")+predicate.stringValue()); // !!! inverse?

				final Collection<? extends Value> values=direct
						? objects(model, resource, predicate)
						: subjects(model, resource, inverse(predicate));

				if ( !values.isEmpty() ) { // omit null value and empty arrays

					object.add(alias, json(model, nestedShape, values, nestedTrail));

				}

			}

			if ( resource instanceof BNode && references.isEmpty() ) { // no back-references > drop id
				object.remove(aliaser.apply("@id"));
			}

			return object.build();

		}

	}


	//// Literals /////////////////////////////////////////////////////////////////////////////////////////////////////

	private JsonValue json(final Literal literal, final Shape shape) {

		final IRI datatype=literal.getDatatype();

		try {

			return datatype.equals(XSD.BOOLEAN) ? json(literal.booleanValue())
					: datatype.equals(XSD.STRING) ? json(literal.stringValue())
					: datatype.equals(XSD.INTEGER) ? json(literal.integerValue())
					: datatype.equals(XSD.DECIMAL) ? json(literal.decimalValue())
					: datatype.equals(RDF.LANGSTRING) ? json(literal, literal.getLanguage().orElse(""))
					: datatype(shape).isPresent() ? json(literal.stringValue()) // only lexical value if type is known
					: json(literal, datatype);

		} catch ( final IllegalArgumentException ignored ) { // malformed literals
			return json(literal, datatype);
		}
	}


	private JsonValue json(final boolean value) {
		return value ? JsonValue.TRUE : JsonValue.FALSE;
	}

	private JsonValue json(final String value) {
		return Json.createValue(value);
	}

	private JsonValue json(final BigInteger value) {
		return Json.createValue(value);
	}

	private JsonValue json(final BigDecimal value) {
		return Json.createValue(value);
	}


	private JsonValue json(final Value literal, final String lang) {
		return Json.createObjectBuilder()
				.add(aliaser.apply("@value"), literal.stringValue())
				.add(aliaser.apply("@language"), lang)
				.build();
	}

	private JsonValue json(final Value literal, final Value datatype) {
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
