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

package com.metreeca.core.formats;

import com.metreeca.core.MessageException;

import java.io.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.function.BiConsumer;

import static com.metreeca.core.MessageException.status;
import static com.metreeca.core.Response.BadRequest;
import static com.metreeca.core.Response.PayloadTooLarge;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.binarySearch;


final class MultipartParser {

	private static final byte[] TokenChars="!#$%&'*+-.^_`|~".getBytes(UTF_8);


	private enum Type {
		Empty, Data, Open, Close, EOF
	}


	@FunctionalInterface private static interface State {

		public State next(final Type type) throws MessageException;

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final int partLimit;
	private final int bodyLimit;

	private final int bufferStart;
	private final int bufferScale;

	private final InputStream input;

	private final byte[] opening;
	private final byte[] closing;

	private final BiConsumer<List<Map.Entry<String, String>>, InputStream> handler;

	private State state=this::preamble;

	private int part;
	private int body;

	private int last;
	private byte[] buffer;

	private List<Map.Entry<String, String>> headers=new ArrayList<>();


	MultipartParser(
			final int part, final int body,
			final InputStream input, final String boundary,
			final BiConsumer<List<Map.Entry<String, String>>, InputStream> handler
	) {

		this.partLimit=part;
		this.bodyLimit=body;

		this.bufferScale=10;
		this.bufferStart=100;

		this.input=input;

		this.opening=("--"+boundary).getBytes(UTF_8);
		this.closing=("--"+boundary+"--").getBytes(UTF_8);

		this.handler=handler;

		this.buffer=new byte[bufferStart];
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	void parse() throws IOException, MessageException {

		if ( opening.length == 2 ) {
			error(BadRequest, "empty boundary");
		}

		if ( opening.length > 70+2 ) {
			error(BadRequest, "illegal boundary");
		}

		for (Type type=Type.Empty; type != Type.EOF; state=state.next(type=read())) {}

	}


	//// States
	// //////////////////////////////////////////////////////////////////////////////////////////////////////

	private State preamble(final Type type) throws MessageException {
		return type == Type.Empty ? skip(this::preamble)
				: type == Type.Data ? skip(this::preamble)
				: type == Type.Open ? skip(this::part)
				: type == Type.Close ? skip(this::epilogue)
				: type == Type.EOF ? skip(this::epilogue)
				: error(BadRequest, "unexpected chunk type {"+type+"}");
	}

	private State part(final Type type) throws MessageException {
		return type == Type.Empty ? skip(this::body)
				: type == Type.Data ? header(this::part)
				: type == Type.Open ? report(this::part)
				: type == Type.Close ? report(this::epilogue)
				: type == Type.EOF ? report(this::epilogue)
				: error(BadRequest, "unexpected chunk type {"+type+"}");
	}

	private State body(final Type type) throws MessageException {
		return type == Type.Empty ? this::body
				: type == Type.Data ? this::body
				: type == Type.Open ? report(this::part)
				: type == Type.Close ? report(this::epilogue)
				: type == Type.EOF ? report(this::epilogue)
				: error(BadRequest, "unexpected chunk type {"+type+"}");
	}

	private State epilogue(final Type type) {
		return skip(this::epilogue);
	}


	//// Actions
	// /////////////////////////////////////////////////////////////////////////////////////////////////////

	private State header(final State next) throws MessageException {
		try {

			final int eol=last > 2 && buffer[last-2] == '\r' && buffer[last-1] == '\n' ? last-2 : last;

			int colon=0;

			while ( colon < eol && buffer[colon] != ':' ) { ++colon; }

			int value=colon+1;

			while ( value < eol && space(buffer[value]) ) { ++value; }

			if ( colon == 0 ) {
				return error(BadRequest, "empty header name {"+new String(buffer, 0, eol, UTF_8)+"}");
			}

			for (int i=0; i < colon; ++i) {
				if ( !token(buffer[i]) ) {
					return error(BadRequest, "malformed header name {"+new String(buffer, 0, eol, UTF_8)+"}");
				}
			}

			for (int i=value; i < eol; ++i) {
				if ( !printable(buffer[i]) ) {
					return error(BadRequest, "malformed header value {"+new String(buffer, 0, eol, UTF_8)+"}");
				}
			}

			headers.add(new SimpleImmutableEntry<>(
					new String(buffer, 0, colon, UTF_8),
					new String(buffer, value, eol-value, UTF_8)
			));

			return next;

		} finally {

			last=0;

		}
	}

	private State report(final State next) {
		try {

			handler.accept(headers, new ByteArrayInputStream(buffer, 0, last));

			return next;

		} finally {

			part=0;

			last=0;
			buffer=new byte[bufferStart];

			headers=new ArrayList<>();

		}
	}

	private State skip(final State next) {
		try {

			return next;

		} finally {

			last=0;

		}
	}

	private State error(final int status, final String message) throws MessageException {
		try {

			throw status(status, String.format("%s (%d)", message, body));

		} finally {

			state=null;

			last=0;
			buffer=null;

			headers=null;

		}
	}


	//// Chunk Reader //////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return the next CRLF-terminated input chunk
	 */
	private Type read() throws IOException, MessageException {

		final int head=last;

		int cr=0;
		int lf=0;

		for (int c; !(cr == '\r' && lf == '\n') && (c=input.read()) >= 0; cr=lf, lf=c, ++part, ++body, ++last) {

			if ( part >= partLimit ) {
				error(PayloadTooLarge, String.format("part size limit <%,d> exceeded", partLimit));
			}

			if ( body >= bodyLimit ) {
				error(PayloadTooLarge, String.format("body size limit <%,d> exceeded", bodyLimit));
			}

			if ( last == buffer.length ) { // extend the buffer

				final byte[] buffer=new byte[bufferScale*this.buffer.length];

				System.arraycopy(this.buffer, 0, buffer, 0, this.buffer.length);

				this.buffer=buffer;
			}

			buffer[last]=(byte)c;

		}

		final Type type=(head == last) ? Type.EOF
				: (head+2 == last && cr == '\r' && lf == '\n') ? Type.Empty
				: boundary(head, opening) ? Type.Open
				: boundary(head, closing) ? Type.Close
				: Type.Data;

		if ( type == Type.Open || type == Type.Close ) {
			last=(head >= 2) ? head-2 : head; // ignore boundary chunks and leading CRLF terminators
		}

		return type;
	}


	/**
	 * @param head     the starting position in the buffer to look for {@code boundary}
	 * @param boundary the boundary marker to be looke for in the buffer at position {@code head}
	 *
	 * @return {@code true}, if the buffer contains the {@code boundary} marker at position {@code head}, ignoring
	 * 		trailing whitespace; {@code false}, otherwise
	 */
	private boolean boundary(final int head, final byte... boundary) {

		for (int i=head, j=0; i < last && j < boundary.length; ++i, ++j) {
			if ( buffer[i] != boundary[j] ) { return false; }
		}

		for (int j=head+boundary.length; j < last; ++j) {
			if ( !space(buffer[j]) ) { return false; }
		}

		return true;
	}


	//// Character Classes /////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @param c the character to be tested
	 *
	 * @return {@code true}, if {@code c} is a whitespace character; {@code false}, otherwise
	 */
	private boolean space(final byte c) {
		return c == ' ' || c == '\t' || c == '\r' || c == '\n';
	}

	/**
	 * @param c the character to be tested
	 *
	 * @return {@code true}, if {@code c} is a printable character; {@code false}, otherwise
	 */
	private boolean printable(final byte c) {
		return c >= 32 && c <= 126;
	}

	/**
	 * @param c the character to be tested
	 *
	 * @return {@code true}, if {@code c} is a token character; {@code false}, otherwise
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.2.6">RFC 7230 Hypertext Transfer Protocol (HTTP/1.1):
	 * 		Message Syntax and Routing - § 3.2.6.  Field Value Components</a>
	 */
	private boolean token(final byte c) {
		return c >= 'a' && c <= 'z'
				|| c >= 'A' && c <= 'Z'
				|| c >= '0' && c <= '0'
				|| binarySearch(TokenChars, c) >= 0;
	}

}
