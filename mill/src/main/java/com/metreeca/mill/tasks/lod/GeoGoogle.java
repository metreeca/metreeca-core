/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.mill.tasks.lod;

import com.metreeca.mill.Task;
import com.metreeca.mill.tasks.Item;
import com.metreeca.mill.tasks.xml.XSLT;
import com.metreeca.spec.things.Transputs;


/**
 * Google geocoding task.
 *
 * @see "https://developers.google.com/maps/documentation/geocoding/intro"
 */
public final class GeoGoogle extends Matcher<GeoGoogle> {

	private String address;

	private String zip;
	private String locality;
	private String country;

	private String key;


	@Override protected GeoGoogle self() { return this; }


	public GeoGoogle address(final String address) {

		this.address=address;

		return this;
	}

	public GeoGoogle zip(final String zip) {

		this.zip=zip;

		return this;
	}

	public GeoGoogle locality(final String locality) {

		this.locality=locality;

		return this;
	}

	public GeoGoogle country(final String country) {

		this.country=country;

		return this;
	}

	/*
	 * Configures the Google application API key for the geocoding request.
	 *
	 * @param key
	 *
	 * @return this task
	 *
	 * @see "https://console.developers.google.com/flows/enableapi?apiid=geocoding_backend&keyType=SERVER_SIDE&reusekey=true"
	 */
	public GeoGoogle key(final String key) {

		this.key=key;

		return this;
	}


	@Override protected Task service() {
		return new Item()

				.iri("https://maps.googleapis.com/maps/api/geocode/xml"

						+"?address={address}"
						+"&components=postal_code:{zip}|locality:{locality}|country:{country}"

						+"&key={key}"
				)

				.parameter("address", address != null ? address : "{address}", "")

				.parameter("zip", zip != null ? zip : "{zip}", "")
				.parameter("locality", locality != null ? locality : "{locality}", "")
				.parameter("country", country != null ? country : "{country}", "")

				.parameter("key", key != null ? key : "{key}", "");
	}

	@Override protected Task postprocessor() {

		// !!! test for error message // <status>???</status>


		return new XSLT()

				.transform(Transputs.text(GeoNames.class, "GeoGoogle.xsl"));
	}
}
