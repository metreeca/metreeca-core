/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.form.things;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.*;
import java.util.regex.Pattern;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.UUID.nameUUIDFromBytes;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;


/**
 * RDF utilities.
 */
public final class Values {

	/**
	 * A pattern matching absolute IRIs.
	 */
	public static final Pattern AbsoluteIRIPattern=Pattern.compile("^\\w+:\\S+$");


	private static final ValueFactory factory=SimpleValueFactory.getInstance(); // before constant initialization

	private static final Comparator<Value> comparator=new ValueComparator();


	public static int compare(final Value x, final Value y) {
		return comparator.compare(x, y);
	}


	private static DecimalFormat exponential() { // ;( DecimalFormat is not thread-safe
		return new DecimalFormat("0.0#########E0", DecimalFormatSymbols.getInstance(Locale.ROOT));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * Internal namespace for local references and predicates (<code>{@value}</code>).
	 */
	public static final String Internal="app://local/";


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final IRI ValueType=iri(SESAME.NAMESPACE, "value"); // abstract datatype IRI for values
	public static final IRI ResoureType=iri(SESAME.NAMESPACE, "resource"); // abstract datatype IRI for resources
	public static final IRI LiteralType=iri(SESAME.NAMESPACE, "literal"); // abstract datatype IRI for literals

	public static final IRI BNodeType=iri(SESAME.NAMESPACE, "bnode"); // datatype IRI for blank nodes
	public static final IRI IRIType=iri(SESAME.NAMESPACE, "iri"); // datatype IRI for IRI references

	public static final Literal True=literal(true);
	public static final Literal False=literal(false);


	//// Accessors /////////////////////////////////////////////////////////////////////////////////////////////////////

	public static boolean is(final Value value, final IRI type) {
		return value != null && type(value).equals(type)
				|| value instanceof Resource && ResoureType.equals(type) // abstract resource datatype
				|| value instanceof Literal && LiteralType.equals(type) // abstract literal datatype
				|| value instanceof Literal && RDFS.LITERAL.equals(type); // abstract resource datatype using rdfs: IRI
	}


	public static String text(final Value value) {
		return value == null ? null : value.stringValue();
	}

	public static IRI type(final Value value) {
		return value == null ? null
				: value instanceof BNode ? BNodeType
				: value instanceof IRI ? IRIType
				: value instanceof Literal ? ((Literal)value).getDatatype()
				: null; // unexpected
	}

	public static String lang(final Value value) {
		return value instanceof Literal ? ((Literal)value).getLanguage().orElse(null) : null;
	}


	//// Factories /////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Namespace namespace(final String prefix, final String name) {

		if ( prefix == null ) {
			throw new NullPointerException("null prefix");
		}

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return new SimpleNamespace(prefix, name);
	}


	public static Statement statement(final Resource subject, final IRI predicate, final Value object) {
		return factory.createStatement(subject, predicate, object);
	}

	public static Statement statement(final Resource subject, final IRI predicate, final Value object, final Resource context) {
		return factory.createStatement(subject, predicate, object, context);
	}


	public static BigInteger integer(final long value) {
		return BigInteger.valueOf(value);
	}

	public static BigInteger integer(final Number value) {
		return value == null ? null
				: value instanceof BigInteger ? (BigInteger)value
				: value instanceof BigDecimal ? ((BigDecimal)value).toBigInteger()
				: BigInteger.valueOf(value.longValue());
	}

	public static BigDecimal decimal(final double value) {
		return BigDecimal.valueOf(value);
	}

	public static BigDecimal decimal(final Number value) {
		return value == null ? null
				: value instanceof BigInteger ? new BigDecimal((BigInteger)value)
				: value instanceof BigDecimal ? (BigDecimal)value
				: BigDecimal.valueOf(value.doubleValue());
	}


	public static BNode bnode() {
		return factory.createBNode();
	}

	public static BNode bnode(final String id) {

		if ( id == null ) {
			throw new NullPointerException("null id");
		}

		return factory.createBNode(id);
	}


