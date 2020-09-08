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

package com.metreeca.rdf;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.*;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.*;
import java.time.temporal.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.*;
import static java.util.UUID.nameUUIDFromBytes;
import static java.util.UUID.randomUUID;


/**
 * RDF utilities.
 */
public final class Values {

	/**
	 * A pattern matching absolute IRIs.
	 */
	public static final Pattern AbsoluteIRIPattern=Pattern.compile("^[a-zA-Z][-+.a-zA-Z0-9]*:.+$");

	/**
	 * A pattern matching IRI components.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc3986#appendix-B">RFC 3986 Uniform Resource Identifier (URI): Generic
	 * 		Syntax - Appendix B.  Parsing a URI Reference with a Regular Expression</a>
	 */
	public static final Pattern IRIPattern=Pattern.compile("^"
			+"(?<schemeall>(?<scheme>[^:/?#]+):)?"
			+"(?<hostall>//(?<host>[^/?#]*))?"
			+"(?<path>[^?#]*)"
			+"(?<queryall>\\?(?<query>[^#]*))?"
			+"(?<fragmentall>#(?<fragment>.*))?"
			+"$"
	);


	private static final ValueFactory factory=SimpleValueFactory.getInstance(); // before constant initialization

	private static final Comparator<Value> comparator=new ValueComparator();


	private static DecimalFormat exponential() { // ;( DecimalFormat is not thread-safe
		return new DecimalFormat("0.0#########E0", DecimalFormatSymbols.getInstance(Locale.ROOT));
	}


	private static final class Inverse extends SimpleIRI {

		private static final long serialVersionUID=7576383707001017160L;


		private Inverse(final String value) { super(value); }


		@Override public boolean equals(final Object object) {
			return object == this || object instanceof Inverse && super.equals(object);
		}

		@Override public int hashCode() { return -super.hashCode(); }

		@Override public String toString() {
			return "^"+super.toString();
		}

	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Internal namespace for local references and predicates (<code>{@value}</code>).
	 */
	public static final String Internal="app:/terms#";


	//// Extended Datatypes ///////////////////////////////////////////////////////////////////////////////////////////

	public static final IRI IRIType=iri(Internal, "iri"); // datatype IRI for IRI references
	public static final IRI BNodeType=iri(Internal, "bnode"); // datatype IRI for blank nodes
	public static final IRI LiteralType=iri(Internal, "literal"); // abstract datatype IRI for literals
	public static final IRI ResourceType=iri(Internal, "resource"); // abstract datatype IRI for resources
	public static final IRI ValueType=iri(Internal, "value"); // abstract datatype IRI for values


	//// Constants ////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final Literal True=literal(true);
	public static final Literal False=literal(false);


	//// Accessors ////////////////////////////////////////////////////////////////////////////////////////////////////

	public static boolean is(final Value value, final IRI datatype) {
		return value != null && (type(value).equals(datatype)
				|| value instanceof Resource && ResourceType.equals(datatype)
				|| value instanceof Literal && LiteralType.equals(datatype)
				|| ValueType.equals(datatype)
		);
	}

	/**
	 * Checks predicate direction.
	 *
	 * @param iri the IRI identifying the predicate
	 *
	 * @return {@code true} if {@code iri} is a direct predicate; {@code false} if {@code iri} is an {@link
	 *        #inverse(IRI)} predicate
	 *
	 * @throws NullPointerException if {@code iri } is null
	 */
	public static boolean direct(final IRI iri) { // !!! remove

		if ( iri == null ) {
			throw new NullPointerException("null iri");
		}

		return !(iri instanceof Inverse);
	}


	public static Predicate<Statement> pattern(final Value subject, final Value predicate, final Value object) {
		return statement
				-> (subject == null || subject.equals(statement.getSubject()))
				&& (predicate == null || predicate.equals(statement.getPredicate()))
				&& (object == null || object.equals(statement.getObject()));
	}


