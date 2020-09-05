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

package com.metreeca.xml.formats;

import com.metreeca.rest.Result;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.ReaderFormat;
import com.metreeca.rest.formats.WriterFormat;

import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.regex.Pattern;

import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.formats.ReaderFormat.reader;
import static com.metreeca.rest.formats.WriterFormat.writer;
import static java.util.regex.Pattern.compile;


/**
 * HTML body format.
 */
public final class HTMLFormat extends Format<Document> {

	/**
	 * The default MIME type for HTML message bodies ({@value}).
	 */
	public static final String MIME="text/html";

	/**
	 * A pattern matching the HTML MIME type.
	 */
	public static final Pattern MIMEPattern=compile("(?i)^(?:text/html)(?:\\s*;.*)?$");


	/**
	 * Creates an HTML body format.
	 *
	 * @return the new HTML body format
	 */
	public static HTMLFormat html() { return new HTMLFormat(); }


	/**
	 * Parses an HTML document.
	 *
	 * @param reader the reader the HTML document is to be parsed from
	 * @param base   the base URL for the HTML document to be parsed
	 *
	 * @return a value result containing the parsed HTML document, if {@code reader} was successfully parsed; an error
	 * result containg the parse exception, otherwise
	 *
	 * @throws NullPointerException if any argument is null
	 */
	public static Result<Document, Exception> html(final Reader reader, final String base) {

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		if ( base == null ) {
			throw new NullPointerException("null base URL");
		}

		try {

			final InputSource input=new InputSource();

			input.setSystemId(base);
			input.setCharacterStream(reader);

			final Document document=builder().newDocument();

			document.setDocumentURI(base);

			transformer().transform(

					new SAXSource(new Parser(), input),
					new DOMResult(document)

			);

			return Value(document);

		} catch ( final TransformerException e ) {

			return Error(e);

		}
	}

	/**
	 * Writes an HTML node.
	 *
	 * @param <W>    the type of the {@code writer} the HTML node is to be written to
	 * @param writer the writer the HTML node is to be written to
	 * @param base   the base URL for the HTML node to be written
	 * @param node   the HTML node to be written
	 *
	 * @return a value result containing the target {@code writer}, if {@code node} was successfully written; an
	 * error result containing the write exception, otherwise
	 *
	 * @throws NullPointerException if any argument is null
	 */
	public static <W extends Writer> Result<W, Exception> html(final W writer, final String base, final Node node) {

		if ( writer == null ) {
			throw new NullPointerException("null writer");
		}

		if ( base == null ) {
			throw new NullPointerException("null base");
		}

		if ( node == null ) {
			throw new NullPointerException("null node");
		}

		try {

			transformer().transform( // !!! format as HTML
					new DOMSource(node, base),
					new StreamResult(writer)
			);

			return Value(writer);

		} catch ( final TransformerException e ) {

			return Error(e);

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final DocumentBuilderFactory builders=DocumentBuilderFactory.newInstance();
	private static final TransformerFactory transformers=TransformerFactory.newInstance();


	static {
		transformers.setAttribute("indent-number", 4);
	}


	private static DocumentBuilder builder() {
		try {

			return builders.newDocumentBuilder();

		} catch ( final ParserConfigurationException e ) {

			throw new RuntimeException("unable to create document builder", e);

		}
	}

	private static Transformer transformer() {
		try {

			final Transformer transformer=transformers.newTransformer();

			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			return transformer;

		} catch ( final TransformerConfigurationException e ) {

			throw new RuntimeException("unable to create transformer", e);

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private HTMLFormat() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return the optional HTML body representation of {@code message}, as retrieved from the reader supplied by its
	 * {@link ReaderFormat} representation, if one is present and the value of the {@code Content-Type} header is
	 * {@link #MIME}; a failure reporting the {@link Response#UnsupportedMediaType} status, otherwise
	 */
	@Override public Result<Document, Failure> get(final Message<?> message) {

		return message
				.headers("Content-Type").stream()
				.anyMatch(MIMEPattern.asPredicate())

				? message
				.body(reader())
				.process(source -> {

					try ( final Reader reader=source.get() ) {

						return html(reader, message.item()).error(Failure::malformed);

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}

				})

				: Error(new Failure()
				.status(Response.UnsupportedMediaType)
				.notes("missing HTML body")

		);

	}


	/**
	 * Configures the {@link WriterFormat} representation of {@code message} to write the HML {@code value} to the
	 * writer
	 * supplied by the accepted writer and sets the {@code Content-Type} header to {@value #MIME}, unless already
	 * defined.
	 */
	@Override public <M extends Message<M>> M set(final M message, final Document value) {
		return message
				.header("~Content-Type", MIME)
				.body(writer(), writer -> html(writer, message.item(), value).error().ifPresent(e -> {

					throw new RuntimeException("unable to format HTML body", e);

				}));
	}

}