	public static IRI iri() {
		return factory.createIRI("urn:uuid:", randomUUID().toString());
	}

	public static IRI iri(final String iri) {

		if ( iri == null ) {
			throw new NullPointerException("null iri");
		}

		return factory.createIRI(iri);
	}

	public static IRI iri(final String space, final String name) {

		if ( space == null ) {
			throw new NullPointerException("null space");
		}

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return factory.createIRI(space, space.endsWith("/") && name.startsWith("/") ? name.substring(1) : name);
	}


	public static IRI internal(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return iri(Internal, name);
	}


	public static Literal literal(final Object value) {
		return value == null ? null

				: value instanceof Boolean ? literal(((Boolean)value).booleanValue())

				: value instanceof Byte ? literal(((Byte)value).byteValue())
				: value instanceof Short ? literal(((Short)value).shortValue())
				: value instanceof Integer ? literal(((Integer)value).intValue())
				: value instanceof Long ? literal(((Long)value).longValue())
				: value instanceof Float ? literal(((Float)value).floatValue())
				: value instanceof Double ? literal(((Double)value).doubleValue())
				: value instanceof BigInteger ? literal((BigInteger)value)
				: value instanceof BigDecimal ? literal((BigDecimal)value)

				: value instanceof Date ? literal((Date)value)
				: value instanceof LocalDate ? literal((LocalDate)value)
				: value instanceof LocalDateTime ? literal((LocalDateTime)value)
				: value instanceof OffsetDateTime ? literal((OffsetDateTime)value)


				: value instanceof byte[] ? literal((byte[])value)

				: literal(value.toString());
	}

	public static Literal literal(final boolean value) {
		return factory.createLiteral(value);
	}

	public static Literal literal(final byte value) {
		return factory.createLiteral(value);
	}

	public static Literal literal(final short value) {
		return factory.createLiteral(value);
	}

	public static Literal literal(final int value) {
		return factory.createLiteral(value);
	}

	public static Literal literal(final long value) {
		return factory.createLiteral(value);
	}

	public static Literal literal(final float value) {
		return factory.createLiteral(value);
	}

	public static Literal literal(final double value) {
		return factory.createLiteral(value);
	}

	public static Literal literal(final BigInteger value) {
		return value == null ? null : factory.createLiteral(value);
	}

	public static Literal literal(final BigDecimal value) {
		return value == null ? null : factory.createLiteral(value);
	}

	public static Literal literal(final String value) {
		return value == null ? null : factory.createLiteral(value);
	}


	@Deprecated public static Literal literal(final Date value) {
		return value == null ? null : factory.createLiteral(value);
	}

	public static Literal literal(final LocalDate value) {
		return value == null ? null : literal(ISO_LOCAL_DATE_TIME.format(value.atStartOfDay()), XMLSchema.DATETIME);
	}

	public static Literal literal(final LocalDateTime value) {
		return value == null ? null : literal(ISO_LOCAL_DATE_TIME.format(value), XMLSchema.DATETIME);
	}

	public static Literal literal(final OffsetDateTime value) {
		return value == null ? null : literal(ISO_OFFSET_DATE_TIME.format(value), XMLSchema.DATETIME);
	}


	public static Literal literal(final byte[] value) {
		return value == null ? null : factory.createLiteral("data:application/octet-stream;base64,"
				+Base64.getEncoder().encodeToString(value), XMLSchema.ANYURI);
	}

	public static Literal literal(final String value, final String lang) {

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		if ( lang == null ) {
			throw new NullPointerException("null lang");
		}

		return factory.createLiteral(value, lang);
	}

	public static Literal literal(final String value, final IRI datatype) {

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		if ( datatype == null ) {
			throw new NullPointerException("null datatype");
		}

		return factory.createLiteral(value, datatype);
	}


	/**
	 * Creates a date-time literal for the current time.
	 *
	 * @return an xsd:dateTime literal representing the current system with second precision
	 */
	public static Literal time() {
		return time(false);
	}

