/*
 * Copyright © 2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.xml.actions;

import org.w3c.dom.*;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE;


/**
 * XPath expression matching.
 *
 * <p>Applies XPath expression to XML nodes.</p>
 */
public final class XPath {

	/**
	 * The prefix mapped to the default namespace of the target document ({@value}).
	 */
	public static final String DefaultPrefix="_";


	private static final String HTMLPrefix="html";
	private static final String HTMLUri="http://www.w3.org/1999/xhtml";

	private static final Pattern EntityPattern=Pattern.compile("&#(?<hex>x?)(?<code>\\d+);", CASE_INSENSITIVE);

	private static final XPathFactory factory=XPathFactory.newInstance();


	/**
	 * Creates an xpath-based function.
	 *
	 * @param query a function taking as argument an xpath matching action and returning a value
	 * @param <R>   the type of the value returned by {@code query}
	 *
	 * @return a function taking an XML node as argument and returning the value produced by applying {@code query}
	 * to an xpath matching action targeting the argument
	 *
	 * @throws NullPointerException if {@code query} is null
	 */
	public static <R> Function<Node, R> XPath(final Function<XPath, R> query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		return node -> query.apply(new XPath(node));
	}


	/**
	 * Decodes XML numeric entities.
	 *
	 * @param text the text to be decoded
	 *
	 * @return a version of {@code text} where XML numeric entities (for instance {@code &#x2019;} or {@code &#8220;})
	 * are replaced with the corresponding Unicode characters
	 *
	 * @throws NullPointerException if {@code text} is null
	 */
	public static String decode(final CharSequence text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		final StringBuffer buffer=new StringBuffer(text.length());
		final Matcher matcher=EntityPattern.matcher(text);

		while ( matcher.find() ) {

			matcher.appendReplacement(buffer, "");

			buffer.append(Character.toChars(
					Integer.parseInt(matcher.group("code"), matcher.group("hex").isEmpty() ? 10 : 16)
			));
		}

		matcher.appendTail(buffer);

		return buffer.toString();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Node node;
	private final URI base;

	private final javax.xml.xpath.XPath xpath;

	/**
	 * Creates an xpath matching action.
	 *
	 * @param node the target XML node for the action
	 *
	 * @throws NullPointerException if {@code node} is null
	 */
	public XPath(final Node node) {

		if ( node == null ) {
			throw new NullPointerException("null node");
		}

		this.xpath=factory.newXPath();

		final Node root=node instanceof Document ? ((Document)node).getDocumentElement() : node;
		final NamedNodeMap attributes=root.getAttributes();

		final Map<String, String> namespaces=new HashMap<>();

		for (int i=0, n=attributes.getLength(); i < n; ++i) {

			final Node attribute=attributes.item(i);

			if ( XMLNS_ATTRIBUTE.equals(attribute.getNodeName()) ) { // default namespace

				namespaces.put(DefaultPrefix, attribute.getNodeValue());

			} else if ( XMLNS_ATTRIBUTE.equals(attribute.getPrefix()) ) { // prefixed namespace

				namespaces.put(attribute.getLocalName(), attribute.getNodeValue());

			}

		}

		final String namespace=root.getNamespaceURI();

		namespaces.computeIfAbsent(DefaultPrefix, prefix -> namespace);
		namespaces.computeIfAbsent(HTMLPrefix, prefix -> HTMLUri.equals(namespace) ? namespace : null);

		xpath.setNamespaceContext(new NamespaceContext() {

			@Override public String getNamespaceURI(final String prefix) {
				return namespaces.get(prefix);
			}

			@Override public String getPrefix(final String namespaceURI) {
				throw new UnsupportedOperationException("prefix lookup");
			}

			@Override public Iterator<String> getPrefixes(final String namespaceURI) {
				throw new UnsupportedOperationException("prefixes lookup");
			}

		});


		this.node=node;
		this.base=Optional

				.ofNullable(Optional
						.of((String)evaluate("/html:html/html:head/html:base/@href", XPathConstants.STRING))
						.filter(href -> !href.isEmpty())
						.orElse(node.getBaseURI())
				)

				.map(url -> {

					try {
						return new URI(url);
					} catch ( final URISyntaxException e ) {
						return null;
					}

				})

				.orElse(null);

	}


	/**
	 * Retrieves the target node.
	 *
	 * @return the target node of this xpath matching action.
	 */
	public Node node() {
		return node;
	}

	/**
	 * Retrieves the target document.
	 *
	 * @return the target document of this xpath matching action.
	 */
	public Document document() {
		return node instanceof Document ? (Document)node : node.getOwnerDocument();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves a boolean value from the target node.
	 *
	 * @param xpath the xpath expression to be evaluated against the target node
	 *
	 * @return an optional boolean containing the value produced by evaluating {@code xpath} against the target node
	 *
	 * @throws NullPointerException if {@code xpath} is null
	 */
	public Optional<Boolean> bool(final String xpath) {

		if ( xpath == null ) {
			throw new NullPointerException("null xpath");
		}

		return Optional.of((Boolean)evaluate(xpath, XPathConstants.BOOLEAN));
	}

	/**
	 * Retrieves a numeric value from the target node.
	 *
	 * @param xpath the xpath expression to be evaluated against the target node
	 *
	 * @return an optional number containing the value produced by evaluating {@code xpath} against the target node,
	 * if one was available; an empty optional, otherwise
	 *
	 * @throws NullPointerException if {@code xpath} is null
	 */
	public Optional<Double> number(final String xpath) {

		if ( xpath == null ) {
			throw new NullPointerException("null xpath");
		}

		return Optional.of((Double)evaluate(xpath, XPathConstants.NUMBER)).filter(x -> !Double.isNaN(x));
	}


	/**
	 * Retrieves a textual value from the target node.
	 *
	 * @param xpath the xpath expression to be evaluated against the target node
	 *
	 * @return an optional non-empty string containing the value produced by evaluating {@code xpath} against the
	 * target node, if one was available and not empty; an empty optional, otherwise
	 *
	 * @throws NullPointerException if {@code xpath} is null
	 */
	public Optional<String> string(final String xpath) {

		if ( xpath == null ) {
			throw new NullPointerException("null xpath");
		}

		return Optional.of((String)evaluate(xpath, XPathConstants.STRING)).filter(s -> !s.isEmpty());
	}

	/**
	 * Retrieves textual values from the target node.
	 *
	 * @param xpath the xpath expression to be evaluated against the target node
	 *
	 * @return a stream of non-empty strings containing the values produced by evaluating {@code xpath} against the
	 * target node
	 *
	 * @throws NullPointerException if {@code xpath} is null
	 */
	public Stream<String> strings(final String xpath) {

		if ( xpath == null ) {
			throw new NullPointerException("null xpath");
		}

		return nodes(xpath).map(Node::getTextContent).filter(s -> !s.isEmpty());
	}


	/**
	 * Retrieves a URI value from the target node.
	 *
	 * @param xpath the xpath expression to be evaluated against the target node
	 *
	 * @return an optional string URIs containing the values produced by evaluating {@code xpath} against the target
	 * node, if one was available, and resolving it against the {@linkplain Node#getBaseURI() base URI} of the target
	 * node, if available; syntactically malformed URIs are returned verbatim
	 *
	 * @throws NullPointerException if {@code xpath} is null
	 */
	public Optional<String> link(final String xpath) {

		if ( xpath == null ) {
			throw new NullPointerException("null xpath");
		}

		return node(xpath).map(Node::getTextContent).map(s -> {
			try {

				return base == null ? s : base.resolve(s).normalize().toString();

			} catch ( final IllegalArgumentException e ) {

				return s;

			}
		});
	}

	/**
	 * Retrieves URI values from the target node.
	 *
	 * @param xpath the xpath expression to be evaluated against the target node
	 *
	 * @return a stream of URIs containing the values produced by evaluating {@code xpath} against the target node
	 * and resolving them against the {@linkplain Node#getBaseURI() base URI} of the target node, if available;
	 * syntactically malformed URIs are returned verbatim
	 *
	 * @throws NullPointerException if {@code xpath} is null
	 */
	public Stream<String> links(final String xpath) {

		if ( xpath == null ) {
			throw new NullPointerException("null xpath");
		}

		return nodes(xpath).map(Node::getTextContent).map(s -> {
			try {

				return base == null ? s : base.resolve(s).normalize().toString();

			} catch ( final IllegalArgumentException e ) {

				return s;

			}
		});
	}


	/**
	 * Retrieves an element value from the target node.
	 *
	 * @param xpath the xpath expression to be evaluated against the target node
	 *
	 * @return an optional element containing the value produced by evaluating {@code xpath} against the target node,
	 * if one was available; an empty optional, otherwise
	 *
	 * @throws NullPointerException if {@code xpath} is null
	 */
	public Optional<Element> element(final String xpath) {

		if ( xpath == null ) {
			throw new NullPointerException("null xpath");
		}

		return node(xpath)
				.filter(Element.class::isInstance)
				.map(Element.class::cast);
	}

	/**
	 * Retrieves element values from the target node.
	 *
	 * @param xpath the xpath expression to be evaluated against the target node
	 *
	 * @return a stream of elements containing the values produced by evaluating {@code xpath} against the target node
	 *
	 * @throws NullPointerException if {@code xpath} is null
	 */
	public Stream<Element> elements(final String xpath) {

		if ( xpath == null ) {
			throw new NullPointerException("null xpath");
		}

		return nodes(xpath)
				.filter(Element.class::isInstance)
				.map(Element.class::cast);
	}


	/**
	 * Retrieves a node value from the target node.
	 *
	 * @param xpath the xpath expression to be evaluated against the target node
	 *
	 * @return an optional node containing the value produced by evaluating {@code xpath} against the target node,
	 * if one was available; an empty optional, otherwise
	 *
	 * @throws NullPointerException if {@code xpath} is null
	 */
	public Optional<Node> node(final String xpath) {

		if ( xpath == null ) {
			throw new NullPointerException("null xpath");
		}

		return Optional.ofNullable((Node)evaluate(xpath, XPathConstants.NODE));
	}

	/**
	 * Retrieves node values from the target node.
	 *
	 * @param xpath the xpath expression to be evaluated against the target node
	 *
	 * @return a stream of nodes containing the values produced by evaluating {@code xpath} against the target node
	 *
	 * @throws NullPointerException if {@code xpath} is null
	 */
	public Stream<Node> nodes(final String xpath) {

		if ( xpath == null ) {
			throw new NullPointerException("null xpath");
		}

		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<Node>() {

			private final NodeList nodes=(NodeList)evaluate(xpath, XPathConstants.NODESET);

			private int next;

			@Override public boolean hasNext() {
				return next < nodes.getLength();
			}

			@Override public Node next() throws NoSuchElementException {

				if ( !hasNext() ) {
					throw new NoSuchElementException("no more iterator elements");
				}

				return nodes.item(next++);

			}

		}, Spliterator.ORDERED), false);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Object evaluate(final String query, final QName type) {
		try {

			return xpath.compile(query).evaluate(node, type);

		} catch ( final XPathExpressionException e ) {
			throw new RuntimeException(String.format("unable to evaluate xpath expression {%s}", query), e);
		}
	}

}
