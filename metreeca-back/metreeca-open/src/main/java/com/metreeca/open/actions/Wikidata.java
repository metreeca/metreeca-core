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

package com.metreeca.open.actions;

import com.metreeca.json.Frame;
import com.metreeca.rdf4j.assets.Graph;
import com.metreeca.rest.Xtream;
import com.metreeca.rest.actions.*;
import com.metreeca.rest.formats.JSONFormat;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

import javax.json.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.Values.literal;
import static com.metreeca.rest.formats.JSONFormat.json;
import static java.lang.Double.parseDouble;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;


/**
 * Wikidata entity search.
 *
 * @see
 * <a href="https://www.wikidata.org/wiki/Special:ApiSandbox#action=wbsearchentities&format=json&search=&language=en">Wikidata
 * API sandbox</a>
 */
public final class Wikidata implements Function<String, Xtream<Frame>> {

	public static final String WD="http://www.wikidata.org/entity/";
	public static final String WDP="http://www.wikidata.org/prop/";
	public static final String WDT="http://www.wikidata.org/prop/direct/";
	public static final String WIKIBASE="http://wikiba.se/ontology#";

	public static final IRI ITEM=iri(WIKIBASE, "Item");
	public static final IRI PROPERTY=iri(WIKIBASE, "Property");
	public static final IRI SITELINKS=iri(WIKIBASE, "sitelinks");

	public static final IRI P31=iri(WDT, "P31"); // instance of
	public static final IRI P279=iri(WDT, "P279"); // subclass of
	public static final IRI P1647=iri(WDT, "P1647"); // subproperty of
	public static final IRI P625=iri(WDT, "P625"); // coordinate location
	public static final IRI P1549=iri(WDT, "P1549"); // demonym

	public static final IRI Q4167410=iri(WD, "Q4167410"); // Wikimedia disambiguation page

	public static final Collection<Value> Names=unmodifiableSet(new HashSet<>(asList(RDFS.LABEL, SKOS.ALT_LABEL)));


	private static final Pattern CoordPattern=Pattern.compile("[-+]?(?:\\d*\\.)?\\d+");
	private static final Pattern PointPattern=Pattern.compile("Point\\(("+CoordPattern+")\\s+("+CoordPattern+")\\)");

	private static final Limit<String> limit=new Limit<>(2);


	public static Graph Graph() {
		return new Graph(new SPARQLRepository("https://query.wikidata.org/sparql"));
	}


	public static Optional<Map.Entry<Double, Double>> point(final String literal) {
		return Optional.ofNullable(literal)
				.map(PointPattern::matcher)
				.filter(Matcher::matches)
				.map(matcher -> new SimpleImmutableEntry<>(
						parseDouble(matcher.group(2)),
						parseDouble(matcher.group(1))
				));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Xtream<Frame> apply(final String query) {
		return Xtream.of(limit.apply(query))

				.flatMap(new Fill<>()

						.model("https://www.wikidata.org/w/api.php"
								+"?action=wbsearchentities"
								+"&format=json"
								+"&search=%{query}"
								+"&language=en"
								+"&limit=max"
						)

						.value("query", query)

				)

				.optMap(new Query(request -> request.header("Accept", JSONFormat.MIME)))
				.optMap(new Fetch())
				.optMap(new Parse<>(json()))

				.flatMap(response -> response.getJsonArray("search").stream()
						.map(JsonValue::asJsonObject)
						.map(this::match)
				);
	}


	private Frame match(final JsonObject match) {
		return frame(iri(match.getString("concepturi")))
				.set(RDFS.LABEL).value(literal(string(match.get("label"))))
				.set(RDFS.COMMENT).value(literal(string(match.get("description"))));
	}

	private String string(final JsonValue value) {
		return value instanceof JsonString ? ((JsonString)value).getString() : null;
	}

}
