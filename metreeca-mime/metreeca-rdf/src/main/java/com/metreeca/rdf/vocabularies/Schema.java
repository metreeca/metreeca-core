package com.metreeca.rdf.vocabularies;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Constants for the schema.org vocabulary.
 *
 * @see <a href="https://schema.org/">https://schema.org/</a>
 */
public class Schema { // !!! complete with properties from https://schema.org/Thing

	/**
	 * The schema.org namespace ({@value}).
	 */
	public static final String NAMESPACE="http://schema.org/";

	/**
	 * Recommended prefix for the schema.org namespace ({@value}).
	 */
	public static final String PREFIX="schema";

	/**
	 * An immutable {@link Namespace} constant that represents the schema.org namespace.
	 */
	public static final Namespace NS=new SimpleNamespace(PREFIX, NAMESPACE);


	/** The <a href="https://schema.org/name">https://schema.org/name</a> property. */
	public static final IRI NAME;

	/** The <a href="https://schema.org/description">https://schema.org/name</a> property. */
	public static final IRI DESCRIPTION;


	static {

		final SimpleValueFactory factory=SimpleValueFactory.getInstance();

		NAME=factory.createIRI(NAMESPACE, "name");
		DESCRIPTION=factory.createIRI(NAMESPACE, "description");

	}

}
