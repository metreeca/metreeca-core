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

package com.metreeca.rest.services;

import java.io.File;
import java.util.function.Supplier;


/**
 * System file storage.
 *
 * <p>Provides access to system-specific file storage areas.</p>
 */
@FunctionalInterface public interface Storage {

	/**
	 * Retrieves the default storage factory.
	 *
	 * @return the default storage factory, which provides access to files stored in the current working directory of
	 * the process in the host filesystem
	 */
	public static Supplier<Storage> storage() {
		return () -> path -> {

			if ( path == null ) {
				throw new NullPointerException("null path");
			}

			return new File(System.getProperty("user.dir"), path);
		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Locates a file.
	 *
	 * @param path the path of the file to be located
	 *
	 * @return the file identified by {@code path}
	 *
	 * @throws NullPointerException     if {@code path} is null
	 * @throws IllegalArgumentException if {@code path} is not a valid system-specific file path
	 */
	public File file(final String path);

}
