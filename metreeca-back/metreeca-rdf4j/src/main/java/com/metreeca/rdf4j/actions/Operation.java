/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.rdf4j.actions;

import com.metreeca.rdf4j.services.Graph;
import com.metreeca.rest.services.Logger;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.impl.SimpleDataset;

import java.util.*;

import static com.metreeca.rest.Context.service;


public abstract class Operation<T extends Operation<T>> {

	private Graph graph;

	private String base;
	private Boolean inferred;
	private Integer timeout;

	private SimpleDataset dataset;

	private final Map<String, Value> bindings=new HashMap<>();

	private final Logger logger=service(Logger.logger());


	@SuppressWarnings("unchecked") private T self() {
		return (T)this;
	}


	protected Graph graph() {
		return graph != null ? graph : (graph=service(Graph.graph()));
	}

	protected String base() {
		return base;
	}

	protected Logger logger() {
		return logger;
	}


	private SimpleDataset dataset() {
		return dataset != null ? dataset : (dataset=new SimpleDataset());
	}


	protected <O extends org.eclipse.rdf4j.query.Operation> O configure(final O operation) {

		if ( dataset != null ) { operation.setDataset(dataset); }
		if ( inferred != null ) { operation.setIncludeInferred(inferred); }
		if ( timeout != null ) { operation.setMaxExecutionTime(timeout); }

		bindings.forEach(operation::setBinding);

		return operation;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public T graph(final Graph graph) {

		if ( graph == null ) {
			throw new NullPointerException("null graph");
		}

		this.graph=graph;

		return self();
	}


	public T base(final String base) {

		if ( base == null ) {
			throw new NullPointerException("null base");
		}

		this.base=base;

		return self();
	}

	public T inferred(final boolean inferred) {

		this.inferred=inferred;

		return self();
	}

	public T timeout(final int timeout) {

		this.timeout=timeout;

		return self();
	}


	public T dflt(final IRI... graphs) {

		if ( graphs == null || Arrays.stream(graphs).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null graphs");
		}

		Arrays.stream(graphs).forEach(g -> dataset().addDefaultGraph(g));

		return self();
	}

	public T named(final IRI... graphs) {

		if ( graphs == null || Arrays.stream(graphs).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null graphs");
		}

		Arrays.stream(graphs).forEach(g -> dataset().addNamedGraph(g));

		return self();
	}

	public T remove(final IRI... graphs) {

		if ( graphs == null || Arrays.stream(graphs).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null graphs");
		}

		Arrays.stream(graphs).forEach(g -> dataset().addDefaultRemoveGraph(g));

		return self();
	}

	public T insert(final IRI graph) {

		if ( graph == null ) {
			throw new NullPointerException("null graph");
		}

		dataset().setDefaultInsertGraph(graph);

		return self();
	}


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
