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

package com.metreeca.rest._multipart;

import com.metreeca.form.things.Values;
import com.metreeca.rest.*;

import org.eclipse.rdf4j.model.IRI;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.form.things.Values.iri;
import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.bodies.InputBody.input;

import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;


/**
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


	private static final MultipartBody Instance=new MultipartBody();


	public static MultipartBody multipart() {


		//if ( part <= 0 ) {
		//	throw new IllegalArgumentException("illegal part size limit ["+part+"]");
		//}
		//
		//if ( body <= 0 ) {
		//	throw new IllegalArgumentException("illegal body size limit ["+body+"]");
		//}
		//
		//if ( part > body ) {
		//	throw new IllegalArgumentException("illegal part ["+part+"]");
		//}


		return Instance;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private MultipartBody() {}


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

								new MultipartParser(0, 0, source.get(), boundary, (headers, body) -> {

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

									parts.put(name, new Part(item, message.request())

											.headers((Map<String, List<String>>)headers.stream().collect(groupingBy(
													Map.Entry::getKey,
													LinkedHashMap::new,
													mapping(Map.Entry::getValue, toList())
											)))

											.body(input(), () -> body)

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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class Part extends Message<Part> {

		private final IRI item;
		private final Request request;


		private Part(final IRI item, final Request request) {
			this.item=item;
			this.request=request;
		}


		@Override public IRI item() {
			return item;
		}

		@Override public Request request() {
			return request;
		}

	}

}
