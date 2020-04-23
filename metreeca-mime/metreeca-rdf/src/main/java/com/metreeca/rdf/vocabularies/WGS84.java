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
 * Constants for the <a href="https://www.w3.org/2003/01/geo/">Basic Geo (WGS84 lat/long) Vocabulary</a>.
 *
 * @see <a href="https://www.w3.org/2003/01/geo/">Basic Geo (WGS84 lat/long) Vocabulary</a>
 */
public final class WGS84 {

	/**
	 * The WGS84 namespace ({@value}).
	 */
	public static final String NAMESPACE="http://www.w3.org/2003/01/geo/wgs84_pos#";

	/**
	 * Recommended prefix for the RDF Schema namespace ({@value}).
	 */
	public static final String PREFIX="wgs84";

	/**
	 * An immutable {@link Namespace} constant that represents the WGS84 namespace.
	 */
	public static final Namespace NS=new SimpleNamespace(PREFIX, NAMESPACE);


	/**
	 * http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing
	 */
	public static final IRI SPATIAL_THING=SimpleValueFactory.getInstance().createIRI(NAMESPACE, "SpatialThing");

	/**
	 * http://www.w3.org/2003/01/geo/wgs84_pos#TemporalThing
	 */
	public static final IRI TEMPORAL_THING=SimpleValueFactory.getInstance().createIRI(NAMESPACE, "TemporalThing");


	/**
	 * http://www.w3.org/2003/01/geo/wgs84_pos#Event
	 */
	public static final IRI EVENT=SimpleValueFactory.getInstance().createIRI(NAMESPACE, "Event");

	/**
	 * http://www.w3.org/2003/01/geo/wgs84_pos#Point
	 */
	public static final IRI POINT=SimpleValueFactory.getInstance().createIRI(NAMESPACE, "Point");


	/**
	 * http://www.w3.org/2003/01/geo/wgs84_pos#location
	 */
	public static final IRI LOCATION=SimpleValueFactory.getInstance().createIRI(NAMESPACE, "location");

	/**
	 * http://www.w3.org/2003/01/geo/wgs84_pos#lat
	 */
	public static final IRI LAT=SimpleValueFactory.getInstance().createIRI(NAMESPACE, "lat");

	/**
	 * http://www.w3.org/2003/01/geo/wgs84_pos#long
	 */
	public static final IRI LONG=SimpleValueFactory.getInstance().createIRI(NAMESPACE, "long");

	/**
	 * http://www.w3.org/2003/01/geo/wgs84_pos#alt
	 */
	public static final IRI ALT=SimpleValueFactory.getInstance().createIRI(NAMESPACE, "alt");

	/**
	 * http://www.w3.org/2003/01/geo/wgs84_pos#lat_long
	 */
	public static final IRI LAT_LONG=SimpleValueFactory.getInstance().createIRI(NAMESPACE, "lat_long");


	private WGS84() {}

}
