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

import com.metreeca.json.formats.JSONFormat;
import com.metreeca.rdf.Values;
import com.metreeca.tree.Shape;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import javax.json.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;

import static com.metreeca.json.formats.JSONFormat.aliaser;
import static com.metreeca.rdf.Values.*;
import static com.metreeca.rdf.formats.RDFFormat.iri;
import static com.metreeca.rdf.formats.RDFJSONCodec.aliases;
import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Field.fields;
import static com.metreeca.tree.shapes.MaxCount.maxCount;
import static java.util.stream.Collectors.toCollection;


abstract class RDFJSONEncoder {

	private final String base;
	private final Function<String, String> aliaser;


	RDFJSONEncoder(final CharSequence base, final JsonObject context) {
		this.base=root(base);
		aliaser=aliaser(context);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected JsonValue json(final Collection<Statement> model, final Shape shape, final Resource focus) {
		return (focus != null)
				? json(model, shape, focus, resource -> false)
				: json(model, shape, subjects(model), resource -> false);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JsonValue json(final Collection<Statement> model,
			final Shape shape, final Collection<? extends Value> values, final Predicate<Resource> trail) {
		if ( shape != null && maxCount(shape).map(limit -> limit == 1).orElse(false) ) { // single subject

			return values.isEmpty() ? JsonValue.EMPTY_JSON_OBJECT : json(model, shape, values.iterator().next(), trail);

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

	private JsonValue json(final Collection<Statement> model,
			final Shape shape, final Resource resource, final Predicate<Resource> trail) { // !!! refactor

		final Object datatype=datatype(shape).orElse(null);
		final Map<Object, Shape> fields=fields(shape);

		final boolean inlineable=
				IRIType.equals(datatype) || BNodeType.equals(datatype) || ResourceType.equals(datatype);


		final String id=id(resource);

		if ( trail.test(resource) ) { // a back-reference to an enclosing copy of self -> omit fields

			return inlineable
					? Json.createValue(id)
					: Json.createObjectBuilder().add(aliaser.apply(JSONFormat.id), id).build();

		} else if ( inlineable && resource instanceof IRI && fields.isEmpty() ) { // inline proved leaf IRI

			return Json.createValue(id);

		} else {

			final JsonObjectBuilder object=Json.createObjectBuilder().add(aliaser.apply(JSONFormat.id), id);

			final Collection<Resource> references=new ArrayList<>();

			final Predicate<Resource> nestedTrail=reference -> {

				if ( reference.equals(resource) ) {
					references.add(reference); // mark resource as back-referenced
				}

				return reference.equals(resource) || trail.test(reference);

			};

			if ( shape == null ) { // write all direct fields

				for (final IRI predicate : predicates(model, resource)) {
					object.add(predicate.stringValue(),
							json(model, null, objects(model, resource, predicate), nestedTrail)
					);
				}

			} else { // write direct/inverse fields as specified by the shape

				final Map<IRI, String> aliases=aliases(shape);

				for (final Map.Entry<Object, Shape> entry : fields.entrySet()) {

					final IRI predicate=iri(entry.getKey());
					final boolean direct=direct(predicate);

					final Shape nestedShape=entry.getValue();

					final String alias=Optional.ofNullable(aliases.get(predicate))
							.orElseGet(() -> (direct ? "" : "^")+predicate.stringValue());

					final Collection<? extends Value> values=direct
							? objects(model, resource, predicate)
							: subjects(model, resource, inverse(predicate));

					if ( !values.isEmpty() ) { // omit null value and empty arrays

						object.add(alias, json(model, nestedShape, values, nestedTrail));

					}

				}

			}

			if ( resource instanceof BNode && references.isEmpty() ) {
				object.remove(aliaser.apply(JSONFormat.id)); // drop id field for blank nodes without back-references
			}

			return object.build();

		}
	}


	private JsonValue json(final Literal literal, final Shape shape) {

		final IRI datatype=literal.getDatatype();

		try {

			return datatype.equals(XMLSchema.BOOLEAN) ? json(literal.booleanValue())
					: datatype.equals(XMLSchema.STRING) ? json(literal.stringValue())
					: datatype.equals(XMLSchema.INTEGER) ? json(literal.integerValue())
					: datatype.equals(XMLSchema.DECIMAL) ? json(literal.decimalValue())
					: datatype.equals(RDF.LANGSTRING) ? json(literal, literal.getLanguage().orElse(""))
					: datatype(shape).isPresent() ? Json.createValue(literal.stringValue()) // only lexical value if
					// type is known
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
				.add(aliaser.apply(JSONFormat.value), literal.stringValue())
				.add(aliaser.apply(JSONFormat.language), lang)
				.build();
	}

	private JsonValue json(final Value literal, final Value datatype) {
		return Json.createObjectBuilder()
				.add(aliaser.apply(JSONFormat.value), literal.stringValue())
				.add(aliaser.apply(JSONFormat.type), datatype.stringValue())
				.build();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String id(final Resource resource) {
		return resource instanceof BNode ? "_:"+resource.stringValue() : relativize(resource.stringValue());
	}


	private String root(final CharSequence base) {
		if ( base == null ) { return null; } else {

			final Matcher matcher=Values.IRIPattern.matcher(base);

			return matcher.matches() ? matcher.group("schemeall")+matcher.group("hostall")+"/" : null;

		}
	}

	private String relativize(final String iri) {
		return base != null && iri.startsWith(base) ? iri.substring(base.length()-1) : iri;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Set<Resource> subjects(final Collection<Statement> model) {
		return model.stream()
				.map(Statement::getSubject)
				.collect(toCollection(LinkedHashSet::new));
	}

	private Set<IRI> predicates(final Collection<Statement> model, final Value resource) {
		return model.stream()
				.filter(pattern(resource, null, null))
				.map(Statement::getPredicate)
				.collect(toCollection(LinkedHashSet::new));
	}

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
