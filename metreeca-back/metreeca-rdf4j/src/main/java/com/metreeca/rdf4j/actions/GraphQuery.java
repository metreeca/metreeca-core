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

package com.metreeca.rdf4j.actions;

import com.metreeca.rdf4j.assets.Graph;
import com.metreeca.rest.Context;
import com.metreeca.rest.assets.Logger;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.Operation;

import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.rest.assets.Logger.time;
import static org.eclipse.rdf4j.common.iteration.Iterations.asList;
import static org.eclipse.rdf4j.query.QueryLanguage.SPARQL;


/**
 * SPARQL graph query action.
 *
 * <p>Executes SPARQL graph queries against the {@linkplain #graph(Graph) target graph}.</p>
 */
public final class GraphQuery extends Action<GraphQuery> implements Function<String, Stream<Statement>> {

    private final Logger logger=Context.asset(Logger.logger());


    /**
     * Executes a SPARQL graph query.
     *
     * @param query the graph query to be executed
     *
     * @return a stream of statements produced by executing {@code query} against the {@linkplain #graph(Graph)
     * target graph} after {@linkplain #configure(Operation) configuring} it; null or empty queries are silently ignored
     */
    @Override public Stream<Statement> apply(final String query) {
        return query == null || query.isEmpty() ? Stream.empty() : graph().exec(connection -> {
            return time(() -> // statements must be retrieved inside txn

                    asList(configure(connection.prepareGraphQuery(SPARQL, query, base())).evaluate()).parallelStream()

            ).apply((t, v) ->

                    logger.info(this, String.format("executed in <%,d> ms", t))

            );
        });
    }

}
