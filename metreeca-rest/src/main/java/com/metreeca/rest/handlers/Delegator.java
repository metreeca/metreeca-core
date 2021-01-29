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

package com.metreeca.rest.handlers;

import com.metreeca.rest.*;

import static java.util.function.Function.identity;


/**
 * Delegating handler.
 *
 * <p>Delegates request processing to a {@linkplain #delegate(Handler) delegate} handler, possibly assembled as a
 * combination of other handlers and wrappers.</p>
 */
public abstract class Delegator implements Handler {

	private Handler delegate=request -> request.reply(identity());


	/**
	 * Configures the delegate handler.
	 *
	 * @param delegate the handler request processing is delegated to
	 *
	 * @return this delegator
	 *
	 * @throws NullPointerException     if {@code delegate} is null
	 */
	protected Delegator delegate(final Handler delegate) {

		if ( delegate == null ) {
			throw new NullPointerException("null delegate");
		}

		this.delegate=delegate;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Future<Response> handle(final Request request) {
		return delegate.handle(request);
	}

}
