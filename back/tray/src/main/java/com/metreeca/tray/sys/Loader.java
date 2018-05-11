/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.tray.sys;

import com.metreeca.tray.Tool;

import java.io.InputStream;
import java.util.Optional;


/**
 * System resource loader
 */
@FunctionalInterface public interface Loader {

	/**
	 * Loader factory.
	 *
	 * <p>The default loader acquired through this factory retrieves system resources from the classpath through {@link
	 * ClassLoader#getResourceAsStream(String)}.</p>
	 */
	public static Tool<Loader> Tool=tools -> path -> {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return Optional.ofNullable(Loader.class.getClassLoader().getResourceAsStream(path));
	};


	public Optional<InputStream> load(final String path);

}
