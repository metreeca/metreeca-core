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

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.util.function.Function;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import static com.metreeca.rdf.Cell.cell;
import static com.metreeca.rdf.Values.iri;
import static com.metreeca.rdf.Values.literal;
import static com.metreeca.rest.formats.JSONFormat.json;


/**
 * DBpedia Lookup service.
 *
 * @see <a href="https://github.com/dbpedia/lookup">DBpedia Lookup</a>
 */
public final class DBpedia implements Function<String, Feed<Cell>> {

	private static final Limit<String> limit=new Limit<>(2);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Feed<Cell> apply(final String query) {
		return Feed.of(limit.apply(query))

				.flatMap(new Text<String>

						(
								"http://lookup.dbpedia.org/api/search/KeywordSearch"
										+"?QueryString=%{query}"
										+"&QueryClass"
										+"&MaxHits=10"
						)

						.value("query", query)

				)

				.optMap(new Query(request -> request.header("Accept", JSONFormat.MIME)))
				.optMap(new Fetch())
				.optMap(new Parse<>(json()))

				.flatMap(response -> response.getJsonArray("results").stream()
						.map(JsonValue::asJsonObject)
						.map(this::result)
				);
	}


	private Cell result(final JsonObject result) {
		return cell(iri(result.getString("uri")))

				.insert(RDFS.LABEL, literal(string(result.get("label"))))
				.insert(RDFS.COMMENT, literal(string(result.get("description"))))
				.insert(RDF.TYPE, result.getJsonArray("classes").stream().map(clazz ->
						iri(clazz.asJsonObject().getString("uri"))
				))

				.get();
	}

	private String string(final JsonValue value) {
		return value instanceof JsonString ? ((JsonString)value).getString() : null;
	}

}
