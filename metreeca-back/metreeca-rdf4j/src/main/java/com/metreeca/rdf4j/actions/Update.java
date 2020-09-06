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

import com.metreeca.core.Context;
import com.metreeca.core.assets.Logger;
import com.metreeca.rdf4j.assets.Graph;

import org.eclipse.rdf4j.query.Operation;

import java.util.function.Consumer;

import static com.metreeca.core.assets.Logger.time;
import static org.eclipse.rdf4j.query.QueryLanguage.SPARQL;

/**
 * SPARQL update action.
 *
 * <p>Executes SPARQL updates against the {@linkplain #graph(Graph) target graph}.</p>
 */
public final class Update extends Action<Update> implements Consumer<String> {

    private final Logger logger=Context.asset(Logger.logger());


    /**
     * Executes a SPARQL tuple query.
     *
     * @param update the update to be executed against the {@linkplain #graph(Graph) target graph} after
     * {@linkplain #configure(Operation) configuring} it; null or empty queries are silently ignored
     */
    @Override public void accept(final String update) {
        if ( update != null && !update.isEmpty() ) {
            graph().exec(connection -> {
                time(() ->

                        configure(connection.prepareUpdate(SPARQL, update, base())).execute()

                ).apply(t ->

                        logger.info(this, String.format("executed in <%,d> ms", t))

                );
            });
        }
    }

}
