/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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
