package com.metreeca.feed.lod;

import com.metreeca.feed.Feed;
import com.metreeca.feed._formats._RDF.Cell;
import com.metreeca.feed._services.Limit;
import com.metreeca.feed.net.Fetch;
import com.metreeca.feed.net.Parse;
import com.metreeca.feed.net.Query;
import com.metreeca.feed.text.Text;
import com.metreeca.rest.formats.JSONFormat;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.util.function.Function;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import static com.metreeca.rdf.Values.iri;
import static com.metreeca.rdf.Values.literal;
import static com.metreeca.rest.formats.JSONFormat.json;


/**
 * DBpedia Lookup service.
 *
 * @param <V>
 *
 * @see <a href="https://github.com/dbpedia/lookup">DBpedia Lookup</a>
 */
public final class DBpedia<V> implements Function<V, Feed<Cell>> {

	private static final Limit limit=new Limit(2);

	private Function<V, String> text=v -> "";


	public DBpedia<V> text(final Function<V, String> text) {

		if ( text == null ) {
			throw new NullPointerException("null text getter");
		}

		this.text=text;

		return this;
	}


	@Override public Feed<Cell> apply(final V v) {
		return Feed.of(limit.apply(v))

				.flatMap(new Text<V>

						(
								"http://lookup.dbpedia.org/api/search/KeywordSearch"
										+"?QueryString=%{query}"
										+"&QueryClass"
										+"&MaxHits=10"
						)

						.value("query", text)

				)

				.tryMap(new Query(request -> request.header("Accept", JSONFormat.MIME)))
				.tryMap(new Fetch())
				.tryMap(new Parse<>(json()))

				.flatMap(response -> response.getJsonArray("results").stream()
						.map(JsonValue::asJsonObject)
						.map(this::result)
				);
	}


	private Cell result(final JsonObject result) {
		return new Cell(iri(result.getString("uri")))
				.insert(RDFS.LABEL, literal(string(result.get("label"))))
				.insert(RDFS.COMMENT, literal(string(result.get("description"))))
				.insert(RDF.TYPE, result.getJsonArray("classes").stream().map(clazz ->
						iri(clazz.asJsonObject().getString("uri"))
				));
	}

	private String string(final JsonValue value) {
		return value instanceof JsonString ? ((JsonString)value).getString() : null;
	}

}
