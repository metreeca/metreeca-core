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

import com.metreeca.rest.*;
import com.metreeca.rest.formats.InputFormat;
import com.metreeca.rest.formats.OutputFormat;

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

import static com.metreeca.rest.Either.Left;
import static com.metreeca.rest.Either.Right;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.BadRequest;
import static com.metreeca.rest.Response.UnsupportedMediaType;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;
import static java.util.regex.Pattern.compile;


/**
 * HTML message format.
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
	 * Creates an HTML message format.
	 *
	 * @return a new HTML message format
	 */
	public static HTMLFormat html() { return new HTMLFormat(); }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Parses an HTML document.
	 *
	 * @param reader the reader the HTML document is to be parsed from
	 * @param base   the possibly null base URL for the HTML document to be parsed
	 *
	 * @return either a parsing exception or the HTML document parsed from {@code reader}
	 *
	 * @throws NullPointerException if {@code reader} is null
	 */
	public static Either<TransformerException, Document> html(final Reader reader, final String base) {

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

			return Right(document);

		} catch ( final TransformerException e ) {

			return Left(e);

		}
	}

	/**
	 * Writes an HTML node.
	 *
	 * @param <W>    the type of the {@code writer} the HTML node is to be written to
	 * @param writer the writer the HTML node is to be written to
	 * @param base   the possibly null base URL for the HTML node to be written
	 * @param node   the HTML node to be written
	 *
	 * @return the target {@code writer}
	 *
	 * @throws NullPointerException if either {@code writer} or {@code node} is null
	 */
	public static <W extends Writer> W html(final W writer, final String base, final Node node) {

		if ( writer == null ) {
			throw new NullPointerException("null writer");
		}

		if ( node == null ) {
			throw new NullPointerException("null node");
		}

		try {

			transformer().transform(
					new DOMSource(node, base),
					new StreamResult(writer)
			);

			return writer;

		} catch ( final TransformerException unexpected ) {
			throw new RuntimeException(unexpected);
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static DocumentBuilder builder() {
		try {

			return DocumentBuilderFactory.newInstance().newDocumentBuilder();

		} catch ( final ParserConfigurationException e ) {

			throw new RuntimeException("unable to create document builder", e);

		}
	}

	private static Transformer transformer() {
		try {

			final TransformerFactory factory=TransformerFactory.newInstance();

			factory.setAttribute("indent-number", 4);

			final Transformer transformer=factory.newTransformer();

			transformer.setOutputProperty(OutputKeys.METHOD, "html");
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
	 * Decodes the HTML {@code message} body from the input stream supplied by the {@code message} {@link InputFormat}
	 * body, if one is available and the {@code message} {@code Content-Type} header is matched by
	 * {@link #MIMEPattern}, taking into account the {@code message} {@linkplain Message#charset() charset}
	 */
	@Override public Either<MessageException, Document> decode(final Message<?> message) {
		return message.header("Content-Type").filter(MIMEPattern.asPredicate())

				.map(type -> message.body(input()).flatMap(source -> {

					try (
							final InputStream input=source.get();
							final Reader reader=new InputStreamReader(input, message.charset())
					) {

						return html(reader, message.item()).fold(e -> Left(status(BadRequest, e)), Either::Right);

					} catch ( final UnsupportedEncodingException e ) {

						return Left(status(BadRequest, e));

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}

				}))

				.orElseGet(() -> Left(status(UnsupportedMediaType, "no HTML body")));
	}

	/**
	 * Configures {@code message} {@code Content-Type} header to {@value #MIME}, unless already defined, and encodes
	 * the HTML {@code value} into the output stream accepted by the {@code message} {@link OutputFormat} body,
	 * taking into account the {@code message} {@linkplain Message#charset() charset}
	 */
	@Override public <M extends Message<M>> M encode(final M message, final Document value) {
		return message

				.header("~Content-Type", MIME)

				.body(output(), output -> {

					try ( final Writer writer=new OutputStreamWriter(output, message.charset()) ) {

						html(writer, message.item(), value);

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}

				});
	}

}
