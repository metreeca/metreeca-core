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

package com.metreeca.form;

import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.IRI;

import static com.metreeca.form.things.Values.iri;


/**
 * Shape constraints RDF vocabulary.
 */
public final class Spec {

	public static final String Namespace="app://spec.metreeca.com/terms#"; // keep aligned with client


	//// Shape Types ///////////////////////////////////////////////////////////////////////////////////////////////////

	public static final IRI Datatype=Values.iri(Namespace, "Datatype");
	public static final IRI Class=Values.iri(Namespace, "Class");
	public static final IRI MinExclusive=Values.iri(Namespace, "MinExclusive");
	public static final IRI MaxExclusive=Values.iri(Namespace, "MaxExclusive");
	public static final IRI MinInclusive=Values.iri(Namespace, "MinInclusive");
	public static final IRI MaxInclusive=Values.iri(Namespace, "MaxInclusive");
	public static final IRI MinLength=Values.iri(Namespace, "MinLength");
	public static final IRI MaxLength=Values.iri(Namespace, "MaxLength");
	public static final IRI Pattern=Values.iri(Namespace, "Pattern");
	public static final IRI Like=Values.iri(Namespace, "Like");

	public static final IRI MinCount=Values.iri(Namespace, "MinCount");
	public static final IRI MaxCount=Values.iri(Namespace, "MaxCount");
	public static final IRI In=Values.iri(Namespace, "In");
	public static final IRI All=Values.iri(Namespace, "All");
	public static final IRI Any=Values.iri(Namespace, "Any");

	public static final IRI Trait=Values.iri(Namespace, "Trait");
	public static final IRI Virtual=Values.iri(Namespace, "Virtual");

	public static final IRI And=Values.iri(Namespace, "And");
	public static final IRI Or=Values.iri(Namespace, "Or");
	public static final IRI Test=Values.iri(Namespace, "Test");
	public static final IRI When=Values.iri(Namespace, "When");

	public static final IRI Alias=Values.iri(Namespace, "Alias");
	public static final IRI Label=Values.iri(Namespace, "Label");
	public static final IRI Notes=Values.iri(Namespace, "Notes");
	public static final IRI Placeholder=Values.iri(Namespace, "Placeholder");
	public static final IRI Default=Values.iri(Namespace, "Default");
	public static final IRI Hint=Values.iri(Namespace, "Hint");
	public static final IRI Group=Values.iri(Namespace, "Group");


	//// Shape Shorthands ////////////////////////////////////////////////////////////////////////////////// !!! remove?

	public static final IRI required=Values.iri(Namespace, "required");
	public static final IRI optional=Values.iri(Namespace, "optional");
	public static final IRI repeatable=Values.iri(Namespace, "repeatable");
	public static final IRI multiple=Values.iri(Namespace, "multiple");
	public static final IRI only=Values.iri(Namespace, "only");


	//// Shift Types ///////////////////////////////////////////////////////////////////////////////////////////////////

	public static final IRI Step=Values.iri(Namespace, "Step");
	public static final IRI Count=Values.iri(Namespace, "Count");


	//// Shape/Shifts Properties ///////////////////////////////////////////////////////////////////////////////////////

	public static final IRI shape=Values.iri(Namespace, "shape");
	public static final IRI shapes=Values.iri(Namespace, "shapes");

	public static final IRI value=Values.iri(Namespace, "value");
	public static final IRI values=Values.iri(Namespace, "values");

	public static final IRI iri=Values.iri(Namespace, "iri");
	public static final IRI text=Values.iri(Namespace, "text");
	public static final IRI flags=Values.iri(Namespace, "flags");
	public static final IRI limit=Values.iri(Namespace, "limit");

	public static final IRI trait=Values.iri(Namespace, "trait");
	public static final IRI shift=Values.iri(Namespace, "shift");

	public static final IRI step=Values.iri(Namespace, "step");
	public static final IRI inverse=Values.iri(Namespace, "inverse");

	public static final IRI test=Values.iri(Namespace, "test");
	public static final IRI pass=Values.iri(Namespace, "pass");
	public static final IRI fail=Values.iri(Namespace, "fail");


	//// Query Properties //////////////////////////////////////////////////////////////////////////////////////////////

	public static final IRI meta=Values.iri(Namespace, "meta"); // default subject for introspective query results
	public static final IRI path=Values.iri(Namespace, "path");

	public static final IRI stats=Values.iri(Namespace, "stats");
	public static final IRI items=Values.iri(Namespace, "items");

	public static final IRI max=Values.iri(Namespace, "max");
	public static final IRI min=Values.iri(Namespace, "min");
	public static final IRI count=Values.iri(Namespace, "count");


	//// Parametric Axes and Values ////////////////////////////////////////////////////////////////////////////////////

	public static final IRI role=Values.iri(Namespace, "role");
	public static final IRI task=Values.iri(Namespace, "task");
	public static final IRI view=Values.iri(Namespace, "view");
	public static final IRI mode=Values.iri(Namespace, "mode");

	/**
	 * Super user/role.
	 */
	public static final IRI root=Values.iri(Namespace, "root");

	/**
	 * Anonymous user/role.
	 */
	public static final IRI none=Values.iri(Namespace, "none");


	public static final IRI create=Values.iri(Namespace, "create");
	public static final IRI relate=Values.iri(Namespace, "relate");
	public static final IRI update=Values.iri(Namespace, "update");
	public static final IRI delete=Values.iri(Namespace, "delete");
	public static final IRI client=Values.iri(Namespace, "client");
	public static final IRI server=Values.iri(Namespace, "server");

	public static final IRI digest=Values.iri(Namespace, "digest");
	public static final IRI detail=Values.iri(Namespace, "detail");

	public static final IRI verify=Values.iri(Namespace, "verify");
	public static final IRI filter=Values.iri(Namespace, "filter");

	public static final IRI any=Values.iri(Namespace, "any"); // wildcard axis value


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Spec() {}

}
