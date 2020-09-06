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
import com.metreeca.rest.formats.InputFormat;
import com.metreeca.rest.formats.OutputFormat;

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
import java.util.regex.Pattern;

import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.BadRequest;
import static com.metreeca.rest.Response.UnsupportedMediaType;
import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;
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


	/**
	 * Creates an XML body format.
	 *
	 * @return a new XML body format
	 */
	public static XMLFormat xml() {return new XMLFormat(null);}

	/**
	 * Creates an XML body format using a custom SAX parser.
	 *
	 * @param parser the custom SAX parser
	 *
	 * @return a new XML body format a custom SAX {@code parser}
	 *
	 * @throws NullPointerException if {@code parser} is null
	 */
	public static XMLFormat xml(final XMLReader parser) {

		if ( parser == null ) {
			throw new NullPointerException("null parser");
		}

		return new XMLFormat(parser);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Parses a XML document.
	 *
	 * @param input the input stream the XML document is to be parsed from
	 *
	 * @return either a parsing exception or the XML document parsed from {@code input}
	 *
	 * @throws NullPointerException if {@code input} is null
	 */
	public static Result<Document, TransformerException> xml(final InputStream input) {

		if ( input == null ) {
			throw new NullPointerException("null input");
		}

		return xml(input, null);
	}

	/**
	 * Parses a XML document.
	 *
	 * @param input the input stream the XML document is to be parsed from
	 * @param base  the possibly null base URL for the XML document to be parsed
	 *
	 * @return either a parsing exception or the XML document parsed from {@code input}
	 *
	 * @throws NullPointerException if {@code input} is null
	 */
	public static Result<Document, TransformerException> xml(final InputStream input, final String base) {

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
	 * @return either a parsing exception or the XML document parsed from {@code source}
	 *
	 * @throws NullPointerException if {@code source} is null
	 */
	public static Result<Document, TransformerException> xml(final Source source) {

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

	private final XMLReader parser;


	private XMLFormat(final XMLReader parser) {
		this.parser=parser;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Decodes the XML {@code message} body from the input stream supplied by the {@code message} {@link InputFormat}
	 * body, if one is available and the {@code message} {@code Content-Type} header is matched by
	 * {@link #MIMEPattern}, taking into account the {@code message} {@linkplain Message#charset() charset}
	 */
	@Override public Result<Document, MessageException> decode(final Message<?> message) {
		return message.header("Content-Type").filter(MIMEPattern.asPredicate())

				.map(type -> message.body(input()).process(source -> {

					try ( final InputStream input=source.get() ) {

						final InputSource inputSource=new InputSource();

						inputSource.setSystemId(message.item());
						inputSource.setByteStream(input);
						inputSource.setEncoding(message.charset());

						final SAXSource saxSource=(parser != null)
								? new SAXSource(parser, inputSource)
								: new SAXSource(inputSource);

						saxSource.setSystemId(message.item());

						return xml(saxSource).fold(Result::Value, e -> Error(status(BadRequest, e)));

					} catch ( final UnsupportedEncodingException e ) {

						return Error(status(BadRequest, e));

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}

				}))

				.orElseGet(() -> Error(status(UnsupportedMediaType, "no XML body")));
	}


	/**
	 * Configures {@code message} {@code Content-Type} header to {@value #MIME}, unless already defined, and encodes
	 * the XML {@code value} into the output stream accepted by the {@code message} {@link OutputFormat} body,
	 * taking into account the {@code message} {@linkplain Message#charset() charset}
	 */
	@Override public <M extends Message<M>> M encode(final M message, final Document value) {
		return message

				.header("~Content-Type", MIME)

				.body(output(), output -> {

					try ( final Writer writer=new OutputStreamWriter(output, message.charset()) ) {

						final Source source=new DOMSource(value);
						final javax.xml.transform.Result result=new StreamResult(writer);

						source.setSystemId(message.item());
						result.setSystemId(message.item());

						transformer().transform(source, result);

					} catch ( final TransformerException unexpected ) {

						throw new RuntimeException(unexpected);

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}

				});
	}

}
