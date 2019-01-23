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


	//// Shape Types ///////////////////////////////////////////////////////////////////////////////////////////////////

	public static final IRI Meta=iri(Namespace, "Meta");
	public static final IRI Datatype=iri(Namespace, "Datatype");
	public static final IRI Class=iri(Namespace, "Class");
	public static final IRI MinExclusive=iri(Namespace, "MinExclusive");
	public static final IRI MaxExclusive=iri(Namespace, "MaxExclusive");
	public static final IRI MinInclusive=iri(Namespace, "MinInclusive");
	public static final IRI MaxInclusive=iri(Namespace, "MaxInclusive");
	public static final IRI MinLength=iri(Namespace, "MinLength");
	public static final IRI MaxLength=iri(Namespace, "MaxLength");
	public static final IRI Pattern=iri(Namespace, "Pattern");
	public static final IRI Like=iri(Namespace, "Like");

	public static final IRI MinCount=iri(Namespace, "MinCount");
	public static final IRI MaxCount=iri(Namespace, "MaxCount");
	public static final IRI In=iri(Namespace, "In");
	public static final IRI All=iri(Namespace, "All");
	public static final IRI Any=iri(Namespace, "Any");

	public static final IRI Trait=iri(Namespace, "Trait");
	public static final IRI Virtual=iri(Namespace, "Virtual");

	public static final IRI And=iri(Namespace, "And");
	public static final IRI Or=iri(Namespace, "Or");
	public static final IRI Test=iri(Namespace, "Test");
	public static final IRI When=iri(Namespace, "When");


	//// Shift Types ///////////////////////////////////////////////////////////////////////////////////////////////////

	public static final IRI Step=iri(Namespace, "Step");
	public static final IRI Count=iri(Namespace, "Count");


	//// Shape/Shifts Properties ///////////////////////////////////////////////////////////////////////////////////////

	public static final IRI shape=iri(Namespace, "shape");
	public static final IRI shapes=iri(Namespace, "shapes");

	public static final IRI value=iri(Namespace, "value");
	public static final IRI values=iri(Namespace, "values");

	public static final IRI iri=iri(Namespace, "iri");
	public static final IRI text=iri(Namespace, "text");
	public static final IRI flags=iri(Namespace, "flags");
	public static final IRI limit=iri(Namespace, "limit");

	public static final IRI trait=iri(Namespace, "trait");
	public static final IRI shift=iri(Namespace, "shift");

	public static final IRI step=iri(Namespace, "step");
	public static final IRI inverse=iri(Namespace, "inverse");

	public static final IRI test=iri(Namespace, "test");
	public static final IRI pass=iri(Namespace, "pass");
	public static final IRI fail=iri(Namespace, "fail");


	//// Query Properties //////////////////////////////////////////////////////////////////////////////////////////////

	public static final IRI meta=iri(Namespace, "meta"); // default subject for introspective query results
	public static final IRI path=iri(Namespace, "path");

	public static final IRI stats=iri(Namespace, "stats");
	public static final IRI items=iri(Namespace, "items");

	public static final IRI max=iri(Namespace, "max");
	public static final IRI min=iri(Namespace, "min");
	public static final IRI count=iri(Namespace, "count");


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

	public static final IRI verify=iri(Namespace, "verify");
	public static final IRI filter=iri(Namespace, "filter");

	/**
	 * Wildcard axis value.
	 */
	public static final IRI any=iri(Namespace, "any");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Form() {}

}
