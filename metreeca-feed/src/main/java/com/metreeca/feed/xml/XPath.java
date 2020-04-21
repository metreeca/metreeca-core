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

package com.metreeca.feed.xml;

import org.w3c.dom.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE;


public final class XPath {

	public static final String DefaultPrefix="_";


	private static final String HTMLPrefix="html";
	private static final String HTMLUri="http://www.w3.org/1999/xhtml";

	private static final Pattern EntityPattern=Pattern.compile("&#(?<hex>x?)(?<code>\\d+);", CASE_INSENSITIVE);


	private static final XPathFactory factory=XPathFactory.newInstance();


	public static Function<Node, Optional<String>> String(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return node -> new XPath(node).string(path);
	}

	public static Function<Node, Stream<String>> Strings(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return node -> new XPath(node).strings(path);
	}


	public static Function<Node, Stream<String>> Links(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return node -> new XPath(node).links(path);
	}


	public static Function<Node, Optional<Element>> Element(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return node -> new XPath(node).element(path);
	}

	public static Function<Node, Stream<Element>> Elements(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return node -> new XPath(node).elements(path);
	}


	public static Function<Node, Optional<Node>> Node(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return node -> new XPath(node).node(path);
	}

	public static Function<Node, Stream<Node>> Nodes(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return node -> new XPath(node).nodes(path);
	}


	public static <R> Function<Node, R> Query(final Function<XPath, R> query) {

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


	public XPath(final Node node) {

		if ( node == null ) {
			throw new NullPointerException("null node");
		}

		this.node=node;
		this.base=Optional.ofNullable(node.getBaseURI()).map(s -> {

			try {
				return new URI(s);
			} catch ( final URISyntaxException e ) {
				return null;
			}

		}).orElse(null);

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

	}


	public Node node() {
		return node;
	}

	public Document document() {
		return node instanceof Document ? (Document)node : node.getOwnerDocument();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Optional<Boolean> bool(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return Optional.of((Boolean)evaluate(path, XPathConstants.BOOLEAN));
	}

	public Optional<Double> number(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return Optional.of((Double)evaluate(path, XPathConstants.NUMBER)).filter(x -> !Double.isNaN(x));
	}


	public Optional<String> string(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return Optional.of((String)evaluate(path, XPathConstants.STRING)).filter(s -> !s.isEmpty());
	}

	public Stream<String> strings(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return nodes(path).map(Node::getTextContent);
	}


	public Stream<String> links(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return nodes(path).map(Node::getTextContent).map(s -> base == null ? s : base.resolve(s).toString());
	}


	public Optional<Element> element(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return node(path)
				.filter(node -> node instanceof Element)
				.map(node -> (Element)node);
	}

	public Stream<Element> elements(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return nodes(path)
				.filter(node -> node instanceof Element)
				.map(node -> (Element)node);
	}


	public Optional<Node> node(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return Optional.ofNullable((Node)evaluate(path, XPathConstants.NODE));
	}

	public Stream<Node> nodes(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<Node>() {

			private final NodeList nodes=(NodeList)evaluate(path, XPathConstants.NODESET);

			private int next=0;

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
