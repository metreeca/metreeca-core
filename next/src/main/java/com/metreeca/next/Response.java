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

import java.util.function.Consumer;


public final class Response extends Writable<Response> implements Source<Response> {

	private final Request request;

	private int status;


	public Response(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		this.request=request;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override protected Response self() {
		return this;
	}

	@Override public void accept(final Consumer<Response> consumer) {

		if ( consumer == null ) {
			throw new NullPointerException("null consumer");
		}

		consumer.accept(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Request request() {
		return request;
	}


	public int status() {
		return status;
	}

	public Response status(final int status) {

		if ( status < 100 || status > 599 ) {
			throw new IllegalArgumentException("illegal status ["+status+"]");
		}

		this.status=status;

		return this;
	}

}
