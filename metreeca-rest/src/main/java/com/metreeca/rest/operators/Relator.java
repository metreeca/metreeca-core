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

import com.metreeca.json.Shape;
import com.metreeca.json.shapes.Guard;
import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.assets.Engine;
import com.metreeca.rest.formats.JSONLDFormat;
import com.metreeca.rest.handlers.Delegator;

import javax.json.JsonObject;

import static com.metreeca.json.shapes.Guard.*;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.Wrapper.wrapper;
import static com.metreeca.rest.assets.Engine.*;


/**
 * Model-driven resource relator.
 *
 * <p>Performs:</p>
 *
 * <ul>
 *
 * <li>shape-based {@linkplain Engine#throttler(Object, Object...) authorization}, considering shapes enabled by the
 * {@linkplain Guard#Relate} task and {@linkplain Guard#Target}/{@linkplain Guard#Digest} areas, when operating on
 * {@linkplain Request#collection() collections}, or the {@linkplain Guard#Detail} area, when operating on other
 * resources;</li>
 *
 * <li>engine assisted resource {@linkplain Engine#relate(Request) retrieval};</li>
 *
 * <li>engine-assisted response payload {@linkplain JSONLDFormat#trim(org.eclipse.rdf4j.model.IRI, Shape, JsonObject)
 * trimming}, considering shapes
 * as above.</li>
 *
 * </ul>
 *
 * <p>All operations are executed inside a single {@linkplain Engine engine transaction}.</p>
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

		final Engine engine=asset(engine());

		delegate(engine.wrap(((Handler)engine::relate)

				.with(trimmer())

				.with(wrapper(Request::collection,
						throttler(Relate, Target, Digest),
						throttler(Relate, Detail)
				))

		));
	}

}