	public static int compare(final Value x, final Value y) {
		return comparator.compare(x, y);
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


	//// Factories ////////////////////////////////////////////////////////////////////////////////////////////////////


	public static ValueFactory factory() {
		return factory;
	}


	public static String uuid() {
		return randomUUID().toString();
	}

	public static String uuid(final String text) {
		return text == null ? null : nameUUIDFromBytes(text.getBytes(UTF_8)).toString();
	}


	public static String md5() {

		final byte[] bytes=new byte[16];

		ThreadLocalRandom.current().nextBytes(bytes);

		return DatatypeConverter
				.printHexBinary(bytes)
				.toLowerCase(Locale.ROOT);
	}

	public static String md5(final String text) {
		try {

			return text == null ? null : DatatypeConverter
					.printHexBinary(MessageDigest.getInstance("MD5").digest(text.getBytes(UTF_8)))
					.toLowerCase(Locale.ROOT);

		} catch ( final NoSuchAlgorithmException unexpected ) {
			throw new InternalError(unexpected);
		}
	}


	public static Namespace namespace(final String prefix, final String name) {
		return prefix == null || name == null ? null : new SimpleNamespace(prefix, name);
	}


	public static Statement statement(
			final Resource subject, final IRI predicate, final Value object) {
		return subject == null || predicate == null || object == null ? null
				: factory.createStatement(subject, predicate, object);
	}

	public static Statement statement(
			final Resource subject, final IRI predicate, final Value object, final Resource context) {
		return subject == null || predicate == null || object == null ? null
				: factory.createStatement(subject, predicate, object, context);
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

		return factory.createBNode(id.startsWith("_:") ? id.substring(2) : id);
	}


	public static IRI iri() {
		return factory.createIRI("urn:uuid:", uuid());
	}

	public static IRI iri(final URI uri) {
		return uri == null ? null : factory.createIRI(uri.toString());
	}

	public static IRI iri(final URL url) {
		return url == null ? null : factory.createIRI(url.toString());
	}

	public static IRI iri(final String iri) {
		return iri == null ? null : factory.createIRI(iri);
	}

	public static IRI iri(final IRI space, final String name) {
		return space == null || name == null ? null : iri(space.stringValue(), name);
	}

	public static IRI iri(final String space, final String name) {
		return space == null || name == null ? null
				: factory.createIRI(space, space.endsWith("/") && name.startsWith("/") ? name.substring(1) : name);
	}


	public static IRI internal(final String name) {
		return iri(Internal, name);
	}


	/**
	 * Inverts the direction of a predicate.
	 *
	 * @param iri the IRI identifying the predicate
	 *
	 * @return null, if {@code iri} is null; an inverse predicate IRI identified by the textual value of {@code iri}
	 * , if
	 *        {@code iri} is an {@linkplain #direct(IRI) predicate}; a direct predicate IRI identified by the textual
	 *        value of
	 *        {@code iri}, otherwise
	 */
	public static IRI inverse(final IRI iri) { // !!! remove
		return iri == null ? null
				: iri instanceof Inverse ? factory.createIRI(iri.stringValue())
				: new Inverse(iri.stringValue());
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


	public static Literal literal(final LocalDate value) {
		return value == null ? null : literal(ISO_LOCAL_DATE_TIME.format(value.atStartOfDay()), XSD.DATETIME);
	}

	public static Literal literal(final LocalDateTime value) {
		return value == null ? null : literal(ISO_LOCAL_DATE_TIME.format(value), XSD.DATETIME);
	}

	public static Literal literal(final OffsetDateTime value) {
		return value == null ? null : literal(ISO_OFFSET_DATE_TIME.format(value), XSD.DATETIME);
	}


	public static Literal literal(final byte[] value) {
		return value == null ? null : factory.createLiteral("data:application/octet-stream;base64,"
				+Base64.getEncoder().encodeToString(value), XSD.ANYURI);
	}

	public static Literal literal(final String value, final String lang) {
		return value == null || lang == null ? null : factory.createLiteral(value, lang);
	}

	public static Literal literal(final String value, final IRI datatype) {
		return value == null || datatype == null ? null : factory.createLiteral(value, datatype);
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
	 * 		specified by {@code millis}
	 */
	public static Literal time(final boolean millis) {
		return time(Instant.now(), millis);
	}


	/**
	 * Creates a date-time literal for a specific time.
	 *
	 * @param time the time to be converted represented as the number of milliseconds from the epoch of
	 *             1970-01-01T00:00:00Z
	 *
	 * @return an {@code xsd:dateTime} literal representing {@code time} with second precision
	 */
	public static Literal time(final long time) {
		return time(Instant.ofEpochMilli(time), false);
	}

	/**
	 * Creates a date-time literal for a specific time.
	 *
	 * @param time   the time to be converted represented as the number of milliseconds from the epoch of
	 *               1970-01-01T00:00:00Z
	 * @param millis if {@code true}, includes milliseconds in the literal textual representation
	 *
	 * @return an {@code xsd:dateTime} literal representing {@code time} with second or millisecond precision as
	 * 		specified by {@code millis}
	 */
	public static Literal time(final long time, final boolean millis) {
		return time(Instant.ofEpochMilli(time), millis);
	}


	/**
	 * Creates a date-instant literal for a specific instant.
	 *
	 * @param instant the instant to be converted
	 *
	 * @return an {@code xsd:dateTime} literal representing {@code instant} with second precision, if {@code instant
	 * } is
	 * 		not null; {@code null}, otherwise
	 */
	public static Literal time(final Instant instant) {
		return time(instant, false);
	}

	/**
	 * Creates a date-instant literal for a specific instant.
	 *
	 * @param instant the instant to be converted
	 * @param millis  if {@code true}, includes milliseconds in the literal textual representation
	 *
	 * @return an {@code xsd:dateTime} literal representing {@code instant} with second or millisecond precision as
	 * 		specified by {@code millis}, if {@code instant} is not null; {@code null}, otherwise
	 */
	public static Literal time(final Instant instant, final boolean millis) {
		return instant == null ? null : literal(
				ISO_DATE_TIME.format(instant.truncatedTo(millis ? ChronoUnit.MILLIS : ChronoUnit.SECONDS).atZone(UTC)),
				XSD.DATETIME
		);
	}


	///// Converters //////////////////////////////////////////////////////////////////////////////////////////////////

	public static Optional<String> iri(final Value value) {
		return value instanceof IRI ? Optional.of(value.stringValue()) : Optional.empty();
	}

	public static Optional<String> string(final Value value) {
		return value instanceof Literal ? Optional.of(value.stringValue()) : Optional.empty();
	}


	public static Optional<BigInteger> integer(final Value value) {
		try {
			return value instanceof Literal ? Optional.of(((Literal)value).integerValue()) : Optional.empty();
		} catch ( final NumberFormatException e ) {
			return Optional.empty();
		}
	}

	public static Optional<BigDecimal> decimal(final Value value) {
		try {
			return value instanceof Literal ? Optional.of(((Literal)value).decimalValue()) : Optional.empty();
		} catch ( final NumberFormatException e ) {
			return Optional.empty();
		}
	}


	public static Optional<Instant> instant(final Value value) {

		return value instanceof Literal ? instant((Literal)value) : Optional.empty();

	}

	public static Optional<Instant> instant(final Literal literal) {
		if ( literal == null ) {

			return Optional.empty();

		} else if ( literal.getDatatype().equals(XSD.DATETIME) ) {

			final TemporalAccessor accessor=ISO_DATE_TIME.parse(literal.stringValue());

			return Optional.of(Instant.from(accessor.isSupported(ChronoField.INSTANT_SECONDS)
					? Instant.from(accessor)
					: LocalDateTime.from(accessor).atZone(ZoneId.systemDefault())
			));

		} else { // !!! review
			throw new UnsupportedOperationException("unsupported temporal datatype ["+literal.getDatatype()+"]");
		}
	}

	public static Optional<LocalDate> localDate(final Value value) {
		return value instanceof Literal ? localDate((Literal)value) : Optional.empty();
	}

	public static Optional<LocalDate> localDate(final Literal value) { // !!! review/complete
		return Optional.ofNullable(value != null && value.getDatatype().equals(XSD.DATETIME)
				? ISO_LOCAL_DATE_TIME.parse(value.stringValue()).query(TemporalQueries.localDate())
				: null // !!! review
		);
	}


	//// Formatters ///////////////////////////////////////////////////////////////////////////////////////////////////

	public static String format(final Iterable<Statement> statements) {
		return format(statements, null);
	}

	public static String format(final Iterable<Statement> statements, final String base) {
		if ( statements == null ) {
			return null;
		} else {
			try ( final StringWriter writer=new StringWriter() ) {

				Rio.write(statements, writer, base, RDFFormat.TURTLE);

				return writer.toString();

			} catch ( final URISyntaxException e ) {
				throw new RuntimeException(e);
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}
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
		if ( iri == null ) {
			return null;
		} else {

			final String role=direct(iri) ? "" : "^";
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
							return role+prefix+':'+name;
						}
					}
				}
			}

			return role+'<'+text+'>'; // !!! relativize wrt to base
		}
	}

	public static String format(final Literal literal) {
		return format(literal, (Map<String, String>)null);
	}

	public static String format(final Literal literal, final Map<String, String> namespaces) { // !!! refactor
		if ( literal == null ) {
			return null;
		} else {

			final IRI type=literal.getDatatype();

			try {

				return type.equals(XSD.BOOLEAN) ? String.valueOf(literal.booleanValue())
						: type.equals(XSD.INTEGER) ? String.valueOf(literal.integerValue())
						: type.equals(XSD.DECIMAL) ? literal.decimalValue().toPlainString()
						: type.equals(XSD.DOUBLE) ? exponential().format(literal.doubleValue())
						: type.equals(XSD.STRING) ? quote(literal.getLabel())
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


	public static String quote(final CharSequence text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Values() {}

}
