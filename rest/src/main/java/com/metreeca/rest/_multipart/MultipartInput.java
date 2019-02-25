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

import com.metreeca.form.things.Codecs;
import com.metreeca.form.things.Maps;
import com.metreeca.rest.Message;
import com.metreeca.rest.Request;
import com.metreeca.rest.bodies.InputBody;

import org.eclipse.rdf4j.model.IRI;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.metreeca.form.things.Values.iri;

import static java.lang.Math.min;
import static java.util.Collections.*;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;


final class MultipartInput {

	private static final int InitialBuffer=1000;


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Message<?> message;

	private final InputStream input;
	private final State[] boundary;

	private State state=this::lf;

	private int size;
	private int mark;

	private byte[] buffer=new byte[InitialBuffer];

	private String name;
	private final Collection<Map.Entry<String, String>> headers=new ArrayList<>();

	private Map<String, Message<?>> parts;


	MultipartInput(final Message<?> message, final InputStream input, final String boundary) {

		this.message=message;

		this.input=input;
		this.boundary=new State[boundary.length()];

		final byte[] bytes=boundary.getBytes(Codecs.UTF8);

		for (int i=0; i < bytes.length; i++) { // convert boundary into a sequence of states

			final int index=i;
			final byte b=bytes[i];

			this.boundary[i]=(index+1) < this.boundary.length
					? next -> next == b ? this.boundary[index+1] : unmark(this::part)
					: next -> next == b ? this::boundary : unmark(this::part);
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	Map<String, Message<?>> parse() throws IOException {

		int next;

		while ( (next=input.read()) >= 0 ) {

			if ( size == buffer.length ) { // extend the buffer

				final byte[] buffer=new byte[this.buffer.length*2];

				System.arraycopy(this.buffer, 0, buffer, 0, this.buffer.length);

				this.buffer=buffer;
			}

			buffer[size++]=(byte)next;

			state=state.process(next);

		}

		state=state.process(-1);

		return parts == null ? emptyMap() : unmodifiableMap(parts);
	}


	//// States ////////////////////////////////////////////////////////////////////////////////////////////////////////

	private State part(final int next) {
		return next == -1 ? part(this::epilogue)
				: next == '\r' ? mark(this::cr)
				: next == '\n' ? mark(this::lf)
				: this::part;
	}

	private State cr(final int next) {
		return next == -1 ? part(this::epilogue)
				: next == '\r' ? mark(this::cr)
				: next == '\n' ? this::lf
				: unmark(this::part);
	}

	private State lf(final int next) {
		return next == -1 ? part(this::epilogue)
				: next == '\r' ? mark(this::cr)
				: next == '\n' ? mark(this::lf)
				: next == '-' ? this::open
				: unmark(this::part);
	}

	private State open(final int next) {
		return next == -1 ? part(this::epilogue)
				: next == '-' ? boundary[0]
				: unmark(this::part);
	}

	private State boundary(final int next) {
		return next == -1 ? part(this::epilogue)
				: next == '-' ? this::close
				: this::ws;
	}

	private State close(final int next) {
		return next == -1 ? part(this::epilogue)
				: next == '-' ? part(this::epilogue)
				: unmark(this::part);
	}

	private State ws(final int next) {
		return next == -1 ? part(this::epilogue)
				: next == '\n' ? part(this::headers)
				: this::ws; // ignore trailing garbage on same line
	}

	private State headers(final int next) {
		return next == -1 ? part(this::epilogue)
				: next == '\r' ? this::headers // !!!
				: next == '\n' ? clear(this::part)
				: this::name;
	}

	private State name(final int next) {
		return next == -1 ? error("unexpected end of file while parsing headers")
				: next == ':' ? name(this::separator)
				: next == '_' || next >= 'a' && next <= 'z' || next >= 'A' && next <= 'Z' ? this::name
				: size > 0 && (next == '-' || next >= '0' && next <= '9') ? this::name
				: error("malformed header name {"+new String(buffer, 0, size, Codecs.UTF8)+"}");
	}

	private State separator(final int next) {
		return next == -1 ? error("unexpected end of file while parsing headers")
				: next == ' ' || next == '\t' ? skip(this::separator)
				: this::value;
	}

	private State value(final int next) {
		return next == -1 ? error("unexpected end of file while parsing headers")
				: next == '\r' ? skip(this::value) // !!!
				: next == '\n' ? value(this::headers)
				: this::value;
	}

	private State epilogue(final int next) {
		return this::epilogue;
	}

	private State error(final String message) {
		throw new IllegalStateException(message);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private State mark(final State state) {

		mark=size-1;

		return state;
	}

	private State unmark(final State state) {

		mark=Integer.MAX_VALUE;

		return state;
	}


	private State part(final State state) {

		if ( parts == null ) {

			parts=new LinkedHashMap<>(); // ignore preamble

		} else {

			final int length=min(size, mark);
			final byte[] buffer=this.buffer;

			final Map<String, List<String>> headers=this.headers.stream().collect(groupingBy(
					Map.Entry::getKey, LinkedHashMap::new, Collectors.mapping(Map.Entry::getValue, toList())
			));

			final String name="part"+parts.size(); // !!! compute from content-type header
			final IRI iri=iri(); // !!! compute from content-type header

			parts.put(name, new Part(iri, message)
					.headers(headers)
					.body(InputBody.input(), () -> new ByteArrayInputStream(buffer, 0, length))
			);

			this.buffer=new byte[InitialBuffer];
			this.headers.clear();
		}

		return clear(state);
	}

	private State name(final State state) {

		name=new String(buffer, 0, size-1, Codecs.UTF8);

		return clear(state);
	}

	private State value(final State state) {

		final String value=new String(buffer, 0, size-1, Codecs.UTF8);

		headers.add(Maps.entry(name, value));
		name=null;

		return clear(state);
	}

	private State skip(final State state) {

		size--;

		return state;
	}

	private State clear(final State state) {

		size=0;
		mark=Integer.MAX_VALUE;

		return state;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@FunctionalInterface private static interface State {

		public State process(final int next);

	}

	private static final class Part extends Message<Part> {

		private final IRI item;
		private final Request request;


		private Part(final IRI item, final Message<?> message) {
			this.item=item;
			this.request=message.request();
		}


		@Override public IRI item() { return item; }

		@Override public Request request() { return request; }

	}

}
