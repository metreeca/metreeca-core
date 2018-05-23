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
import com.metreeca.tray.IO;

import java.util.Objects;
import java.util.regex.Pattern;


/**
 * Geonames matching task.
 *
 * @see "http://www.geonames.org/export/geonames-search.html"
 */
public final class GeoNames extends Matcher<GeoNames> {

	private static final Pattern JunkPattern=Pattern.compile("\\s*\\([^)]*\\)");


	// !!! review defaults (can't include parameter if empty…)

	private String name;
	private String keywords;
	private String featureClass;
	private String featureCode;
	private String country;

	private Boolean fuzzy;
	private int limit;

	private String user;


	@Override protected GeoNames self() { return this; }


	public GeoNames name(final String name) {

		this.name=name;

		return this;
	}

	public GeoNames keywords(final String keywords) {

		this.keywords=keywords == null ? null : JunkPattern.matcher(keywords).replaceAll("");

		return this;
	}

	/*
	 * @param clazz
	 *
	 * @return
	 *
	 * @see "http://www.geonames.org/export/codes.html"
	 */
	public GeoNames featureClass(final String clazz) { // !!! support multiple classes

		if ( clazz == null ) {
			throw new NullPointerException("null clazz");
		}

		this.featureClass=clazz;

		return this;
	}

	/*
	 * @param code
	 *
	 * @return
	 *
	 * @see "http://www.geonames.org/export/codes.html"
	 */
	public GeoNames featureCode(final String code) { // !!! support multiple codes

		if ( code == null ) {
			throw new NullPointerException("null code");
		}

		this.featureCode=code;

		return this;
	}

	/*
	 * @param code
	 *
	 * @return
	 *
	 * @see "http://www.geonames.org/export/codes.html"
	 */
	public GeoNames country(final String country) { // !!! support multiple codes

		if ( country == null ) {
			throw new NullPointerException("null country");
		}

		this.country=country;

		return this;
	}

	public GeoNames fuzzy(final Boolean fuzzy) {

		this.fuzzy=fuzzy;

		return this;
	}

	public GeoNames limit(final int limit) {

		if ( limit < 0 ) {
			throw new IllegalArgumentException("illegal limit ["+limit+"]");
		}

		this.limit=limit;

		return this;
	}

	public GeoNames user(final String user) {

		if ( user == null ) {
			throw new NullPointerException("null user");
		}

		this.user=user;

		return this;
	}


	@Override protected Task service() {
		return new Item()

				.iri("http://api.geonames.org/search"

						//+"?name={name}" // !!! breaks if left empty
						+"?q={keywords}"
						+"&fuzzy={fuzzy}" // !!! breaks if left empty

						+"&featureClass={featureClass}"
						+"&featureCode={featureCode}"
						+"&country={country}" // !!! breaks if left empty?

						+"&style=FULL"
						+"&maxRows={limit}"

						+"&username={user}")

				.parameter("name", name != null ? name : "{name}", "")
				.parameter("keywords", keywords != null ? keywords : "{keywords}", "")
				.parameter("fuzzy", Objects.equals(fuzzy, Boolean.TRUE) ? "1.0" : Objects.equals(fuzzy, Boolean.FALSE) ? "0.0" : "{fuzzy}", "0.6")

				.parameter("featureClass", featureClass != null ? featureClass : "{featureClass}", "")
				.parameter("featureCode", featureCode != null ? featureCode : "{featureCode}", "")
				.parameter("country", country != null ? country : "{country}", "")

				.parameter("limit", limit > 0 ? String.valueOf(limit) : "{limit}", "")

				.parameter("user", user != null ? user : "{user}", "demo");
	}

	@Override protected Task postprocessor() {

		// !!! test for error message (e.g. illegal user name)
		// geonames>
		//  <status message="invalid feature class " value="14"/>
		// </geonames>

		return new XSLT()

				.transform(IO.text(GeoNames.class, "GeoNames.xsl"));
	}

}
