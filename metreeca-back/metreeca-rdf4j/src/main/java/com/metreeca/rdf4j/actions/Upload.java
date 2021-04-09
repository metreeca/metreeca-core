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

import org.eclipse.rdf4j.model.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static com.metreeca.rest.Toolbox.service;
import static com.metreeca.rest.services.Logger.logger;
import static com.metreeca.rest.services.Logger.time;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;


/**
 * RDF upload action.
 *
 * <p>Uploads RDF statements to a {@linkplain #graph(Graph) target graph}.</p>
 */
public final class Upload implements Consumer<Collection<Statement>> {

    private static final Resource[] DefaultContexts=new Resource[0];


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Graph graph=service(Graph.graph());

    private Resource[] contexts=DefaultContexts;

    private final AtomicBoolean clear=new AtomicBoolean();
    private final AtomicLong count=new AtomicLong();

	private final Logger logger=service(logger());


    /**
     * Configures the target graph (default to the {@linkplain Graph#graph() shared graph service}).
     *
     * @param graph the target graph for this action
     *
     * @return this action
     *
     * @throws NullPointerException if {@code graph} is null
     */
    public Upload graph(final Graph graph) {

        if ( graph == null ) {
            throw new NullPointerException("null graph");
        }

        this.graph=graph;

        return this;
    }

    /**
     * Configures the target upload contexts (default to the empty array, that is to the default context).
     *
     * @param contexts an array of contexts statements are to be uploaded to
     *
     * @return this action
     *
     * @throws NullPointerException if {@code contexts} is null or contains null values
     */
    public Upload contexts(final Resource... contexts) {

        if ( contexts == null || Arrays.stream(contexts).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null contexts");
        }

        this.contexts=contexts.clone();

        return this;
    }

    /**
     * Configures the clear flag (default to {@code false}).
     *
     * @param clear {@code true} if the target contexts are to be cleared before the first upload performed by this
     *              action; {@code false}, otherwise
     *
     * @return this action
     */
    public Upload clear(final boolean clear) {

        this.clear.set(clear);

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Uploads RDF statements.
     *
     * @param statements a collection of RDF statements to be uploaded to the {@linkplain #graph(Graph) target graph};
     *                   null or empty collections are silently ignored
     */
    @Override public void accept(final Collection<Statement> statements) {
        if ( statements != null && !statements.isEmpty() ) {

	        final String contexts=this.contexts.length == 0 ? "default context" : Arrays.stream(this.contexts)
			        .map(Value::stringValue)
			        .collect(joining(", "));

	        graph.update(connection -> time(() -> {

		        if ( clear.getAndSet(false) ) {

			        connection.clear(this.contexts);

			        logger.info(this, format(
					        "cleared <%s>", contexts
			        ));
		        }

		        if ( !statements.isEmpty() ) {
			        connection.add(statements, this.contexts);
		        }

	        }).apply(t -> logger.info(this, format(
			        "uploaded <%,d / %,d> statements to <%s> in <%,d> ms",
			        statements.size(), count.addAndGet(statements.size()), contexts, t
	        ))));

        }
    }

}
