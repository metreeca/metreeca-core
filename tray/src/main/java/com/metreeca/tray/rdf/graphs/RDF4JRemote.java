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

package com.metreeca.tray.rdf.graphs;

import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Setup;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.repository.http.HTTPRepository;

import java.util.function.Supplier;

import static com.metreeca.tray.Tray.tool;


public final class RDF4JRemote extends Graph {

	public static final Supplier<Graph> Factory=() -> {

		final Setup setup=tool(Setup.Factory);

		final String url=setup.get("graph.remote.url")
				.orElseThrow(() -> new IllegalArgumentException("missing remote URL property"));

		final String usr=setup.get("graph.remote.usr", "");
		final String pwd=setup.get("graph.remote.pwd", "");

		return new RDF4JRemote(url, usr, pwd);
	};


	public RDF4JRemote(final String url, final String usr, final String pwd) {
		super("RDF4J Remote Store", IsolationLevels.SERIALIZABLE, () -> {

			if ( url == null ) {
				throw new NullPointerException("null url");
			}

			if ( usr == null ) {
				throw new NullPointerException("null usr");
			}

			if ( pwd == null ) {
				throw new NullPointerException("null pwd");
			}

			final HTTPRepository repository=new HTTPRepository(url);

			if ( !usr.isEmpty() || !pwd.isEmpty() ) {
				repository.setUsernameAndPassword(usr, pwd);
			}

			return repository;

		});
	}

}
