/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rdf4j.actions;

import com.metreeca.rdf4j.services.Graph;
import com.metreeca.rest.services.Logger;

import org.eclipse.rdf4j.query.Operation;

import java.util.function.Consumer;

import static com.metreeca.rest.Toolbox.service;
import static com.metreeca.rest.services.Logger.time;

import static org.eclipse.rdf4j.query.QueryLanguage.SPARQL;

import static java.lang.String.format;

/**
 * SPARQL update action.
 *
 * <p>Executes SPARQL updates against the {@linkplain #graph(Graph) target graph}.</p>
 */
public final class Update extends Action<Update> implements Consumer<String> {

	private final Logger logger=service(Logger.logger());


	/**
	 * Executes a SPARQL tuple query.
	 *
	 * @param update the update to be executed against the {@linkplain #graph(Graph) target graph} after {@linkplain
	 *               #configure(Operation) configuring} it; null or empty queries are silently ignored
	 */
	@Override public void accept(final String update) {
		if ( update != null && !update.isEmpty() ) {
			graph().update(connection -> time(() ->

					configure(connection.prepareUpdate(SPARQL, update, base())).execute()

			).apply(t ->

					logger.info(this, format("executed in <%,d> ms", t))

			));
		}
	}

}
