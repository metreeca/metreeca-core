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

package com.metreeca.gate;

import org.eclipse.rdf4j.model.IRI;

import static com.metreeca.form.things.Values.iri;


/**
 * Identity and access management RDF vocabulary and utilities.
 */
public final class Gate {

	public static final String Namespace="app://gate.metreeca.com/terms#";

	public static final IRI secret=iri(Namespace, "secret");
	public static final IRI access=iri(Namespace, "access");
	public static final IRI action=iri(Namespace, "digest");

	public static final IRI item=iri(Namespace, "item"); // IRI
	public static final IRI task=iri(Namespace, "task"); // Value
	public static final IRI user=iri(Namespace, "user"); // IRI
	public static final IRI time=iri(Namespace, "time"); // xsd:dataTime with ms-precision
	public static final IRI code=iri(Namespace, "code"); // xsd:integer | xsd:boolean
	public static final IRI hash=iri(Namespace, "hash"); // xsd:string


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Gate() {} // utility

}
