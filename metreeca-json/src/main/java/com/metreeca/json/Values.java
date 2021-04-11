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
import org.eclipse.rdf4j.model.vocabulary.RDF;
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
import java.util.regex.Matcher;
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

	private static final String IRIScheme="(?<schemeall>(?<scheme>[-+\\w]+):)";
	private static final String IRIHost="(?<hostall>//(?<host>[^/?#]*))";
	private static final String IRIQuery="(?<queryall>\\?(?<query>[^#]*))";
	private static final String IRIFragment="(?<fragmentall>#(?<fragment>.*))";
	private static final String IRIPath="(?<pathall>(?<path>[^?#]*)"+IRIQuery+"?"+IRIFragment+"?)";


	/**
	 * A pattern matching absolute IRIs.
	 */
	public static final Pattern AbsoluteIRIPattern=Pattern.compile("^"+IRIScheme+IRIHost+"?"+IRIPath+"$");

	/**
	 * A pattern matching IRI components.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc3986#appendix-B">RFC 3986 Uniform Resource Identifier (URI): Generic
	 * Syntax - Appendix B.  Parsing a URI Reference with a Regular Expression</a>
	 */
	public static final Pattern IRIPattern=Pattern.compile("^"+IRIScheme+"?"+IRIHost+"?"+IRIPath+"$");


	private static final Pattern NewlinePattern=Pattern.compile("\n");


	private static final ValueFactory factory=new AbstractValueFactory() {}; // before constant initialization
	private static final Comparator<Value> comparator=new ValueComparator();

	private static final ThreadLocal<DecimalFormat> exponential=ThreadLocal.withInitial(() ->
			new DecimalFormat("0.0#########E0", DecimalFormatSymbols.getInstance(ROOT)) // ;( not thread-safe
	);

	private static final char[] HexDigits="0123456789abcdef".toCharArray();


	//// Constants /////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final Literal True=literal(true);
	public static final Literal False=literal(false);


	//// Internal Namespace ////////////////////////////////////////////////////////////////////////////////////////////

	public static final String Base="app:/";

	public static final Namespace NS=namespace("", Base+"terms#");


	public static IRI term(final String name) {
		return name == null ? null : iri(NS.getName(), name);
	}

	public static IRI item(final String name) {
		return name == null ? null : iri(Base, name);
	}


	//// Extended Datatypes ////////////////////////////////////////////////////////////////////////////////////////////

	public static final IRI ValueType=term("value"); // abstract datatype IRI for values
	public static final IRI ResourceType=term("resource"); // abstract datatype IRI for resources
	public static final IRI BNodeType=term("bnode"); // datatype IRI for blank nodes
	public static final IRI IRIType=term("iri"); // datatype IRI for IRI references
	public static final IRI LiteralType=term("literal"); // abstract datatype IRI for literals


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


	//// Inverse Predicates ////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * An IRI scheme for inverse predicates ({@value}).
	 */
	private static final String InverseScheme="inverse:";


	/**
	 * Checks predicate direction.
	 *
	 * @param predicate the IRI identifying the predicate
	 *
	 * @return {@code true} if {@code predicate} identifies a direct predicate; {@code false} if {@code predicate}
	 * identifies an {@link #inverse(IRI) inverse} predicate
	 *
	 * @throws NullPointerException if {@code predicate} is null
	 */
	public static boolean direct(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return !predicate.stringValue().startsWith(InverseScheme);
	}

	/**
	 * Creates an inverse predicate.
	 *
	 * @param predicate the IRI identifying the predicate
	 *
	 * @return the inverse version of {@code predicate}
	 *
	 * @throws NullPointerException if {@code predicate} is null
	 */
	public static IRI inverse(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		final String label=predicate.stringValue();

		return label.startsWith(InverseScheme)
				? iri(label.substring(InverseScheme.length()))
				: iri(InverseScheme+label);
	}

	/**
	 * Traverses a predicate.
	 *
	 * @param predicate the IRI identifying the predicate to be traversed
	 * @param direct    a predicate mapper to be executed if {@code predicate} is {@link #direct(IRI) direct}
	 * @param inverse   a predicate mapper to be executed if {@code predicate} is {@link #inverse(IRI) inverse}
	 * @param <V>       the type of the value returned by predicate mappers
	 *
	 * @return the value returned by the predicate mapper selected according to the direction of {@code predicate}
	 *
	 * @throws NullPointerException if any argument is null
	 */
	public static <V> V traverse(final IRI predicate, final Function<IRI, V> direct, final Function<IRI, V> inverse) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( direct == null ) {
			throw new NullPointerException("null direct");
		}

		if ( inverse == null ) {
			throw new NullPointerException("null inverse");
		}

		return predicate.stringValue().startsWith(InverseScheme)
				? inverse.apply(iri(predicate.stringValue().substring(InverseScheme.length())))
				: direct.apply(predicate);
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


	public static String root(final IRI resource) {
		return Optional.ofNullable(resource)
				.map(Value::stringValue)
				.map(IRIPattern::matcher)
				.filter(Matcher::matches)
				.map(matcher -> Optional.ofNullable(matcher.group("schemeall")).orElse("")
						+Optional.ofNullable(matcher.group("hostall")).orElse("")
						+"/"
				)
				.orElse(Base);
	}

	public static String path(final IRI resource) {
		return Optional.ofNullable(resource)
				.map(Value::stringValue)
				.map(IRIPattern::matcher)
				.filter(Matcher::matches)
				.map(matcher -> matcher.group("pathall"))
				.orElse("/");
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


	//// Identifiers ///////////////////////////////////////////////////////////////////////////////////////////////////

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


	public static String hex(final byte[] bytes) {
		if ( bytes == null ) { return null; } else {

			final char[] hex=new char[bytes.length*2];

			for (int i=0, l=bytes.length; i < l; ++i) {

				final int b=bytes[i]&0xFF;

				hex[2*i]=HexDigits[b >>> 4];
				hex[2*i+1]=HexDigits[b&0x0F];
			}

			return new String(hex);
		}
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


	public static Triple triple(
			final Resource subject, final IRI predicate, final Value object
	) {
		return subject == null || predicate == null || object == null ? null
				: factory.createTriple(subject, predicate, object);
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


	public static Literal literal(final boolean value) {
		return factory.createLiteral(value);
	}


	public static Literal literal(final Number value) {
		return literal(value, false);
	}

	public static Literal literal(final Number value, final boolean strict) {
		return value == null ? null

				: value instanceof Byte ? literal(value.byteValue(), strict)
				: value instanceof Short ? literal(value.shortValue(), strict)
				: value instanceof Integer ? literal(value.intValue(), strict)
				: value instanceof Long ? literal(value.longValue(), strict)

				: value instanceof Float ? literal(value.floatValue(), strict)
				: value instanceof Double ? literal(value.doubleValue(), strict)

				: value instanceof BigInteger ? literal((BigInteger)value)
				: value instanceof BigDecimal ? literal((BigDecimal)value)

				: null;
	}

	public static Literal literal(final byte value) {
		return literal(value, false);
	}

	public static Literal literal(final byte value, final boolean strict) {
		return strict ? factory.createLiteral(value) : factory.createLiteral(BigInteger.valueOf(value));
	}

	public static Literal literal(final short value) {
		return literal(value, false);
	}

	public static Literal literal(final short value, final boolean strict) {
		return strict ? factory.createLiteral(value) : factory.createLiteral(BigInteger.valueOf(value));
	}

	public static Literal literal(final int value) {
		return literal(value, false);
	}

	public static Literal literal(final int value, final boolean strict) {
		return strict ? factory.createLiteral(value) : factory.createLiteral(BigInteger.valueOf(value));
	}

	public static Literal literal(final long value) {
		return literal(value, false);
	}

	public static Literal literal(final long value, final boolean strict) {
		return strict ? factory.createLiteral(value) : factory.createLiteral(BigInteger.valueOf(value));
	}

	public static Literal literal(final float value) {
		return literal(value, false);
	}

	public static Literal literal(final float value, final boolean strict) {
		return strict || Float.isInfinite(value) || Float.isNaN(value) ?
				factory.createLiteral(value) : factory.createLiteral(BigDecimal.valueOf(value));
	}

	public static Literal literal(final double value) {
		return literal(value, false);
	}

	public static Literal literal(final double value, final boolean strict) {
		return strict || Double.isInfinite(value) || Double.isNaN(value) ?
				factory.createLiteral(value) : factory.createLiteral(BigDecimal.valueOf(value));
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


	//// Converters ////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Optional<Resource> resource(final Value value) {
		return Optional.ofNullable(value).filter(Value::isResource).map(Resource.class::cast);
	}

	public static Optional<BNode> bnode(final Value value) {
		return Optional.ofNullable(value).filter(Value::isBNode).map(BNode.class::cast);
	}

	public static Optional<IRI> iri(final Value value) {
		return Optional.ofNullable(value).filter(Value::isIRI).map(IRI.class::cast);
	}

	public static Optional<Literal> literal(final Value value) {
		return Optional.ofNullable(value).filter(Value::isLiteral).map(Literal.class::cast);
	}


	public static Optional<Boolean> bool(final Value value) {
		return literal(value).map(guard(Literal::booleanValue));
	}


	public static Optional<BigInteger> integer(final Value value) {
		return literal(value).map(guard(Literal::integerValue));
	}

	public static Optional<BigDecimal> decimal(final Value value) {
		return literal(value).map(guard(Literal::decimalValue));
	}


	public static Optional<String> string(final Value value) {
		return literal(value).map(guard(Literal::stringValue));
	}


	public static Optional<TemporalAccessor> temporalAccessor(final Value value) {
		return literal(value).map(guard(Literal::temporalAccessorValue));
	}

	public static Optional<TemporalAmount> temporalAmount(final Value value) {
		return literal(value).map(guard(Literal::temporalAmountValue));
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


	public static String format(final List<IRI> path) {
		return path == null ? null : path.stream().map(Values::format).collect(joining("/"));
	}

	public static String format(final Collection<? extends Value> values) {
		return values == null ? null : values.stream().map(Values::format).collect(joining(", "));
	}


	public static String format(final Value value) {
		return value == null ? null
				: value instanceof Focus ? format((Focus)value)
				: value instanceof BNode ? format((BNode)value)
				: value instanceof IRI ? format((IRI)value)
				: format((Literal)value);
	}

	public static String format(final Frame frame) {
		return frame == null ? null : frame.toString();
	}

	public static String format(final Focus focus) {
		return focus == null ? null : "{"+focus.stringValue()+"}";
	}

	public static String format(final BNode bnode) {
		return bnode == null ? null : "_:"+bnode.getID();
	}

	public static String format(final IRI iri) { // !!! relativize wrt to base
		return iri == null ? null : traverse(iri,
				direct -> direct.equals(RDF.TYPE) ? "a" : '<'+iri.stringValue()+'>',
				inverse -> "^<"+inverse.stringValue()+'>'
		);
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
						.map(lang -> quote(literal.getLabel())+'@'+lang)
						.orElseGet(() -> quote(literal.getLabel())+"^^"+format(type));

			} catch ( final IllegalArgumentException ignored ) {

				return quote(literal.getLabel())+"^^"+format(type);

			}
		}
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

	public static String indent(final CharSequence text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		return NewlinePattern.matcher(text).replaceAll("\n\t");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Values() {}

}
