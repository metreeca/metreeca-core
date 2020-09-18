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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.query.impl.SimpleDataset;

import java.util.*;


/**
 * SPARQL-based processing action.
 * 
 * <p>Executes SPARQL queries/updates against a {@linkplain #graph(Graph) target graph}.</p>
 *
 * @param <T> the type of this action for fluent methods
 */
public abstract class Action<T extends Action<T>> {

    private Graph graph=Context.asset(Graph.graph());

    private String base;
    private Boolean inferred;
    private Integer timeout;

    private SimpleDataset dataset;

    private final Map<String, Value> bindings=new HashMap<>();


    @SuppressWarnings("unchecked") private T self() {
        return (T)this;
    }


    /**
     * Retrieves the target graph.
     *
     * @return the target graph of this action.
     */
    protected Graph graph() {
        return graph;
    }

    /**
     * Retrieves the base IRI.
     *
     * @return the (possibly null) base IRI of this action.
     */
    protected String base() {
        return base;
    }


    private SimpleDataset dataset() {
        return dataset != null ? dataset : (dataset=new SimpleDataset());
    }


    /**
     * Configures a SPARQL operations.
     *
     * <p>Set the following parameters, if actually modified through one of corresponding public setter method:</p>
     *
     * <ul>
     *     <li>{@linkplain Operation#setDataset(Dataset) dataset};</li>
     *     <li>{@linkplain Operation#setIncludeInferred(boolean) inferred};</li>
     *     <li>{@linkplain Operation#setMaxExecutionTime(int) timeout};</li>
     *     <li>{@linkplain Operation#setBinding(String, Value) bindings}.</li>
     * </ul>
     *
     * @param operation the operation to be configured
     * @param <O>       tye type of the {@code operation}
     *
     * @return the configured {@code operation}
     *
     * @throws NullPointerException if {@code operation} is null
     */
    protected <O extends Operation> O configure(final O operation) {

        if ( operation == null ) {
            throw new NullPointerException("null operation");
        }

        if ( dataset != null ) { operation.setDataset(dataset); }
        if ( inferred != null ) { operation.setIncludeInferred(inferred); }
        if ( timeout != null ) { operation.setMaxExecutionTime(timeout); }

        bindings.forEach(operation::setBinding);

        return operation;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Configures the target graph (default to the {@linkplain Graph#graph() shared graph service}).
     *
     * @param graph the target graph for this action
     *
     * @return this action
     *
     * @throws NullPointerException if {@code graph} is null
     */
    public T graph(final Graph graph) {

        if ( graph == null ) {
            throw new NullPointerException("null graph");
        }

        this.graph=graph;

        return self();
    }


    /**
     * Configures the base IRI (default to code {@code null}).
     *
     * @param base the base IRI for this action
     *
     * @return this action
     *
     * @throws NullPointerException if {@code base} is null
     */
    public T base(final String base) {

        if ( base == null ) {
            throw new NullPointerException("null base");
        }

        this.base=base;

        return self();
    }

    /**
     * Configures the inferred flag (default to the default of the {@linkplain #graph(Graph) target graph}).
     *
     * @param inferred {@code true} if inferred statements are to be considered in the evaulation of this action;
     *                 {@code false}, otherwise
     *
     * @return this action
     */
    public T inferred(final boolean inferred) {

        this.inferred=inferred;

        return self();
    }

    /**
     * Configures the execution timeout (default to the default of the {@linkplain #graph(Graph) target graph}).
     *
     * @param timeout the timeout for the execution of this action (in seconds); zero for unlimited execution time
     *
     * @return this action
     *
     * @throws IllegalArgumentException if {@code timeout} is negative
     */
    public T timeout(final int timeout) {

        if ( timeout < 0 ) {
            throw new IllegalArgumentException("negative timeout");
        }

        this.timeout=timeout;

        return self();
    }


    /**
     * Configures the default graphs (default to the default of the {@linkplain #graph(Graph) target graph}).
     *
     * @param graphs the default graphs for this action
     *
     * @return this action
     *
     * @throws NullPointerException if {@code graphs} is null or contains null values
     */
    public T dflt(final IRI... graphs) {

        if ( graphs == null || Arrays.stream(graphs).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null graphs");
        }

        Arrays.stream(graphs).forEach(g -> dataset().addDefaultGraph(g));

        return self();
    }

    /**
     * Configures the named graphs (default to the default of the {@linkplain #graph(Graph) target graph}).
     *
     * @param graphs the named graphs for this action
     *
     * @return this action
     *
     * @throws NullPointerException if {@code graphs} is null or contains null values
     */
    public T named(final IRI... graphs) {

        if ( graphs == null || Arrays.stream(graphs).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null graphs");
        }

        Arrays.stream(graphs).forEach(g -> dataset().addNamedGraph(g));

        return self();
    }

    /**
     * Configures the default remove graphs (default to the default of the {@linkplain #graph(Graph) target graph}).
     *
     * @param graphs the default remove graphs for this action
     *
     * @return this action
     *
     * @throws NullPointerException if {@code graphs} is null or contains null values
     */
    public T remove(final IRI... graphs) {

        if ( graphs == null || Arrays.stream(graphs).anyMatch(Objects::isNull) ) {
            throw new NullPointerException("null graphs");
        }

        Arrays.stream(graphs).forEach(g -> dataset().addDefaultRemoveGraph(g));

        return self();
    }

    /**
     * Configures the default insert graph (default to the default of the {@linkplain #graph(Graph) target graph}).
     *
     * @param graph the default insert graph for this action
     *
     * @return this action
     *
     * @throws NullPointerException if {@code graph} is null
     */
    public T insert(final IRI graph) {

        if ( graph == null ) {
            throw new NullPointerException("null graph");
        }

        dataset().setDefaultInsertGraph(graph);

        return self();
    }


    /**
     * Configures a binding.
     *
     * @param name  the name of the variable to be bound
     * @param value the value to be bound to the {@code name} variable
     *
     * @return this action
     *
     * @throws NullPointerException if either {@code name} or {@code value} is null
     */
    public T binding(final String name, final Value value) {

        if ( name == null ) {
            throw new NullPointerException("null name");
        }

        if ( value == null ) {
            throw new NullPointerException("null value");
        }

        this.bindings.put(name, value);

        return self();
    }

}
