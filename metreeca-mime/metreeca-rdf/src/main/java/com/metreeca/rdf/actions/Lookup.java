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
import com.metreeca.rest.actions.*;

import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;

import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.Values.iri;
import static com.metreeca.rdf.formats.RDFFormat.rdf;


/**
 * Linked data lookup.
 *
 * <p>Maps linked data resource IRIs to optional RDF descriptions retrieved by dereferencing them.</p>
 */
public final class Lookup implements Function<String, Optional<Frame>> {

	@Override public Optional<Frame> apply(final String iri) {
		return Optional.of(iri)

				.flatMap(new Query(request -> request

						.header("Accept", "text/turtle, application/rdf+xml;q=0.9")

				))

				.flatMap(new Fetch())

				.flatMap(new Parse<>(rdf(codec -> codec

						.set(BasicParserSettings.VERIFY_URI_SYNTAX, false)
						.set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, false)
						.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, false)
						.set(BasicParserSettings.NORMALIZE_DATATYPE_VALUES, false)

				)))

				.map(model -> frame(iri(iri), model));
	}

}
