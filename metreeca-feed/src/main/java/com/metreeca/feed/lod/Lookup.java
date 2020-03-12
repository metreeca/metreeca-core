/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.feed.lod;

import com.metreeca.feed._formats._RDF.Cell;
import com.metreeca.feed.net.Fetch;
import com.metreeca.feed.net.Parse;
import com.metreeca.feed.net.Query;
import com.metreeca.rdf.Values;

import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;

import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.rdf.formats.RDFFormat.rdf;


public final class Lookup implements Function<String, Optional<Cell>> {

	@Override public Optional<Cell> apply(final String iri) {
		return Optional.of(iri)

				.flatMap(new Query(request -> request

						.header("Accept", "text/turtle, application/rdf+xml;q=0.9")

				))

				.flatMap(new Fetch())

				.flatMap(new Parse<>(rdf(codec -> codec

						.set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, false)
						.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, false)
						.set(BasicParserSettings.NORMALIZE_DATATYPE_VALUES, false)

				)))

				.map(model -> new Cell(Values.iri(iri), model));
	}

}