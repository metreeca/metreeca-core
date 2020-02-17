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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;


public final class XPath {

	private static final String HTMLPrefix="html";
	private static final String HTMLUri="http://www.w3.org/1999/xhtml";


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

			if ( XMLConstants.XMLNS_ATTRIBUTE.equals(attribute.getPrefix()) ) {
				namespaces.put(attribute.getLocalName(), attribute.getNodeValue());
			}
		}

		if ( HTMLUri.equals(root.getNamespaceURI())) {
			namespaces.putIfAbsent(HTMLPrefix, HTMLUri);
		}

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
