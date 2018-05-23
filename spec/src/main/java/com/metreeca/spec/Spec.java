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

package com.metreeca.spec;

import org.eclipse.rdf4j.model.IRI;

import static com.metreeca.spec.things.Values.iri;


/**
 * Shape constraints RDF vocabulary.
 */
public final class Spec {

	public static final String Namespace="tag:com.metreeca,2016:spec/terms#"; // keep aligned with client // !!! review


	//// Shape Types ///////////////////////////////////////////////////////////////////////////////////////////////////

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

	public static final IRI Alias=iri(Namespace, "Alias");
	public static final IRI Label=iri(Namespace, "Label");
	public static final IRI Notes=iri(Namespace, "Notes");
	public static final IRI Placeholder=iri(Namespace, "Placeholder");
	public static final IRI Default=iri(Namespace, "Default");
	public static final IRI Hint=iri(Namespace, "Hint");
	public static final IRI Group=iri(Namespace, "Group");


	//// Shape Shorthands ////////////////////////////////////////////////////////////////////////////////// !!! remove?

	public static final IRI required=iri(Namespace, "required");
	public static final IRI optional=iri(Namespace, "optional");
	public static final IRI repeatable=iri(Namespace, "repeatable");
	public static final IRI multiple=iri(Namespace, "multiple");
	public static final IRI only=iri(Namespace, "only");


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

	public static final IRI meta=iri(Namespace, "meta"); // default subject for query results // !!! review
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

	public static final IRI any=iri(Namespace, "any"); // wildcard axis value


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Spec() {}

}