	/**
	 * Creates a date-time literal for the current time.
	 *
	 * @param millis if {@code true}, milliseconds are included in the literal textual representation
	 *
	 * @return an {@code xsd:dateTime} literal representing the current system with second or millisecond precision as
	 * specified by {@code millis}
	 */
	public static Literal time(final boolean millis) {
		return time(Instant.now().toEpochMilli(), millis);
	}

	/**
	 * Creates a date-time literal for the current time.
	 *
	 * @param time the time to be converted represented as the number of milliseconds from the epoch of
	 *             1970-01-01T00:00:00Z
	 *
	 * @return an {@code xsd:dateTime} literal representing {@code time} with second precision
	 */
	public static Literal time(final long time) {
		return time(time, false);
	}

	/**
	 * Creates a date-time literal for a specific time.
	 *
	 * @param time   the time to be converted represented as the number of milliseconds from the epoch of
	 *               1970-01-01T00:00:00Z
	 * @param millis if {@code true}, includes milliseconds in the literal textual representation
	 *
	 * @return an {@code xsd:dateTime} literal representing {@code time} with second or millisecond precision as
	 * specified by {@code millis}
	 */
	public static Literal time(final long time, final boolean millis) {
		return literal(DateTimeFormatter
				.ofPattern(millis ? "yyyy-MM-dd'T'HH:mm:ssX" : "yyyy-MM-dd'T'HH:mm:ss.SSSX")
				.withZone(ZoneOffset.UTC)
				.format(Instant.ofEpochMilli(time)), XMLSchema.DATETIME);
	}


	public static Literal uuid() {
		return literal(randomUUID().toString());
	}

	public static Literal uuid(final String text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		return literal(nameUUIDFromBytes(text.getBytes(Codecs.UTF8)).toString());
	}


	///// Converters ///////////////////////////////////////////////////////////////////////////////////////////////////

	public static String iri(final Value value) {
		return value instanceof IRI ? value.stringValue() : null;
	}


	public static BigInteger integer(final Value value) {
		return value instanceof Literal ? ((Literal)value).integerValue() : null;
	}

	public static BigDecimal decimal(final Value value) {
		return value instanceof Literal ? ((Literal)value).decimalValue() : null;
	}


	public static Instant instant(final Value value) {

		return value instanceof Literal ? instant((Literal)value) : null;

	}

	public static Instant instant(final Literal literal) {
		if ( literal == null ) {

			return null;

		} else if ( literal.getDatatype().equals(XMLSchema.DATETIME) ) {

			final DateTimeFormatter formatter=DateTimeFormatter.ISO_DATE_TIME;
			final TemporalAccessor accessor=formatter.parse(literal.stringValue());

			return Instant.from(accessor.isSupported(ChronoField.INSTANT_SECONDS)
					? Instant.from(accessor)
					: LocalDateTime.from(accessor).atZone(ZoneId.systemDefault()));

		} else { // !!! review
			throw new UnsupportedOperationException("unsupported temporal datatype ["+literal.getDatatype()+"]");
		}
	}

	public static LocalDate localDate(final Value value) {
		return value instanceof Literal ? localDate((Literal)value) : null;
	}

	public static LocalDate localDate(final Literal value) { // !!! review/complete
		return value != null && value.getDatatype().equals(XMLSchema.DATETIME)
				? ISO_LOCAL_DATE_TIME.parse(value.stringValue()).query(TemporalQueries.localDate())
				: null; // !!! review
	}


