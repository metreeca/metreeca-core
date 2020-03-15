package com.metreeca.feed.lod;

import com.metreeca.feed.Feed;
import com.metreeca.feed._formats._RDF.Cell;
import com.metreeca.feed._services.Limit;
import com.metreeca.feed.net.Fetch;
import com.metreeca.feed.net.Parse;
import com.metreeca.feed.net.Query;
import com.metreeca.feed.text.Text;
import com.metreeca.rest.formats.JSONFormat;

import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.util.function.Function;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import static com.metreeca.rdf.Values.iri;
import static com.metreeca.rdf.Values.literal;
import static com.metreeca.rest.formats.JSONFormat.json;


/**
 * Wikidata entity search.
 *
 * @see <a href="https://www.wikidata.org/wiki/Special:ApiSandbox#action=wbsearchentities&format=json&search=&language=en">Wikidata API sandbox</a>
 */
public final class Wikidata implements Function<String, Feed<Cell>> {

	private static final Limit limit=new Limit(2);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Feed<Cell> apply(final String query) {
		return Feed.of(limit.apply(query))

				.flatMap(new Text<String>

						(
								"https://www.wikidata.org/w/api.php"
										+ "?action=wbsearchentities"
										+ "&format=json"
										+ "&search=%{query}"
										+ "&language=en"
										+ "&limit=50"
						)

						.value("query", query)

				)

				.tryMap(new Query(request -> request.header("Accept", JSONFormat.MIME)))
				.tryMap(new Fetch())
				.tryMap(new Parse<>(json()))

				.flatMap(response -> response.getJsonArray("search").stream()
						.map(JsonValue::asJsonObject)
						.map(this::match)
				);
	}


	private Cell match(final JsonObject match) {
		return new Cell(iri(match.getString("concepturi")))
				.insert(RDFS.LABEL, literal(string(match.get("label"))))
				.insert(RDFS.COMMENT, literal(string(match.get("description"))));
	}

	private String string(final JsonValue value) {
		return value instanceof JsonString ? ((JsonString)value).getString() : null;
	}

}
