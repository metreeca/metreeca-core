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

package com.metreeca.xml.actions;

import org.w3c.dom.*;

import java.util.Locale;
import java.util.function.Function;

import static com.metreeca.rest.actions.Clean.normalize;

/**
 * X/HTML to Markdown conversion.
 *
 * <p>Converts an X/HTMl document to a markdown-based plain text representation.</p>
 */
public final class Untag implements Function<Node, String> {

	@Override public String apply(final Node element) {
		return element == null ? "" : new Builder().format(element).toString();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class Builder {

		private final StringBuilder builder=new StringBuilder(100);


		private Builder format(final NodeList nodes) {

			for (int i=0, n=nodes.getLength(); i < n; ++i) {
				format(nodes.item(i));
			}

			return this;
		}

		private Builder format(final Node node) {
			return node instanceof Document ? format((Document)node)
					: node instanceof Element ? format((Element)node)
					: node instanceof Text ? format((Text)node)
					: this;
		}

		private Builder format(final Document document) {

			document.normalize();

			format(document.getDocumentElement());

			return this;
		}

		private Builder format(final Element element) {
			switch ( element.getTagName().toLowerCase(Locale.ROOT) ) {

				case "h1":

					return feed().append("# ").append(normalize(element.getTextContent()));

				case "h2":

					return feed().append("## ").append(normalize(element.getTextContent()));

				case "h3":

					return feed().append("### ").append(normalize(element.getTextContent()));

				case "p":

				case "ul":
				case "ol":

				case "div":
				case "section":

					return feed().format(element.getChildNodes());

				case "li":

					return wrap().append("- ").format(element.getChildNodes());

				case "br":

					return wrap();

				case "hr":

					return feed().append("---");

				case "head":
				case "style":
				case "script":

					return this;

				default:

					return format(element.getChildNodes());

			}
		}

		private Builder format(final Text text) {

			final String value=text.getNodeValue();
			final boolean border=text.getPreviousSibling() == null || text.getNextSibling() == null;

			if ( !(border && normalize(value).isEmpty()) ) {
				builder.append(value);
			}

			return this;
		}


		private Builder append(final String string) {

			builder.append(string);

			return this;
		}

		private Builder feed() {

			if ( builder.length() == 0 || builder.charAt(builder.length()-1) != '\n' ) {
				builder.append('\n');
			}

			if ( builder.length() == 1 || builder.charAt(builder.length()-2) != '\n' ) {
				builder.append('\n');
			}

			return this;
		}

		private Builder wrap() {

			if ( builder.length() == 0 || builder.charAt(builder.length()-1) != '\n' ) {
				builder.append('\n');
			}

			return this;
		}


		@Override public String toString() {
			return builder.toString();
		}

	}

}
