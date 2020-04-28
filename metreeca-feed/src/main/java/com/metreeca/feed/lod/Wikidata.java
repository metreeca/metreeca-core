/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.feed.lod;

import com.metreeca.feed.Feed;
import com.metreeca.feed._services.Limit;
import com.metreeca.feed.net.Fetch;
import com.metreeca.feed.net.Parse;
import com.metreeca.feed.net.Query;
import com.metreeca.feed.text.Text;
import com.metreeca.rdf.Cell;
import com.metreeca.rest.formats.JSONFormat;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.rdf.Cell.cell;
import static com.metreeca.rdf.Values.iri;
import static com.metreeca.rdf.Values.literal;
import static com.metreeca.rest.formats.JSONFormat.json;
import static java.lang.Double.parseDouble;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;


/**
 * Wikidata entity search.
 *
 * @see <a href="https://www.wikidata.org/wiki/Special:ApiSandbox#action=wbsearchentities&format=json&search=&language=en">Wikidata API sandbox</a>
 */
public final class Wikidata implements Function<String, Feed<Cell>> {

	public static final String WD="http://www.wikidata.org/entity/";
	public static final String WDT="http://www.wikidata.org/prop/direct/";
	public static final String WIKIBASE="http://wikiba.se/ontology#";

	public static final IRI ITEM=iri(WIKIBASE, "Item");
	public static final IRI PROPERTY=iri(WIKIBASE, "Property");

	public static final IRI P31=iri(WDT, "P31"); // instance of
	public static final IRI P279=iri(WDT, "P279"); // subclass of
	public static final IRI P1647=iri(WDT, "P1647"); // subproperty of
	public static final IRI P625=iri(WDT, "P625");

	public static final IRI Q4167410=iri(WD, "Q4167410"); // Wikimedia disambiguation page

	public static final Collection<Value> Names=unmodifiableSet(new HashSet<>(asList(RDFS.LABEL, SKOS.ALT_LABEL)));


	private static final Pattern CoordPattern=Pattern.compile("[-+]?(?:\\d*\\.)?\\d+");
	private static final Pattern PointPattern=Pattern.compile("Point\\(("+CoordPattern+")\\s+("+CoordPattern+")\\)");

	private static final Limit<String> limit=new Limit<>(2);


	public static Optional<Map.Entry<Double, Double>> point(final String literal) {
		return Optional.of(literal)
				.map(PointPattern::matcher)
				.filter(Matcher::matches)
				.map(matcher -> new AbstractMap.SimpleImmutableEntry<>(
						parseDouble(matcher.group(2)),
						parseDouble(matcher.group(1))
				));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Feed<Cell> apply(final String query) {
		return Feed.of(limit.apply(query))

				.flatMap(new Text<String>

						(
								"https://www.wikidata.org/w/api.php"
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


	private Cell match(final JsonObject match) {
		return cell(iri(match.getString("concepturi")))
				.insert(RDFS.LABEL, literal(string(match.get("label"))))
				.insert(RDFS.COMMENT, literal(string(match.get("description"))))
				.get();
	}

	private String string(final JsonValue value) {
		return value instanceof JsonString ? ((JsonString)value).getString() : null;
	}

}
