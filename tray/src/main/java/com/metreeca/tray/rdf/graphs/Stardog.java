/*
 * Copyright © 2013-2017 Metreeca srl. All rights reserved.
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

package com.metreeca.tray.rdf.graphs;

public final class Stardog/* extends Graph*/ {

	//public static final Tool<Graph> Tool=tools -> {
	//
	//	final Setup setup=tools.get(Setup.Tool);
	//
	//	final String url=setup.get("graph.stardog.url")
	//			.orElseThrow(() -> new IllegalArgumentException("missing remote URL property"));
	//
	//	final String usr=setup.get("graph.stardog.usr", "");
	//	final String pwd=setup.get("graph.stardog.pwd", "");
	//
	//	return new Stardog(url, usr, pwd);
	//
	//};
	//
	//
	//public Stardog(final String url, final String usr, final String pwd) {
	//	super("Stardog Community v5", IsolationLevels.READ_COMMITTED, () -> { // only supported level
	//
	//		if ( url == null ) {
	//			throw new IllegalArgumentException("null url");
	//		}
	//
	//		if ( usr == null ) {
	//			throw new IllegalArgumentException("null usr");
	//		}
	//
	//		if ( pwd == null ) {
	//			throw new IllegalArgumentException("null pwd");
	//		}
	//
	//		return new StardogRepository(ConnectionConfiguration
	//				.from(url)
	//				.credentials(usr, pwd)
	//				.reasoning(true));
	//
	//	});
	//}
	//
}
