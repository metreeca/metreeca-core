/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.bodies;

import com.metreeca.form.things.Values;
import com.metreeca.rest.*;

import org.eclipse.rdf4j.model.IRI;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.form.things.Codecs.UTF8;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.bodies.InputBody.input;
import static com.metreeca.rest.bodies.OutputBody.output;

import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;


/**
 * Multipart body format.
 *
 * @see <a href="https://tools.ietf.org/html/rfc2046#section-5.1">RFC 2046 - Multipurpose Internet Mail Extensions
 * (MIME) Part Two: Media Types - § 5.1.  Multipart Media Type</a>
 */
public final class MultipartBody implements Body<Map<String, Message<?>>> {

	private static final Pattern BoundaryPattern=Pattern.compile(parameter("boundary"));
	private static final Pattern NamePattern=Pattern.compile(parameter("name"));
	private static final Pattern ItemPattern=Pattern.compile(parameter("filename"));


	private static String parameter(final String name) {
		return format(";\\s*(?i:%s)\\s*=\\s*(?:\"(?<quoted>[^\"]*)\"|(?<simple>[^;\\s]*))", name);
	}

	private String parameter(final Matcher matcher) {
		return Optional.ofNullable(matcher.group("quoted")).orElseGet(() -> matcher.group("simple"));
	}


	private static final MultipartBody Instance=new MultipartBody(0, 0); // write-only instance


	/**
	 * Retrieves a write-only multipart body format.
	 *
	 * @return a write-only multipart body format with part/body size limit set to 0, intended for configuring multipart
	 * response bodies
	 */
	public static MultipartBody multipart() {
		return Instance;
	}

	/**
	 * Retrieves a multipart body format.
	 *
	 * @param part the size limit for individual message parts; includes boundary and headers and applies also to
	 *             message preamble and epilogue
	 * @param body the size limit for the complete message body
	 *
	 * @return a write-only multipart body format with part/body size limit set to 0, intended for configuring multipart
	 * response bodies
	 *
	 * @throws IllegalArgumentException if either {@code part} or {@code body} is less than 0 or if {@code part} is
	 *                                  greater than {@code body}
	 */
	public static MultipartBody multipart(final int part, final int body) {

		if ( part < 0 ) {
			throw new IllegalArgumentException("negative part size limit");
		}

		if ( body < 0 ) {
			throw new IllegalArgumentException("negative body size limit");
		}

		if ( part > body ) {
			throw new IllegalArgumentException("part size limit greater than body size limit");
		}

		return new MultipartBody(part, body);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final int part;
	private final int body;


	private MultipartBody(final int part, final int body) {
		this.part=part;
		this.body=body;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Result<Map<String, Message<?>>, Failure> get(final Message<?> message) {
		return message.header("Content-Type")

				.filter(type -> type.startsWith("multipart/"))

				.map(type -> message.body(input()).<Result<Map<String, Message<?>>, Failure>>fold(

						source -> {

							final String boundary=message
									.header("Content-Type")
									.map(BoundaryPattern::matcher)
									.filter(Matcher::find)
									.map(this::parameter)
									.orElse("");

							final Map<String, Message<?>> parts=new LinkedHashMap<>();

							try {

								new MultipartParser(part, body, source.get(), boundary, (headers, content) -> {

									final Optional<String> disposition=headers
											.stream()
											.filter(entry ->
													entry.getKey().equalsIgnoreCase("Content-Disposition")
											)
											.map(Map.Entry::getValue)
											.findFirst();

									final String name=disposition
											.map(NamePattern::matcher)
											.filter(Matcher::find)
											.map(this::parameter)
											.filter(match -> !parts.containsKey(match))
											.orElseGet(() -> format("part%d", parts.size()));

									final IRI item=disposition
											.map(ItemPattern::matcher)
											.filter(Matcher::find)
											.map(this::parameter)
											.map(s -> iri("file:"+s))
											.orElseGet(Values::iri);

									parts.put(name, message.link(item)

											.headers((Map<String, List<String>>)headers.stream().collect(groupingBy(
													Map.Entry::getKey,
													LinkedHashMap::new,
													mapping(Map.Entry::getValue, toList())
											)))

											.body(input(), () -> content)

									);

								}).parse();

							} catch ( final ParseException e ) {

								return Error(new Failure().status(Response.BadRequest).cause(e));

							} catch ( final IOException e ) {

								throw new UncheckedIOException(e);

							}

							return Value(parts);

						},

						Result::Error

				))

				.orElseGet(() -> Error(Missing));
	}

	@Override public <T extends Message<T>> T set(final T message) {

		final byte[] bytes="-+0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes(UTF8);

		final byte[] dashes="--".getBytes(UTF8);
		final byte[] crlf="\r\n".getBytes(UTF8);
		final byte[] colon=": ".getBytes(UTF8);

		final String type=message.header("Content-Type").orElse("multipart/mixed");

		final byte[] boundary;

		final Matcher matcher=BoundaryPattern.matcher(type);

		if ( matcher.find() ) {

			boundary=parameter(matcher).getBytes(UTF8);

		} else {

			new Random().nextBytes(boundary=new byte[70]);

			for (int i=0; i < boundary.length; i++) {
				boundary[i]=bytes[(boundary[i]&0xFF)%bytes.length];
			}

			message.header("Content-Type", format("%s; boundary=\"%s\"", type, new String(boundary, UTF8)));

		}

		return message.body(output(), multipart().map(multipart -> target -> {
			try (final OutputStream out=target.get()) {

				for (final Message<?> part : multipart.values()) {

					out.write(dashes);
					out.write(boundary);
					out.write(crlf);

					for (final Map.Entry<String, Collection<String>> header : part.headers().entrySet()) {

						final String name=header.getKey();

						for (final String value : header.getValue()) {
							out.write(name.getBytes(UTF8));
							out.write(colon);
							out.write(value.getBytes(UTF8));
							out.write(crlf);
						}
					}

					out.write(crlf);

					part.body(output()).value().ifPresent(output -> output.accept(() -> out)); // !!! handle errors

					out.write(crlf);
				}

				out.write(dashes);
				out.write(boundary);
				out.write(dashes);
				out.write(crlf);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return object instanceof MultipartBody;
	}

	@Override public int hashCode() {
		return MultipartBody.class.hashCode();
	}

}
