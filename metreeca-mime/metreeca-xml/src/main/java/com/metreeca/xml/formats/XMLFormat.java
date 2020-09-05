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

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import static com.metreeca.rest.Request.status;
import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.formats.ReaderFormat.reader;
import static com.metreeca.rest.formats.WriterFormat.writer;
import static java.util.regex.Pattern.compile;


/**
 * XML body format.
 */
public final class XMLFormat extends Format<Document> {

	/**
	 * The default MIME type for XML message bodies ({@value}).
	 */
	public static final String MIME="application/xml";

	/**
	 * A pattern matching XML-based MIME types, for instance {@code application/rss+xml}.
	 */
	public static final Pattern MIMEPattern=compile("(?i)^.*/(?:.*\\+)?xml(?:\\s*;.*)?$");


	/**
	 * Creates an XML body format.
	 *
	 * @return the new XML body format
	 */
	public static XMLFormat xml() {return new XMLFormat(null);}

	/**
	 * Creates an XML body format using a custom SAX parser.
	 *
	 * @param parser the custom SAX parser
	 *
	 * @return the new XML body format
	 *
	 * @throws NullPointerException if {@code parser} is null
	 */
	public static XMLFormat xml(final XMLReader parser) {

		if ( parser == null ) {
			throw new NullPointerException("null parser");
		}

		return new XMLFormat(parser);
	}


	/**
	 * Parses a XML document.
	 *
	 * @param input the input stream the XML document is to be parsed from
	 *
	 * @return a value result containing the parsed XML document, if {@code input} was successfully parsed; an error
	 * result containing the parse exception, otherwise
	 *
	 * @throws NullPointerException if {@code input} is null
	 */
	public static Result<Document, Exception> xml(final InputStream input) {

		if ( input == null ) {
			throw new NullPointerException("null input");
		}

		return xml(input, null);
	}

	/**
	 * Parses a XML document.
	 *
	 * @param input the input stream the XML document is to be parsed from
	 * @param base  the base URL for the XML document to be parsed
	 *
	 * @return a value result containing the parsed XML document, if {@code input} was successfully parsed; an error
	 * result containing the parse exception, otherwise
	 *
	 * @throws NullPointerException if {@code input} is null
	 */
	public static Result<Document, Exception> xml(final InputStream input, final String base) {

		if ( input == null ) {
			throw new NullPointerException("null input");
		}

		final InputSource source=new InputSource();

		source.setSystemId(base);
		source.setByteStream(input);

		return xml(new SAXSource(source));
	}

	/**
	 * Parses a XML document.
	 *
	 * @param source the source the XML document is to be parsed from
	 *
	 * @return a value result containing the parsed XML document, if {@code source} was successfully parsed; an error
	 * result containing the parse exception, otherwise
	 *
	 * @throws NullPointerException if {@code source} is null
	 */
	public static Result<Document, Exception> xml(final Source source) {

		if ( source == null ) {
			throw new NullPointerException("null source");
		}

		try {

			final Document document=builder().newDocument();

			document.setDocumentURI(source.getSystemId());

			transformer().transform(source, new DOMResult(document));

			return Value(document);

		} catch ( final TransformerException e ) {

			return Error(e);

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

			return TransformerFactory.newInstance().newTransformer();

		} catch ( final TransformerConfigurationException e ) {

			throw new RuntimeException("unable to create transformer", e);

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final XMLReader parser;


	private XMLFormat(final XMLReader parser) {
		this.parser=parser;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return the optional XML body representation of {@code message}, as retrieved from the reader supplied by its
	 * {@link ReaderFormat} representation, if one is present and the value of the {@code Content-Type} header is
	 * matched by {@link #MIMEPattern}; a failure reporting the {@link Response#UnsupportedMediaType} status,
	 * otherwise
	 */
	@Override public Result<Document, UnaryOperator<Response>> get(final Message<?> message) {

		return message
				.headers("Content-Type").stream()
				.anyMatch(MIMEPattern.asPredicate())

				? message
				.body(reader())
				.process(supplier -> {

					try ( final Reader reader=supplier.get() ) {

						final InputSource input=new InputSource();

						input.setSystemId(message.item());
						input.setCharacterStream(reader);

						final SAXSource source=parser != null ? new SAXSource(parser, input) : new SAXSource(input);

						source.setSystemId(message.item());

						return xml(source).error(cause -> status(Response.BadRequest, cause));

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}

				})

				: Error(status(Response.UnsupportedMediaType, "missing XML body")

		);
	}


	/**
	 * Configures the {@link WriterFormat} representation of {@code message} to write the XML {@code value} to the
	 * writer
	 * supplied by the accepted writer and sets the {@code Content-Type} header to {@value #MIME}, unless already
	 * defined.
	 */
	@Override public <M extends Message<M>> M set(final M message, final Document value) {
		return message
				.header("~Content-Type", MIME)
				.body(writer(), writer -> {

					final StreamResult result=new StreamResult(writer);
					final Source source=new DOMSource(value);

					source.setSystemId(message.item());
					result.setSystemId(message.item());

					try {

						transformer().transform(source, result);

					} catch ( final TransformerException e ) {

						throw new RuntimeException("unable to format XML body", e);

					}

				});
	}

}
