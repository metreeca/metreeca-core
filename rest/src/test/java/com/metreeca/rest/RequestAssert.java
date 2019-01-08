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

package com.metreeca.rest;


public final class RequestAssert extends MessageAssert<RequestAssert, Request> {

	public static RequestAssert assertThat(final Request request) {

		// !!! reporting

		//if ( request != null ) {
		//
		//	final ResponseAssert.Cache cache=new ResponseAssert.Cache(request);
		//
		//	if ( !request.body(input()).get().isPresent() ) {
		//		request.body(input()).set(cache::input); // cache binary body
		//	}
		//
		//	if ( !request.body(reader()).get().isPresent() ) {
		//		request.body(reader()).set(cache::reader); // cache textual body
		//	}
		//
		//	final StringBuilder builder=new StringBuilder(2500);
		//
		//	builder.append(request.status()).append('\n');
		//
		//	request.headers().forEach((name, values) -> values.forEach(value ->
		//			builder.append(name).append(": ").append(value).append('\n')
		//	));
		//
		//	builder.append('\n');
		//
		//	request.body(TextFormat.text()).use(text -> {
		//		if ( !text.isEmpty() ) {
		//
		//			final int limit=builder.capacity();
		//
		//			builder.append(text.length() <= limit ? text : text.substring(0, limit)+"\n⋮").append("\n\n");
		//		}
		//	});
		//
		//	Logger.getLogger(request.getClass().getName()).log(
		//			request.success() ? Level.INFO : Level.WARNING,
		//			builder.toString(),
		//			request.cause().orElse(null)
		//	);
		//
		//}

		return new RequestAssert(request);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private RequestAssert(final Request actual) {
		super(actual, RequestAssert.class);
	}

}
