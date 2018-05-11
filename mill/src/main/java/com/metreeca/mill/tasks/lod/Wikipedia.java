/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.mill.tasks.lod;

import com.metreeca.mill.Task;
import com.metreeca.mill.tasks.Item;
import com.metreeca.mill.tasks.xml.XQuery;


/**
 * Wikipedia matching task.
 *
 * @see "https://en.wikipedia.org/w/api.php"
 * @see "https://en.wikipedia.org/w/api.php?action=help&modules=query"
 * @see "https://en.wikipedia.org/w/api.php?action=help&modules=query%2Bextracts"
 * @see "https://en.wikipedia.org/w/api.php?action=help&modules=query%2Bpageimages"
 */
public final class Wikipedia extends Matcher<Wikipedia> {

	private String keywords;
	private String thumbsize;


	@Override protected Wikipedia self() { return this; }


	public Wikipedia keywords(final String keywords) {

		this.keywords=keywords;

		return this;
	}

	public Wikipedia thumbsize(final String thumbsize) {

		this.thumbsize=thumbsize;

		return this;
	}


	@Override protected Task service() {
		return new Item()

				.iri("https://en.wikipedia.org/w/api.php"

						+"?action=query"
						+"&redirects="
						+"&format=xml"
						+"&prop=extracts|pageimages"

						+"&exintro="
						+"&explaintext="

						+"&piprop=thumbnail"
						+"&pithumbsize={thumbsize}"

						+"&generator=search"
						+"&gsrwhat=nearmatch"
						+"&gsrsearch={keywords}")

				.parameter("keywords", keywords != null ? keywords : "{keywords}", "")
				.parameter("thumbsize", thumbsize != null ? thumbsize : "{thumbsize}", "256");
	}

	@Override protected Task postprocessor() {
		return new XQuery()

				.transform("(: ;(wikipedia) remove leftovers references from extracts :)\n"
						+"\n"
						+"declare function local:abstract($abstract as xs:string?) as xs:string? {\n"
						+"    replace($abstract, '(\\\\.\\\\s*\\\\^.*)+$', '.')\n"
						+"};\n"
						+"\n"
						+"(: ;(dbpedia) doesn't handle encoded commas :)\n"
						+"\n"
						+"declare function local:dbpedia($url as xs:string?) as xs:string? {\n"
						+"    replace($url, '%2C', ',')\n"
						+"};\n"
						+"\n"
						+"\n"
						+"for $page in //page\n"
						+"let $uri := encode-for-uri(replace(string($page/@title), ' ', '_'))\n"
						+"return\n"
						+"\n"
						+"(: !!! match index or lists :)\n"
						+"\n"
						+"    <Wikipedia>\n"
						+"\t\t<rank rdf:datatype='http://www.w3.org/2001/XMLSchema#integer'>{position()}</rank>\n"
						+"\t\t<title>{string($page/@title)}</title>\n"
						+"\t\t{for $i in $page/extract[string() != ''] return <abstract>{local:abstract(string($i))}</abstract>}\n"
						+"\t\t{for $i in $page/thumbnail return <thumbnail rdf:resource=\"{string($i/@source)}\"/>}\n"
						+"\t\t<wikipedia rdf:resource=\"{concat('http://en.wikipedia.org/wiki/', $uri)}\"/>\n"
						+"\t\t<dbpedia rdf:resource=\"{concat('http://dbpedia.org/resource/', local:dbpedia($uri))}\"/>\n"
						+"    </Wikipedia>\n"
						+"\n"
						+"(: !!! $page/thumbnail {$i/@width}{$i/@height} :)\n");
	}

}
