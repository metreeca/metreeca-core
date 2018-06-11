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

package com.metreeca.link;

import org.eclipse.rdf4j.model.IRI;

import static com.metreeca.spec.things.Values.iri;


/**
 * Linked data server RDF vocabulary.
 */
public final class Link {

	public static final String Namespace="app://link.metreeca.com/terms#"; // keep aligned with client


	//// Extended LDP Resource Types ///////////////////////////////////////////////////////////////////////////////////

	public static final IRI ShapedContainer=iri(Namespace, "ShapedContainer");
	public static final IRI ShapedResource=iri(Namespace, "ShapedResource");


	//// Linked Data Tasks /////////////////////////////////////////////////////////////////////////////////////////////

	public static final IRI create=iri(Namespace, "create");
	public static final IRI relate=iri(Namespace, "relate");
	public static final IRI update=iri(Namespace, "update");
	public static final IRI delete=iri(Namespace, "delete");



	//// Audit Trail Records ///////////////////////////////////////////////////////////////////////////////////////////

	public static final IRI Trace=iri(Namespace, "Trace"); // marker RDF class

	public static final IRI item=iri(Namespace, "item"); // operation target resource (IRI)
	public static final IRI task=iri(Namespace, "task"); // operation type tag (Value)
	public static final IRI user=iri(Namespace, "user"); // operation actor (IRI)
	public static final IRI time=iri(Namespace, "time"); // operation ms-precision timestamp (xsd:dataTime)


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Link() {} // a utility class

}
