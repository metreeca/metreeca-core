/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.next;

import com.metreeca.jeep.IO;
import com.metreeca.jeep.JSON;

import java.io.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.json.JsonException;

import static com.metreeca.jeep.Jeep.title;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;


public final class Part { // !!! factor to Message

	public static Writer part() {
		return new Part().new Writer();
	}


	private String filename="";

	private Map<String, List<String>> headers=new LinkedHashMap<>();

	private Supplier<InputStream> input;
	private Supplier<Reader> reader;


	private Part() {}


	public String filename() {
		return filename;
	}


	public Stream<Map.Entry<String, List<String>>> headers() {
		return headers.entrySet().stream();
	}

	public List<String> headers(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return headers.getOrDefault(title(name), emptyList());
	}

	public Optional<String> header(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return headers(name).stream().findFirst();
	}


	public InputStream input() {
		return input != null ? input.get()
				: reader != null ? IO.input(reader.get())
				: IO.input();
	}

	public Reader reader() {
		return reader != null ? reader.get()
				: input != null ? IO.reader(input.get())
				: IO.reader();
	}


	public byte[] data() {
		try (final InputStream input=input()) {
			return IO.data(input);
		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

	public String text() {
		try (final Reader reader=reader()) {
			return IO.text(reader);
		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	public Object json() throws JsonException {
		return JSON.decode(text());
	}


	public final class Writer {

		private Writer() {}


		public Writer filename(final String filename) {

			if ( filename == null ) {
				throw new NullPointerException("null filename");
			}

			Part.this.filename=filename;

			return this;
		}


		public Writer headers(final Stream<Map.Entry<String, Collection<String>>> headers) {

			if ( headers == null ) {
				throw new NullPointerException("null headers");
			}

			headers.forEachOrdered(header -> header(header.getKey(), header.getValue()));

			return this;
		}

		public Writer header(final String name, final String... values) {
			return header(name, asList(values));
		}

		public Writer header(final String name, final Collection<String> values) {

			if ( name == null ) {
				throw new NullPointerException("null name");
			}

			if ( values == null ) {
				throw new NullPointerException("null values");
			}

			if ( values.contains(null) ) {
				throw new NullPointerException("null value");
			}

			headers.compute(title(name), (key, current) -> unmodifiableList(values.stream().flatMap(
					value -> value.isEmpty() ? current == null ? Stream.empty() : current.stream() : Stream.of(value)
			).distinct().collect(toList())));

			return this;
		}


		public Part input(final Supplier<InputStream> input) {

			if ( input == null ) {
				throw new NullPointerException("null input");
			}

			Part.this.input=input;
			Part.this.reader=null;

			return done();
		}

		public Part reader(final Supplier<Reader> reader) {

			if ( reader == null ) {
				throw new NullPointerException("null reader");
			}

			Part.this.input=null;
			Part.this.reader=reader;

			return done();
		}


		public Part body(final Supplier<InputStream> data, final Supplier<Reader> text) {

			if ( data == null ) {
				throw new NullPointerException("null data");
			}

			if ( text == null ) {
				throw new NullPointerException("null text");
			}

			Part.this.input=data;
			Part.this.reader=text;

			return done();
		}


		public Part data(final byte... data) {

			if ( data == null ) {
				throw new NullPointerException("null data");
			}

			return input(() -> new ByteArrayInputStream(data));
		}

		public Part text(final String text) {

			if ( text == null ) {
				throw new NullPointerException("null text");
			}

			return reader(() -> new StringReader(text));
		}


		public Part json(final Object json) throws JsonException {

			if ( json == null ) {
				throw new NullPointerException("null json");
			}

			return header("Content-Type", "application/json").text(JSON.encode(json));
		}


		public Part done() {
			return Part.this;
		}

	}

}
