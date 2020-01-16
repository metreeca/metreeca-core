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

package com.metreeca.rest.formats;

import com.metreeca.rest.Result;
import com.metreeca.rest.*;

import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.*;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import static com.metreeca.rest.Failure.malformed;
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
	public static final Pattern MIMEPattern=compile("(?i)^(?:.*/(?:.*\\+)?xml|text/html)(?:\\s*;.*)?$");


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


	private static final DocumentBuilderFactory builders=DocumentBuilderFactory.newInstance();
	private static final TransformerFactory transformers=TransformerFactory.newInstance();


	private static DocumentBuilder builder() {
		try {

			return builders.newDocumentBuilder();

		} catch ( final ParserConfigurationException e ) {

			throw new RuntimeException("unable to create document builder", e);

		}
	}

	private static Transformer transformer() {
		try {

			return transformers.newTransformer();

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
	 * matched by {@link #MIMEPattern}; a failure reporting the {@link Response#UnsupportedMediaType} status, otherwise
	 */
	@Override public Result<Document, Failure> get(final Message<?> message) {

		final Optional<String> type=message
				.headers("Content-Type").stream()
				.filter(mime -> MIMEPattern.matcher(mime).matches())
				.findFirst();

		final boolean xml=type.isPresent();
		final boolean html=type.filter(t -> t.toLowerCase(Locale.ROOT).startsWith("text/html")).isPresent();

		return xml

				? message
				.body(reader())
				.process(source -> {

					try (final Reader reader=source.get()) {

						final InputSource input=new InputSource(message.item());

						input.setCharacterStream(reader);

						final Document document=builder().newDocument();

						transformer().transform(

								parser != null ? new SAXSource(parser, input)
										: html ? new SAXSource(new Parser(), input)
										: new SAXSource(input),

								new DOMResult(document)

						);

						return Value(document);

					} catch ( final TransformerException e ) {

						return Error(malformed(e));

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}

				})

				: Error(new Failure()
				.status(Response.UnsupportedMediaType)
				.notes("missing XML body")

		);
	}


	/**
	 * Configures the {@link WriterFormat} representation of {@code message} to write the XML {@code value} to the
	 * writer supplied by the accepted writer and sets the {@code Content-Type} header to {@value #MIME}, unless already
	 * defined.
	 */
	@Override public <M extends Message<M>> M set(final M message, final Document value) {
		return message
				.header("~Content-Type", MIME)
				.body(writer(), target -> {
					try (final Writer writer=target.get()) {

						transformer().transform(
								new DOMSource(value, message.item()),
								new StreamResult(writer)
						);

					} catch ( final TransformerException e ) {

						throw new RuntimeException("unable to format XML body", e);

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}

				});
	}

}
