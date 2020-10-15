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

package com.metreeca.xml.actions;

import com.metreeca.rest.Xtream;
import com.metreeca.rest.actions.Clean;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.*;
import java.util.function.Function;

import static com.metreeca.xml.actions.XPath.XPath;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparingDouble;

/**
 * Main X/HTMl content extraction.
 *
 * <p>Identifies the X/HTMl node containing the main textual content of a complex page.</p>
 */
public final class Extract implements Function<Node, Optional<Node>> {

	private static final Collection<String> textual=new HashSet<>(asList(
			"h1", "h2", "h3", "h4", "h5", "h6",
			"p", "blockquote", "pre",
			"ul", "ol", "dl", "li", "dt", "dd",
			"table", "th", "td"
	));

	private static final Collection<String> ignored=new HashSet<>(asList(
			"style", "script"
	));


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Optional<Node> apply(final Node root) {
		if ( root == null ) { return Optional.empty(); } else {
			return Xtream

					.of(annotate(root))

					.flatMap(XPath(x -> x.nodes("//*")))

					.max(comparingDouble(value -> get(value, "echars", 0.0)))

					.map((node -> {

						try {

							// create a new document to provide a root for xpath queries

							final Document document=DocumentBuilderFactory
									.newInstance()
									.newDocumentBuilder()
									.newDocument();

							document.setDocumentURI(node.getBaseURI());
							document.appendChild(document.adoptNode(node.cloneNode(true)));
							document.normalizeDocument();

							return document;

						} catch ( final ParserConfigurationException unexpected ) {
							throw new RuntimeException(unexpected);
						}

					}));
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private <T extends Node> T annotate(final T node) {

		if ( node instanceof Document ) {

			((Document)node).normalizeDocument();

			annotate(((Document)node).getDocumentElement());

		} else if ( node instanceof Element && !ignored.contains(node.getNodeName()) ) {

			double xchars=0;
			double echars=0;

			int nodes=0;
			int blobs=0;

			final NodeList children=node.getChildNodes();

			for (int i=0, n=children.getLength(); i < n; ++i) {

				final Node child=annotate(children.item(i));

				xchars+=get(child, "xchars", 0.0);
				echars+=get(child, "echars", 0.0);

				if ( child instanceof Element ) { ++nodes; }
				if ( textual.contains(child.getNodeName()) ) { ++blobs; }

			}

			final boolean text=textual.contains(node.getNodeName()) && echars == 0;

			set(node, "xchars", xchars);
			set(node, "echars", text ? xchars : echars*(blobs+1)/(nodes+1));

			((Element)node).setAttribute("chars", String.format("%.1f/%.0f",
					get(node, "echars", 0.0),
					get(node, "xchars", 0.0)
			));

		} else if ( node instanceof Text ) {

			final double length=Clean.normalize(node.getTextContent()).length();

			set(node, "xchars", length*length);

		}

		return node;

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unchecked") private <T> T get(final Node node, final String label, final T value) {
		return Optional.ofNullable((T)node.getUserData(label)).orElse(value);
	}

	private <T> void set(final Node node, final String label, final T value) {
		node.setUserData(label, value, null);
	}

}
