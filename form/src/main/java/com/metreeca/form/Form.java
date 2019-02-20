/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.form;

import org.eclipse.rdf4j.model.IRI;

import static com.metreeca.form.things.Values.iri;


/**
 * Shape constraints RDF vocabulary.
 */
public final class Form {

	public static final String Namespace="app://form.metreeca.com/terms#";


	//// Extended Datatypes ////////////////////////////////////////////////////////////////////////////////////////////

	public static final IRI ValueType=iri(Namespace, "value"); // abstract datatype IRI for values
	public static final IRI ResourceType=iri(Namespace, "resource"); // abstract datatype IRI for resources
	public static final IRI LiteralType=iri(Namespace, "literal"); // abstract datatype IRI for literals

	public static final IRI BNodeType=iri(Namespace, "bnode"); // datatype IRI for blank nodes
	public static final IRI IRIType=iri(Namespace, "iri"); // datatype IRI for IRI references


	//// Shape Metadata ////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Alias annotation.
	 *
	 * <p>The associated {@code xsd:string} value provides an alternate property name for reporting values for
	 * the enclosing shape (e.g. in the context of JSON-based RDF serialization results).</p>
	 */
	public static final IRI Alias=iri(Namespace, "alias");

	/**
	 * Label annotation.
	 *
	 * <p>The associated {@code xsd:string} value provides a human-readable textual label for the enclosing shape.</p>
	 */
	public static final IRI Label=iri(Namespace, "label");

	/**
	 * Textual annotation.
	 *
	 * <p>The associated {@code xsd:string} value provides a human-readable textual description for the enclosing
	 * shape.</p>
	 */
	public static final IRI Notes=iri(Namespace, "notes");

	/**
	 * Placeholder annotation.
	 *
	 * <p>The associated {@code xsd:string} value provides a human-readable textual placeholder for the expected values
	 * of the enclosing shape.</p>
	 */
	public static final IRI Placeholder=iri(Namespace, "placeholder");

	/**
	 * Default value annotation.
	 *
	 * <p>The associated RDF value provides the default for the expected values of enclosing shape.</p>
	 */
	public static final IRI Default=iri(Namespace, "default");

	/**
	 * Hint annotation.
	 *
	 * <p>The associated IRI identifies a resource hinting at possible values for the enclosing shape (e.g. an LDP
	 * container).</p>
	 */
	public static final IRI Hint=iri(Namespace, "hint");

	/**
	 * Group annotation.
	 *
	 * <p>Identifies the enclosing shape as a group for presentation purposes; the associated value provides a
	 * client-dependent suggested representation mode (list, form, tabbed panes, …).</p>
	 */
	public static final IRI Group=iri(Namespace, "group"); // !!! define standard representations hints


	//// Parametric Axes and Values ////////////////////////////////////////////////////////////////////////////////////

	public static final IRI role=iri(Namespace, "role");
	public static final IRI task=iri(Namespace, "task");
	public static final IRI view=iri(Namespace, "view");
	public static final IRI mode=iri(Namespace, "mode");

	/**
	 * Super user/role.
	 */
	public static final IRI root=iri(Namespace, "root");

	/**
	 * Anonymous user/role.
	 */
	public static final IRI none=iri(Namespace, "none");


	public static final IRI create=iri(Namespace, "create");
	public static final IRI relate=iri(Namespace, "relate");
	public static final IRI update=iri(Namespace, "update");
	public static final IRI delete=iri(Namespace, "delete");
	public static final IRI client=iri(Namespace, "client");
	public static final IRI server=iri(Namespace, "server");

	public static final IRI digest=iri(Namespace, "digest");
	public static final IRI detail=iri(Namespace, "detail");

	public static final IRI convey=iri(Namespace, "convey");
	public static final IRI filter=iri(Namespace, "filter");


	//// Query Results Properties //////////////////////////////////////////////////////////////////////////////////////

	public static final IRI stats=iri(Namespace, "stats");
	public static final IRI items=iri(Namespace, "items");

	public static final IRI max=iri(Namespace, "max");
	public static final IRI min=iri(Namespace, "min");
	public static final IRI count=iri(Namespace, "count");
	public static final IRI value=iri(Namespace, "value");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Form() {}

}
