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

package com.metreeca.link;

import com.metreeca.tray.IO;

import java.io.*;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;


/**
 * HTTP response.
 */
public final class Response extends Message<Response> {

	public static final int OK=200; // https://tools.ietf.org/html/rfc7231#section-6.3.1
	public static final int Created=201; // https://tools.ietf.org/html/rfc7231#section-6.3.2
	public static final int Accepted=202; // https://tools.ietf.org/html/rfc7231#section-6.3.3
	public static final int NoContent=204; // https://tools.ietf.org/html/rfc7231#section-6.3.5

	public static final int MovedPermanently=301; // https://tools.ietf.org/html/rfc7231#section-6.4.2
	public static final int SeeOther=303; // https://tools.ietf.org/html/rfc7231#section-6.4.4

	public static final int BadRequest=400; // https://tools.ietf.org/html/rfc7231#section-6.5.1
	public static final int Unauthorized=401; // https://tools.ietf.org/html/rfc7235#section-3.1
	public static final int Forbidden=403; // https://tools.ietf.org/html/rfc7231#section-6.5.3
	public static final int NotFound=404; // https://tools.ietf.org/html/rfc7231#section-6.5.4
	public static final int MethodNotAllowed=405; // https://tools.ietf.org/html/rfc7231#section-6.5.5
	public static final int Conflict=409; // https://tools.ietf.org/html/rfc7231#section-6.5.8
	public static final int UnprocessableEntity=422; // https://tools.ietf.org/html/rfc4918#section-11.2

	public static final int InternalServerError=500; // https://tools.ietf.org/html/rfc7231#section-6.6.1
	public static final int NotImplemented=501; // https://tools.ietf.org/html/rfc7231#section-6.6.2
	public static final int BadGateway=502; // https://tools.ietf.org/html/rfc7231#section-6.6.3
	public static final int ServiceUnavailable=503; // https://tools.ietf.org/html/rfc7231#section-6.6.4


	private static final String ContentType="Content-Type";


	private int status;
	private Throwable cause;

	private Consumer<OutputStream> body=output -> {};


	@Override protected Response self() { return this; }


	public int getStatus() {
		return status;
	}

	public Response setStatus(final int status) { // !!! § valid HTTP response status // https://tools.ietf.org/html/rfc7231#section-6

		if ( status < 0 || status > 599 ) {
			throw new IllegalArgumentException("illegal status ["+status+"]");
		}

		this.status=status;

		return this;
	}


	public Throwable getCause() {
		return cause;
	}

	public Response setCause(final Throwable cause) {

		this.cause=cause;

		return this;
	}


	public Consumer<OutputStream> getBody() {
		return body;
	}

	public Response setBody(final Consumer<OutputStream> body) { // !!! § supports streaming / once > IllegalStateException

		if ( body == null ) {
			throw new NullPointerException("null body");
		}

		this.body=body;

		return this;
	}

	public Response mapBody(final UnaryOperator<Consumer<OutputStream>> mapper) { // !!! § supports self-references

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		this.body=mapper.apply(body);

		return this;
	}


	public byte[] getData() {
		try (final ByteArrayOutputStream body=new ByteArrayOutputStream()) {

			this.body.accept(body);

			return body.toByteArray();

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

	public Response setData(final byte... data) {

		if ( data == null ) {
			throw new NullPointerException("null data");
		}

		if ( getHeaders(ContentType).isEmpty() ) {
			setHeader(ContentType, "application/octet-stream");
		}

		return setBody(out -> {
			try { out.write(data); } catch ( final IOException e ) { throw new UncheckedIOException(e); }
		});
	}


	public String getText() {
		return new String(getData(), IO.UTF8); // !!! use response encoding
	}

	public Response setText(final String text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		if ( getHeaders(ContentType).isEmpty() ) {
			setHeader(ContentType, "text/plain");
		}

		return setData(text.getBytes(IO.UTF8)); // !!! use response encoding
	}

}
