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

package com.metreeca.rdf;

import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;

import java.util.*;


/**
 * RDF file format utilities.
 */
public final class Formats {


	private Formats() {} // a utility class

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static <F extends FileFormat, S> S service(
			final FileFormatServiceRegistry<F, S> registry, final F fallback, final Collection<String> types) {

		if ( registry == null ) {
			throw new NullPointerException("null registry");
		}

		if ( fallback == null ) {
			throw new NullPointerException("null fallback");
		}

		if ( types == null || types.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null types");
		}

		return registry

				.get(types.stream()

						.map(type -> type.equals("*/*") ? Optional.of(fallback)
								: type.endsWith("/*") ? match(type, registry.getKeys())
								: registry.getFileFormatForMIMEType(type)
						)

						.filter(Optional::isPresent)
						.map(Optional::get)
						.findFirst()
						.orElse(fallback))

				.orElseThrow(() -> new IllegalArgumentException(
						"unsupported fallback format ["+fallback.getDefaultMIMEType()+"]"));

	}


	private static <F extends FileFormat> Optional<F> match(final String pattern, final Iterable<F> formats) {

		final String prefix=pattern.substring(0, pattern.indexOf('/')+1);

		for (final F format : formats) { // first try to match with the default MIME type
			if ( format.getDefaultMIMEType().toLowerCase(Locale.ROOT).startsWith(prefix) ) {
				return Optional.of(format);
			}
		}

		for (final F format : formats) { // try alternative MIME types too
			for (final String type : format.getMIMETypes()) {
				if ( type.toLowerCase(Locale.ROOT).startsWith(prefix) ) {
					return Optional.of(format);
				}
			}
		}

		return Optional.empty();
	}

}
