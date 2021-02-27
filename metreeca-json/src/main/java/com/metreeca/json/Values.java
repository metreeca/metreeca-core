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

package com.metreeca.json;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.base.AbstractNamespace;
import org.eclipse.rdf4j.model.base.AbstractValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ROOT;
import static java.util.UUID.nameUUIDFromBytes;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;


/**
 * Value utilities.
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
	 * Syntax - Appendix B.  Parsing a URI Reference with a Regular Expression</a>
	 */
	public static final Pattern IRIPattern=Pattern.compile("^"
			+"(?<schemeall>(?<scheme>[^:/?#]+):)?"
			+"(?<hostall>//(?<host>[^/?#]*))?"
			+"(?<pathall>"
			+"(?<path>[^?#]*)"
			+"(?<queryall>\\?(?<query>[^#]*))?"
			+"(?<fragmentall>#(?<fragment>.*))?"
			+")$"
	);


	private static final ValueFactory factory=new AbstractValueFactory() {}; // before constant initialization
	private static final Comparator<Value> comparator=new ValueComparator();

	private static final ThreadLocal<DecimalFormat> exponential=ThreadLocal.withInitial(() ->
			new DecimalFormat("0.0#########E0", DecimalFormatSymbols.getInstance(ROOT)) // ;( not thread-safe
	);

	private static final char[] HexDigits="0123456789abcdef".toCharArray();


	private static String hex(final byte[] bytes) {

		final char[] hex=new char[bytes.length*2];

		for (int i=0, l=bytes.length; i < l; ++i) {

			final int b=bytes[i]&0xFF;

			hex[2*i]=HexDigits[b >>> 4];
			hex[2*i+1]=HexDigits[b&0x0F];
		}

		return new String(hex);
	}


	//// Helpers ///////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String uuid() {
		return randomUUID().toString();
	}

	public static String uuid(final String text) {
		return text == null ? null : uuid(text.getBytes(UTF_8));
	}

	public static String uuid(final byte[] data) {
		return data == null ? null : nameUUIDFromBytes(data).toString();
	}


	public static String md5() {

		final byte[] bytes=new byte[16];

		ThreadLocalRandom.current().nextBytes(bytes);

		return hex(bytes);
	}

	public static String md5(final String text) {
		return text == null ? null : md5(text.getBytes(UTF_8));
	}

	public static String md5(final byte[] data) {
		try {

			return data == null ? null : hex(MessageDigest.getInstance("MD5").digest(data));

		} catch ( final NoSuchAlgorithmException unexpected ) {
			throw new InternalError(unexpected);
		}
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


	//// Constants /////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final Literal True=literal(true);
	public static final Literal False=literal(false);


	//// Extended Datatypes ////////////////////////////////////////////////////////////////////////////////////////////

	public static final IRI ValueType=iri("type:value"); // abstract datatype IRI for values
	public static final IRI ResourceType=iri("type:resource"); // abstract datatype IRI for resources
	public static final IRI BNodeType=iri("type:bnode"); // datatype IRI for blank nodes
	public static final IRI IRIType=iri("type:iri"); // datatype IRI for IRI references
	public static final IRI LiteralType=iri("type:literal"); // abstract datatype IRI for literals


	public static boolean derives(final IRI upper, final IRI lower) {
		return upper != null && lower != null && (
				upper.equals(ValueType)
						|| upper.equals(ResourceType) && resource(lower)
						|| upper.equals(LiteralType) && literal(lower)
		);
	}


	private static boolean resource(final IRI type) {
		return type.equals(ResourceType) || type.equals(BNodeType) || type.equals(IRIType);
	}

	private static boolean literal(final IRI type) {
		return type.equals(LiteralType) || !type.equals(ValueType) && !resource(type);
	}


	//// Comparator ////////////////////////////////////////////////////////////////////////////////////////////////////

	public static int compare(final Value x, final Value y) {
		return comparator.compare(x, y);
	}


	//// Accessors /////////////////////////////////////////////////////////////////////////////////////////////////////

	public static boolean is(final Value value, final IRI datatype) {
		return value != null && (type(value).equals(datatype)
				|| value instanceof Resource && ResourceType.equals(datatype)
				|| value instanceof Literal && LiteralType.equals(datatype)
				|| ValueType.equals(datatype)
		);
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

	public static ValueFactory factory() {
		return factory;
	}


	public static Namespace namespace(final String prefix, final String name) {
		return prefix == null || name == null ? null : new AbstractNamespace() {

			@Override public String getPrefix() { return prefix; }

			@Override public String getName() { return name; }

		};
	}


	public static Predicate<Statement> pattern(
			final Value subject, final Value predicate, final Value object
	) {
		return statement
				-> (subject == null || subject.equals(statement.getSubject()))
				&& (predicate == null || predicate.equals(statement.getPredicate()))
				&& (object == null || object.equals(statement.getObject()));
	}


	public static Statement statement(
			final Resource subject, final IRI predicate, final Value object
	) {
		return subject == null || predicate == null || object == null ? null
				: factory.createStatement(subject, predicate, object);
	}

	public static Statement statement(
			final Resource subject, final IRI predicate, final Value object, final Resource context
	) {
		return subject == null || predicate == null || object == null ? null
				: factory.createStatement(subject, predicate, object, context);
	}


	public static Value value(final Object value) {
		return value instanceof URI || value instanceof URL ? iri(value.toString()) : literal(value);
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
		return name == null ? null : factory.createIRI("app:/terms#", name);
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

				: value instanceof TemporalAccessor ? literal((TemporalAccessor)value)
				: value instanceof TemporalAmount ? literal((TemporalAmount)value)

				: value instanceof byte[] ? literal((byte[])value)

				: literal(value.getClass());
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


	public static Literal literal(final TemporalAccessor accessor) {
		return accessor == null ? null : factory.createLiteral(accessor);
	}

	public static Literal literal(final TemporalAmount amount) {
		return amount == null ? null : factory.createLiteral(amount);
	}


	public static Literal literal(final byte[] value) {
		return value == null ? null : factory.createLiteral(
				"data:application/octet-stream;base64,"+Base64.getEncoder().encodeToString(value), XSD.ANYURI);
	}


	public static Literal literal(final String value, final String lang) {
		return value == null || lang == null ? null : factory.createLiteral(value, lang);
	}

	public static Literal literal(final String value, final IRI datatype) {
		return value == null || datatype == null ? null : factory.createLiteral(value, datatype);
	}


	private static Literal literal(final Class<?> type) {
		throw new IllegalArgumentException(String.format("unsupported object type <%s>", type.getName()));
	}


	///// Converters //////////////////////////////////////////////////////////////////////////////////////////////////

	public static Optional<IRI> iri(final Value value) {
		return Optional.ofNullable(value).filter(IRI.class::isInstance).map(IRI.class::cast);
	}

	public static Optional<Literal> literal(final Value value) {
		return Optional.ofNullable(value).filter(Literal.class::isInstance).map(Literal.class::cast);
	}


	public static Optional<Boolean> _boolean(final Value value) {
		return literal(value).map(Literal::booleanValue);
	}


	public static Optional<Integer> _int(final Value value) {
		return literal(value).map(guard(Literal::intValue));
	}

	public static Optional<Long> _long(final Value value) {
		return literal(value).map(guard(Literal::longValue));
	}

	public static Optional<Float> __float(final Value value) {
		return literal(value).map(guard(Literal::floatValue));
	}

	public static Optional<Double> _double(final Value value) {
		return literal(value).map(guard(Literal::doubleValue));
	}

	public static Optional<BigInteger> integer(final Value value) {
		return literal(value).map(guard(Literal::integerValue));
	}

	public static Optional<BigDecimal> decimal(final Value value) {
		return literal(value).map(guard(Literal::decimalValue));
	}


	public static Optional<TemporalAccessor> temporalAccessor(final Value value) {
		return literal(value).map(guard(Literal::temporalAccessorValue));
	}

	public static Optional<TemporalAmount> temporalAmount(final Value value) {
		return literal(value).map(guard(Literal::temporalAmountValue));
	}


	public static Optional<String> string(final Value value) {
		return literal(value).map(Literal::stringValue);
	}


	private static <V, R> Function<V, R> guard(final Function<V, R> mapper) {
		return v -> {

			try { return mapper.apply(v); } catch ( final RuntimeException e ) { return null; }

		};
	}


	//// Formatters ///////////////////////////////////////////////////////////////////////////////////////////////////

	public static String format(final Statement statement) {
		return statement == null ? null : String.format("%s %s %s",
				format(statement.getSubject()), format(statement.getPredicate()), format(statement.getObject())
		);
	}


	public static String format(final Collection<? extends Value> values) {
		return values == null ? null : values.stream().map(Values::format).collect(joining(", "));
	}


	public static String format(final Value value) {
		return value == null ? null
				: value instanceof BNode ? format((BNode)value)
				: value instanceof IRI ? format((IRI)value)
				: format((Literal)value);
	}

	public static String format(final BNode bnode) {
		return bnode == null ? null : "_:"+bnode.getID();
	}

	public static String format(final IRI iri) {
		if ( iri == null ) { return null; } else {

			final String role=direct(iri) ? "" : "^";
			final String text=iri.stringValue();

			return role+'<'+text+'>'; // !!! relativize wrt to base
		}
	}

	public static String format(final Literal literal) {
		if ( literal == null ) { return null; } else {

			final IRI type=literal.getDatatype();

			try {

				return type.equals(XSD.BOOLEAN) ? String.valueOf(literal.booleanValue())
						: type.equals(XSD.INTEGER) ? String.valueOf(literal.integerValue())
						: type.equals(XSD.DECIMAL) ? literal.decimalValue().toPlainString()
						: type.equals(XSD.DOUBLE) ? exponential.get().format(literal.doubleValue())
						: type.equals(XSD.STRING) ? quote(literal.getLabel())

						: literal.getLanguage()
						.map(lang -> format(literal.getLabel(), lang))
						.orElseGet(() -> format(literal.getLabel(), type));

			} catch ( final IllegalArgumentException ignored ) {

				return format(literal.getLabel(), type);

			}
		}
	}


	private static String format(final CharSequence label, final String lang) {
		return quote(label)+'@'+lang;
	}

	private static String format(final CharSequence label, final IRI type) {
		return quote(label)+"^^"+format(type);
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


	//// Inverse IRIs //////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Checks predicate direction.
	 *
	 * @param iri the IRI identifying the predicate
	 *
	 * @return {@code true} if {@code iri} is a direct predicate; {@code false} if {@code iri} is an {@link
	 * #inverse(IRI)} predicate
	 */
	public static boolean direct(final IRI iri) {
		return !(iri instanceof Inverse);
	}

	/**
	 * Inverts the direction of a predicate.
	 *
	 * @param iri the IRI identifying the predicate
	 *
	 * @return null, if {@code iri} is null; an inverse predicate IRI identified by the textual value of {@code iri} ,
	 * if {@code iri} is an {@linkplain #direct(IRI) predicate}; a direct predicate IRI identified by the textual
	 * value of {@code iri}, otherwise
	 */
	public static IRI inverse(final IRI iri) {
		return iri == null ? null
				: iri instanceof Inverse ? factory.createIRI(iri.getNamespace(), iri.getLocalName())
				: new Inverse(iri.getNamespace(), iri.getLocalName());
	}


	private static final class Inverse extends org.eclipse.rdf4j.model.base.AbstractIRI {

		private static final long serialVersionUID=7576383707001017160L;


		private final String string;

		private final String namespace;
		private final String localname;


		private Inverse(final String namespace, final String localname) {
			this.string=namespace+localname;
			this.namespace=namespace;
			this.localname=localname;
		}


		@Override public String stringValue() {
			return string;
		}

		@Override public String getNamespace() {
			return namespace;
		}

		@Override public String getLocalName() {
			return localname;
		}


		@Override public boolean equals(final Object object) {
			return object == this || object instanceof Inverse && super.equals(object);
		}

		@Override public int hashCode() { return -super.hashCode(); }

		@Override public String toString() {
			return "^"+super.toString();
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Values() {}

}
