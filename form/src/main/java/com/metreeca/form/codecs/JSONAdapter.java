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

package com.metreeca.form.codecs;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RioSetting;
import org.eclipse.rdf4j.rio.helpers.RioSettingImpl;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;

import static com.metreeca.form.things.Values.iri;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;


public final class JSONAdapter {

	/**
	 * The plain <a href="http://www.json.org/">JSON</a> file format.
	 *
	 * The file extension {@code .json} is recommend for JSON documents.
	 * The media type is {@code application/json}.
	 * The character encoding is {@code UTF-8}.
	 */
	public static final RDFFormat JSONFormat=new RDFFormat("JSON",
			asList("application/json", "text/json"),
			Charset.forName("UTF-8"),
			singletonList("json"),
			iri("http://www.json.org/"),
			RDFFormat.NO_NAMESPACES,
			RDFFormat.NO_CONTEXTS);

	/**
	 * Sets the focus resource for codecs.
	 *
	 * <p>Defaults to {@code null}.</p>
	 */
	public static final RioSetting<Resource> Focus=new RioSettingImpl<>(
			JSONAdapter.class.getName()+"#Focus", "Resource focus", null);

	/**
	 * Sets the expected shape for the resources handled by codecs.
	 *
	 * <p>Defaults to {@code null}.</p>
	 */
	public static final RioSetting<com.metreeca.form.Shape> Shape=new RioSettingImpl<>(
			JSONAdapter.class.getName()+"#Shape", "Resource shape", null);


	public static final Collection<String> Reserved=new HashSet<>(singleton(
			"this"
	));


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JSONAdapter() {}

}
