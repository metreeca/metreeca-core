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

package com.metreeca.rdf.actions;

import com.metreeca.json.Frame;

import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.Values.iri;


/**
 * Linked data resource lookup.
 *
 * <p>Maps linked data resource IRIs to RDF descriptions retrieved by dereferencing them; unknown resources are mapped
 * to empty descriptions.</p>
 */
public final class Lookup implements Function<String, Frame> {

	@Override public Frame apply(final String iri) {
		return Optional.of(iri)

				.map(new Retrieve())

				.map(model -> frame(iri(iri), model))

				.orElseGet(() -> frame(iri(iri)));
	}

}
