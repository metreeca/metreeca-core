/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.rdf;

import org.eclipse.rdf4j.model.IRI;

import static com.metreeca.rdf.Values.iri;


/**
 * Shape constraints RDF vocabulary.
 */
public final class Form {

	public static final String Namespace="app://form.metreeca.com/terms#";


	/**
	 * Super user/role.
	 */
	public static final IRI root=iri(Namespace, "root");

	/**
	 * Anonymous user/role.
	 */
	public static final IRI none=iri(Namespace, "none");


	//// Extended Datatypes ////////////////////////////////////////////////////////////////////////////////////////////

	public static final IRI ValueType=iri(Namespace, "value"); // abstract datatype IRI for values
	public static final IRI ResourceType=iri(Namespace, "resource"); // abstract datatype IRI for resources
	public static final IRI LiteralType=iri(Namespace, "literal"); // abstract datatype IRI for literals

	public static final IRI BNodeType=iri(Namespace, "bnode"); // datatype IRI for blank nodes
	public static final IRI IRIType=iri(Namespace, "iri"); // datatype IRI for IRI references


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
