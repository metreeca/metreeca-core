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

package com.metreeca.rest.operators;

import com.metreeca.json.shapes.Guard;
import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.handlers.Delegator;
import com.metreeca.rest.services.Engine;

import static com.metreeca.json.shapes.Guard.Detail;
import static com.metreeca.json.shapes.Guard.Relate;
import static com.metreeca.rest.Toolbox.service;
import static com.metreeca.rest.services.Engine.engine;


/**
 * Model-driven resource relator.
 *
 * <p>Performs:</p>
 *
 * <ul>
 *
 * <li>{@linkplain Guard#Role role}-based request shape redaction and shape-based
 * {@linkplain Engine#throttler(Object, Object) authorization}, considering shapes enabled by the
 * {@linkplain Guard#Relate} task and {@linkplain Guard#Detail} view;</li>
 *
 * <li>engine assisted resource {@linkplain Engine#relate(Request) retrieval};</li>
 *
 * <li>shape-driven response payload redaction, considering shapes as above.</li>
 *
 * </ul>
 *
 * <p>All operations are executed inside a single {@linkplain Engine#transaction() engine transaction}.</p>
 */
public final class Relator extends Delegator {

	/**
	 * Creates a resource relator.
	 *
	 * @return a new resource relator
	 */
	public static Relator relator() {
		return new Relator();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Relator() {

		final Engine engine=service(engine());

		delegate(((Handler)engine::relate)

				.with(engine.transaction())
				.with(engine.throttler(Relate, Detail))

		);
	}

}