	//// Formatters ////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String format(final Iterable<Statement> statements) {
		return statements == null ? null : stream(statements.spliterator(), false)
				.map(Values::format)
				.collect(joining("\n"));
	}

	public static String format(final Statement statement) {
		return statement == null ? null
				: format(statement.getSubject())
				+" "+format(statement.getPredicate())
				+" "+format(statement.getObject())
				+".";
	}


	public static String format(final Value value) {
		return value == null ? null : format(value, null);
	}

	public static String format(final Value value, final Map<String, String> namespaces) {
		return value == null ? null
				: value instanceof BNode ? format((BNode)value)
				: value instanceof IRI ? format((IRI)value, namespaces)
				: format((Literal)value, namespaces);
	}

	public static String format(final BNode bnode) {
		return bnode == null ? null : "_:"+bnode.getID();
	}

	public static String format(final IRI iri) {
		return format(iri, null);
	}

	public static String format(final IRI iri, final Map<String, String> namespaces) {
		if ( iri == null ) { return null; } else {

			final String text=iri.stringValue();

			if ( namespaces != null ) {
				for (final Map.Entry<String, String> entry : namespaces.entrySet()) {

					final String prefix=entry.getKey();
					final String namespace=entry.getValue();

					if ( prefix == null ) {
						throw new NullPointerException("null prefix");
					}

					if ( namespace == null ) {
						throw new NullPointerException("null namespace");
					}

					if ( text.startsWith(namespace) ) {

						final String name=text.substring(namespace.length());

						if ( name.isEmpty() ) { // !!! || namespaces.name(name)
							return prefix+':'+name;
						}
					}
				}
			}

			return '<'+text+'>'; // !!! relativize wrt to base
		}
	}

	public static String format(final Literal literal) {
		return format(literal, (Map<String, String>)null);
	}

	public static String format(final Literal literal, final Map<String, String> namespaces) { // !!! refactor
		if ( literal == null ) { return null; } else {

			final IRI type=literal.getDatatype();

			try {

				return type.equals(XMLSchema.BOOLEAN) ? String.valueOf(literal.booleanValue())
						: type.equals(XMLSchema.INTEGER) ? String.valueOf(literal.integerValue())
						: type.equals(XMLSchema.DECIMAL) ? literal.decimalValue().toPlainString()
						: type.equals(XMLSchema.DOUBLE) ? exponential().format(literal.doubleValue())
						: type.equals(XMLSchema.STRING) ? quote(literal.getLabel())
						: literal.getLanguage()
						.map(lang -> format(literal, lang))
						.orElseGet(() -> format(literal, type, namespaces));

			} catch ( final IllegalArgumentException ignored ) {
				return format(literal, type, namespaces);
			}
		}
	}


	private static String format(final Literal literal, final String lang) {
		return quote(literal.getLabel())+'@'+lang;
	}

	private static String format(final Literal literal, final IRI type, final Map<String, String> namespaces) {
		return quote(literal.getLabel())+"^^"+format(type, namespaces);
	}


	private static String quote(final CharSequence text) {

		final StringBuilder builder=new StringBuilder(text.length()+text.length()/10);

		builder.append('\''); // use single quote to interoperate with Java/JSON string

		for (int i=0, n=text.length(); i < n; ++i) {
			switch ( text.charAt(i) ) {
				case '\\':
					builder.append("\\\\");
					break;
				case '\'':
					builder.append("\\'");
					break;
				case '\r':
					builder.append("\\r");
					break;
				case '\n':
					builder.append("\\n");
					break;
				case '\t':
					builder.append("\\t");
					break;
				default:
					builder.append(text.charAt(i));
					break;
			}
		}

		builder.append('\'');

		return builder.toString();
	}


	//// Rewriters //////////////////////////////////////////////////////////////////////////////////// !!! permute args

	public static Collection<Statement> rewrite(final Collection<Statement> model, final IRI source, final IRI target) {
		return model.stream().map(statement -> rewrite(statement, source, target)).collect(toList());
	}

	public static Statement rewrite(final Statement statement, final IRI source, final IRI target) {
		return statement(
				rewrite(statement.getSubject(), source, target),
				rewrite(statement.getPredicate(), source, target),
				rewrite(statement.getObject(), source, target),
				rewrite(statement.getContext(), source, target)
		);
	}

	public static <T extends Value> T rewrite(final T value, final T source, final T target) {
		return source.equals(value) ? target : value;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Values() {}

}
