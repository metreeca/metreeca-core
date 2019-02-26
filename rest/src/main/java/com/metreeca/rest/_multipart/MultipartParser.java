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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.metreeca.form.things.Codecs.UTF8;
import static com.metreeca.form.things.Maps.entry;

import static java.util.Arrays.binarySearch;


final class MultipartParser {

	private static final int InitialBuffer=1000;

	private static final byte[] TokenChars="!#$%&'*+-.^_`|~".getBytes(UTF8);


	private enum Type {
		Empty, Data, Open, Close, EOF
	}


	@FunctionalInterface private static interface State {

		public State next(final Type type);

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final InputStream input;

	private final byte[] opening;
	private final byte[] closing;

	private final BiConsumer<List<Map.Entry<String, String>>, InputStream> handler;

	private State state=this::preamble;

	private int size;
	private byte[] buffer=new byte[InitialBuffer];

	private List<Map.Entry<String, String>> headers=new ArrayList<>();


	MultipartParser(
			final InputStream input, final String boundary,
			final BiConsumer<List<Map.Entry<String, String>>, InputStream> handler
	) {

		if ( boundary.isEmpty() ) {
			throw new IllegalArgumentException("empty boundary");
		}

		if ( boundary.length() > 70 ) {
			throw new IllegalArgumentException("illegal boundary");
		}

		this.input=input;

		this.opening=("--"+boundary).getBytes(UTF8);
		this.closing=("--"+boundary+"--").getBytes(UTF8);

		this.handler=handler;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	void parse() throws IOException {

		for (Type type=Type.Empty; type != Type.EOF; state=state.next(type=read())) {}

	}


	//// States ////////////////////////////////////////////////////////////////////////////////////////////////////////

	private State preamble(final Type type) {
		return type == Type.Empty ? skip(this::preamble)
				: type == Type.Data ? skip(this::preamble)
				: type == Type.Open ? skip(this::part)
				: type == Type.Close ? skip(this::epilogue)
				: type == Type.EOF ? skip(this::epilogue)
				: error("unexpected chunk type {"+type+"}");
	}

	private State part(final Type type) {
		return type == Type.Empty ? skip(this::body)
				: type == Type.Data ? header(this::part)
				: type == Type.Open ? report(this::part)
				: type == Type.Close ? report(this::epilogue)
				: type == Type.EOF ? report(this::epilogue)
				: error("unexpected chunk type {"+type+"}");
	}

	private State body(final Type type) {
		return type == Type.Empty ? this::body
				: type == Type.Data ? this::body
				: type == Type.Open ? report(this::part)
				: type == Type.Close ? report(this::epilogue)
				: type == Type.EOF ? report(this::epilogue)
				: error("unexpected chunk type {"+type+"}");
	}

	private State epilogue(final Type type) {
		return skip(this::epilogue);
	}


	//// Actions ///////////////////////////////////////////////////////////////////////////////////////////////////////

	private State header(final State next) {
		try {

			final int eol=size > 2 && buffer[size-2] == '\r' && buffer[size-1] == '\n' ? size-2 : size;

			int colon=0;

			while ( colon < eol && buffer[colon] != ':' ) { ++colon; }

			int value=colon+1;

			while ( value < eol && space(buffer[value]) ) { ++value; }

			if ( colon == 0 ) {
				return error("empty header name {"+new String(buffer, 0, eol, UTF8)+"}");
			}

			for (int i=0; i < colon; ++i) {
				if ( !token(buffer[i]) ) {
					return error("malformed header name {"+new String(buffer, 0, eol, UTF8)+"}");
				}
			}

			for (int i=value; i < eol; ++i) {
				if ( !printable(buffer[i] )) {
					return error("malformed header value {" +new String(buffer, 0, eol, UTF8)+ "}");
				}
			}

			headers.add(entry(
					new String(buffer, 0, colon, UTF8),
					new String(buffer, value, eol-value, UTF8)
			));

			return next;

		} finally {

			size=0;

		}
	}

	private State report(final State next) {
		try {

			handler.accept(headers, new ByteArrayInputStream(buffer, 0, size));

			return next;

		} finally {

			buffer=new byte[InitialBuffer];
			headers=new ArrayList<>();

		}
	}

	private State skip(final State next) {
		try {

			return next;

		} finally {

			size=0;

		}
	}

	private State error(final String message) {
		try {

			throw new IllegalStateException(message);

		} finally {

			state=null;

			size=0;

			buffer=null;
			headers=null;

		}
	}


	//// Chunk Reader //////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return the next CRLF-terminated input chunk
	 */
	private Type read() throws IOException {

		final int head=size;

		int cr=0;
		int lf=0;

		for (int c; !(cr == '\r' && lf == '\n') && (c=input.read()) >= 0; cr=lf, lf=c) {

			if ( size == buffer.length ) { // extend the buffer

				final byte[] buffer=new byte[this.buffer.length*2];

				System.arraycopy(this.buffer, 0, buffer, 0, this.buffer.length);

				this.buffer=buffer;
			}

			buffer[this.size++]=(byte)c;

		}

		final Type type=(head == size) ? Type.EOF
				: (head+2 == size && cr == '\r' && lf == '\n') ? Type.Empty
				: boundary(head, opening) ? Type.Open
				: boundary(head, closing) ? Type.Close
				: Type.Data;

		if ( type == Type.Open || type == Type.Close ) {
			size=(head >= 2) ? head-2 : head; // ignore boundary chunks and leading CRLF terminators
		}

		return type;
	}


	/**
	 * @param head     the starting position in the buffer to look for {@code boundary}
	 * @param boundary the boundary marker to be looke for in the buffer at position {@code head}
	 *
	 * @return {@code true}, if the buffer contains the {@code boundary} marker at position {@code head}, ignoring
	 * trailing whitespace; {@code false}, otherwise
	 */
	private boolean boundary(final int head, final byte... boundary) {

		for (int i=head, j=0; i < size && j < boundary.length; ++i, ++j) {
			if ( buffer[i] != boundary[j] ) { return false; }
		}

		for (int j=head+boundary.length; j < size; ++j) {
			if ( !space(buffer[j]) ) { return false; }
		}

		return true;
	}


	//// Character Classes /////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @param b the character to be tested
	 *
	 * @return {@code true}, if {@code b} is a printable character; {@code false}, otherwise
	 */
	private boolean printable(final byte b) {
		return b >= 32 && b <= 126 ;
	}

	/**
	 * @param b the character to be tested
	 *
	 * @return {@code true}, if {@code b} is a whitespace character; {@code false}, otherwise
	 */
	private boolean space(final byte b) {
		return b == ' ' || b == '\t' || b == '\r' || b == '\n';
	}

	/**
	 * @param b the character to be tested
	 *
	 * @return {@code true}, if {@code b} is a token character; {@code false}, otherwise
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.2.6">RFC 7230 Hypertext Transfer Protocol (HTTP/1.1):
	 * Message Syntax and Routing - § 3.2.6.  Field Value Components</a>
	 */
	private boolean token(final byte b) { //
		return b >= 'a' && b <= 'z'
				|| b >= 'A' && b <= 'Z'
				|| b >= '0' && b <= '0'
				|| binarySearch(TokenChars, b) >= 0;
	}

}
