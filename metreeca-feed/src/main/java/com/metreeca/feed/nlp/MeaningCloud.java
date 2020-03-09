/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.feed.nlp;

import com.metreeca.feed.Feed;
import com.metreeca.feed.net.Fetch;
import com.metreeca.feed.net.Parse;
import com.metreeca.feed.net.Query;
import com.metreeca.feed.text.Text;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import static com.metreeca.rdf.Values.uuid;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.formats.JSONFormat.json;
import static com.metreeca.rest.services.Vault.vault;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.regex.Pattern.LITERAL;


public final class MeaningCloud<V> implements Extractor<V> {

	private static final Pattern WikipediaPattern=Pattern.compile("http://en.wikipedia.org/wiki/", LITERAL);
	private static final String DBpediaReplacement=Matcher.quoteReplacement("http://dbpedia.org/resource/");

	private static String dbpedia(final CharSequence iri) {
		if ( iri == null ) { return null; } else {

			final Matcher matcher=WikipediaPattern.matcher(iri);

			return matcher.lookingAt() ? matcher.replaceAll(DBpediaReplacement) : null;

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Function<V, Instant> time=v -> Instant.now();
	private Function<V, String> text=v -> "";

	private final String key=service(vault()).get("com-meaningcloud").orElse(""); // !!! getter?


	public MeaningCloud<V> time(final Function<V, Instant> time) {

		if ( time == null ) {
			throw new NullPointerException("null time getter");
		}

		this.time=time;

		return this;
	}

	public MeaningCloud<V> text(final Function<V, String> text) {

		if ( text == null ) {
			throw new NullPointerException("null text getter");
		}

		this.text=text;

		return this;
	}


	@Override public Feed<Reference> apply(final V v) {
		return Feed.of(v)

				.flatMap(new Text<V>

						("https://api.meaningcloud.com/topics-2.0"
								+"?key=%{key}"
								+"&lang=en"
								+"&tt=ectmr"
								+"&timeref=%{time}"
								+"&txt=%{text}"
						)

						.parameter("key", key)
						.parameter("time", _v -> Stream.of(ISO_OFFSET_DATE_TIME.format(time.apply(_v).atOffset(UTC))))
						.parameter("text", _v -> Stream.of(text.apply(_v)))

				)

				.tryMap(new Query())
				.tryMap(new Fetch())
				.tryMap(new Parse<>(json()))

				.flatMap(object -> Stream.concat(
						entities(object, "entity_list"),
						entities(object, "concept_list")
				));
	}


	private Stream<Reference> entities(final JsonObject object, final String list) {
		return Optional
				.ofNullable(object.getJsonArray(list))
				.map(Collection::stream)
				.orElseGet(Stream::empty)
				.map(JsonValue::asJsonObject)
				.flatMap(this::entity);

	}

	private Stream<Reference> entity(final JsonObject object) {

		return Optional

				.ofNullable(object.getJsonArray("variant_list"))
				.orElse(JsonValue.EMPTY_JSON_ARRAY)
				.stream()
				.map(JsonValue::asJsonObject)

				.map(variant -> { // :-o beware of setter ordering when using reference values as defaults…

					final Reference reference=new Reference();

					reference.normal=object.getString("form");
					reference.anchor=variant.getString("form");

					reference.offset=Integer.parseInt(variant.getString("inip"));
					reference.length=Integer.parseInt(variant.getString("endp"))-reference.offset;

					reference.weight=Integer.parseInt(object.getString("relevance"))/100.0;

					reference.matter=object.getJsonObject("sementity").getString("type");

					reference.target=Optional
							.ofNullable(object.getJsonArray("semld_list"))
							.orElse(JsonValue.EMPTY_JSON_ARRAY)
							.stream()
							.map(value -> ((JsonString)value).getString())
							.map(MeaningCloud::dbpedia)
							.filter(Objects::nonNull)
							.findFirst()
							.orElseGet(() -> "urn:uuid:"+uuid(reference.matter+"/"+reference.normal));

					return reference;

				});
	}

}
