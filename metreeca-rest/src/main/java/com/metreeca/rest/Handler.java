/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rest;

import com.metreeca.rest.assets.Loader;

import java.util.function.Predicate;

import static com.metreeca.json.Values.md5;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.Format.mime;
import static com.metreeca.rest.Request.*;
import static com.metreeca.rest.Response.*;
import static com.metreeca.rest.assets.Loader.loader;
import static com.metreeca.rest.formats.DataFormat.data;

import static java.lang.String.format;
import static java.util.Arrays.asList;


/**
 * Resource handler {thread-safe}.
 *
 * <p>Exposes and manages the state of a linked data resource, generating {@linkplain Response responses} in reaction
 * to {@linkplain Request requests}.</p>
 *
 * <p><strong>Warning</strong> / Implementations must be thread-safe.</p>
 */
@FunctionalInterface public interface Handler {

	/**
	 * Creates a conditional handler.
	 *
	 * @param test the request predicate used to decide if requests and responses are to be routed to the handler
	 * @param pass the handler requests and responses are to be routed to when {@code test} evaluates to {@code true
	 *             } on
	 *             the request
	 *
	 * @return a conditional handler that routes requests and responses to the {@code pass} handler if the {@code test}
	 * predicate evaluates to {@code true} on the request or to a dummy handler otherwise
	 *
	 * @throws NullPointerException if either {@code test} or {@code pass} is null
	 */
	public static Handler handler(final Predicate<Request> test, final Handler pass) {

		if ( test == null ) {
			throw new NullPointerException("null test predicate");
		}

		if ( pass == null ) {
			throw new NullPointerException("null pass handler");
		}

		return handler(test, pass, request -> request.reply(response -> response));
	}

	/**
	 * Creates a conditional handler.
	 *
	 * @param test the request predicate used to select the handler requests and responses are to be routed to
	 * @param pass the handler requests and responses are to be routed to when {@code test} evaluates to {@code true}
	 *             on the request
	 * @param fail the handler requests and responses are to be routed to when {@code test} evaluates to {@code false }
	 *             on the request
	 *
	 * @return a conditional handler that routes requests and responses either to the {@code pass} or the {@code fail}
	 * handler according to the results of the {@code test} predicate
	 *
	 * @throws NullPointerException if any of the arguments is null
	 */
	public static Handler handler(final Predicate<Request> test, final Handler pass, final Handler fail) {

		if ( test == null ) {
			throw new NullPointerException("null test predicate");
		}

		if ( pass == null ) {
			throw new NullPointerException("null pass handler");
		}

		if ( fail == null ) {
			throw new NullPointerException("null fail handler");
		}

		return request -> (test.test(request) ? pass : fail).handle(request);
	}


	/**
	 * Creates a static fallback handler.
	 *
	 * @param path the path of the {@linkplain Loader shared resource} to be served as fallback content
	 *
	 * @return a new GET handler unconditionally serving the content of the shared resource retrieved from {@code path}
	 * using the default {@linkplain Loader loader}.
	 *
	 * @throws NullPointerException if {@code path} is null
	 */
	public static Handler fallback(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		final byte[] data=asset(loader())
				.load(path)
				.map(Xtream::data)
				.orElseThrow(() -> new RuntimeException(format("missing <%s> resource", path)));

		final String mime=mime(path);
		final String length=String.valueOf(data.length);
		final String etag=format("\"%s\"", md5(data));

		return request -> request.reply(response -> {

			final String method=request.method();

			return (method.equals(GET) || method.equals(HEAD))
					&& request.header("If-None-Match").filter(etag::equals).isPresent()

					? response
					.status(NotModified)

					: method.equals(GET)

					? response
					.status(OK)
					.header("Content-Type", mime)
					.header("Content-Length", length)
					.header("ETag", etag)
					.body(data(), data)

					: method.equals(HEAD)

					? response
					.status(OK)
					.header("Content-Type", mime)
					.header("Content-Length", length)
					.header("ETag", etag)

					: response
					.status(method.equals(OPTIONS) ? OK : MethodNotAllowed)
					.headers("Allow", asList(OPTIONS, HEAD, GET));

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Handles a request.
	 *
	 * @param request the inbound request for the managed linked data resource
	 *
	 * @return a lazy response generated for the managed linked data resource in reaction to {@code request}; lazy
	 * processing supports streaming processing inside ephemeral context like database connections and transactions
	 */
	public Future<Response> handle(final Request request);


	/**
	 * Chains a wrapper.
	 *
	 * @param wrapper the wrapper to be chained
	 *
	 * @return the combined handler obtained by {@linkplain Wrapper#wrap(Handler) wrapping} this handler inside the
	 * combined wrapper generated by {@linkplain Wrapper#with(Wrapper) wrapping} {@code wrapper}
	 * <strong>inside</strong> previously chained wrappers
	 *
	 * @throws NullPointerException if {@code wrapper} is null
	 */
	public default Handler with(final Wrapper wrapper) {

		if ( wrapper == null ) {
			throw new NullPointerException("null wrapper");
		}

		final class Chain implements Handler {

			private final Wrapper wrapper;
			private final Handler handler;
			private final Handler chained;


			private Chain(final Wrapper wrapper, final Handler handler) {
				this.wrapper=wrapper;
				this.handler=handler;
				this.chained=wrapper.wrap(handler);
			}


			@Override public Handler with(final Wrapper wrapper) {

				if ( wrapper == null ) {
					throw new NullPointerException("null wrapper");
				}

				return new Chain(this.wrapper.with(wrapper), handler);
			}

			@Override public Future<Response> handle(final Request request) {

				if ( request == null ) {
					throw new NullPointerException("null request");
				}

				return chained.handle(request);
			}

		}

		return new Chain(wrapper, this);
	}

}
