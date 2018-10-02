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

package com.metreeca.tray.sys;

import java.io.File;
import java.util.function.Supplier;


/**
 * System file storage.
 *
 * <p>Provides access to system-specific file storage areas.</p>
 */
@FunctionalInterface public interface Storage {

	/**
	 * Storage factory.
	 *
	 * <p>The default storage acquired through this factory provides access to files stored in the current working
	 * directory of the process in the host filesystem.</p>
	 */
	public static Supplier<Storage> Factory=() -> name -> {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return new File(System.getProperty("user.dir"));
	};


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Locates a file storage area.
	 *
	 * @param name the name of the storage area to be located
	 *
	 * @return a file providing access to the named storage area
	 *
	 * @throws NullPointerException     if {@code name} is null
	 * @throws IllegalArgumentException if {@code name} is not a valid system-specific filename
	 */
	public File area(final String name);

}
