/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
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
