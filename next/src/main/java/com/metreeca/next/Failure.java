/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.next;

import java.util.Optional;

import javax.json.Json;
import javax.json.JsonValue;


/**
 * HTTP processing failure report.
 *
 * <p>Describes an error condition in HTTP request processing.</p>
 */
public final class Failure {

	/**
	 * The machine readable error tag for failures due to malformed data in message body.
	 */
	public static final String BodyMalformed="body-malformed";


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final int status;
	private final String error;
	private final JsonValue cause;


	/**
	 * Creates a failure report.
	 *
	 * @param status the HTTP status code associated with this failure report
	 *
	 * @throws IllegalArgumentException if {@code status } is less than 0 or greater than 599
	 */
	public Failure(final int status) {

		if ( status < 100 || status > 599 ) {
			throw new IllegalArgumentException("illegal status code ["+status+"]");
		}

		this.status=status;
		this.error=null;
		this.cause=null;
	}

	/**
	 * Creates a failure report.
	 *
	 * @param status the HTTP status code associated with this failure report
	 * @param error  a machine readable tag for the error condition
	 *
	 * @throws IllegalArgumentException if {@code status } is less than 0 or greater than 599
	 * @throws NullPointerException     if {@code error} is null
	 */
	public Failure(final int status, final String error) {

		if ( status < 100 || status > 599 ) {
			throw new IllegalArgumentException("illegal status code ["+status+"]");
		}

		if ( error == null ) {
			throw new NullPointerException("null error");
		}

		this.status=status;
		this.error=error;
		this.cause=null;
	}

	/**
	 * Creates a failure report.
	 *
	 * @param status the HTTP status code associated with this failure report
	 * @param error  a machine readable tag for the error condition
	 * @param cause  the underlying cause of the error condition
	 *
	 * @throws IllegalArgumentException if {@code status } is less than 0 or greater than 599
	 * @throws NullPointerException     if either {@code error} or {@code cause} is null
	 */
	public Failure(final int status, final String error, final Throwable cause) {

		if ( status < 100 || status > 599 ) {
			throw new IllegalArgumentException("illegal status code ["+status+"]");
		}

		if ( error == null ) {
			throw new NullPointerException("null error");
		}

		if ( cause == null ) {
			throw new NullPointerException("null cause");
		}

		this.status=status;
		this.error=error;
		this.cause=Json.createValue(Optional.ofNullable(cause.getMessage()).orElseGet(cause::toString));
	}

	/**
	 * Creates a failure report.
	 *
	 * @param status the HTTP status code associated with this failure report
	 * @param error  a machine readable tag for the error condition
	 * @param cause  a human readable description of the underlying cause of the error condition
	 *
	 * @throws IllegalArgumentException if {@code status } is less than 0 or greater than 599
	 * @throws NullPointerException     if either {@code error} or {@code cause} is null
	 */
	public Failure(final int status, final String error, final String cause) {

		if ( status < 100 || status > 599 ) {
			throw new IllegalArgumentException("illegal status code ["+status+"]");
		}

		if ( error == null ) {
			throw new NullPointerException("null error");
		}

		if ( cause == null ) {
			throw new NullPointerException("null cause");
		}

		this.status=status;
		this.error=error;
		this.cause=Json.createValue(cause);
	}

	/**
	 * Creates a failure report.
	 *
	 * @param status the HTTP status code associated with this failure report
	 * @param error  a machine readable tag for the error condition
	 * @param cause  a structured JSON report describing the underlying cause of the error condition
	 *
	 * @throws IllegalArgumentException if {@code status } is less than 0 or greater than 599
	 * @throws NullPointerException     if either {@code error} or {@code cause} is null
	 */
	public Failure(final int status, final String error, final JsonValue cause) {

		if ( status < 100 || status > 599 ) {
			throw new IllegalArgumentException("illegal status code ["+status+"]");
		}

		if ( error == null ) {
			throw new NullPointerException("null error");
		}

		if ( cause == null ) {
			throw new NullPointerException("null cause");
		}

		this.status=status;
		this.error=error;
		this.cause=cause;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the status code of this failure report.
	 *
	 * @return the HTTP status code associated with this failure report
	 */
	public int status() {
		return status;
	}

	/**
	 * Retrieves the error tag of this failure report.
	 *
	 * @return an optional a machine readable tag for the error condition of this failure report, if one was defined; an
	 * empty optional, otherwise
	 */
	public Optional<String> error() {
		return Optional.ofNullable(error);
	}

	/**
	 * Retrieves the underlying cause of this failure report.
	 *
	 * @return an optional a structured JSON report describing the underlying cause of the error condition of this
	 * failure report, if one was defined; an empty optional, otherwise
	 */
	public Optional<JsonValue> cause() {
		return Optional.ofNullable(cause);
	}

}
