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

package com.metreeca.rest.services;

import java.util.Optional;
import java.util.function.Supplier;


/**
 * Secret vault.
 *
 * <p>Retrieves sensitive configuration parameters from safe system storage.</p>
 */
@FunctionalInterface public interface Vault {

	/**
	 * Retrieves the default vault factory.
	 *
	 * @return the default vault factory, which retrieves parameters from {@linkplain System#getProperties() system
	 * 		properties}
	 */
	public static Supplier<Vault> vault() {
		return () -> id -> Optional.ofNullable(System.getProperty(id));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves a sensitive configuration parameters.
	 *
	 * @param id the unique identifier of the parameter to be retrieved
	 *
	 * @return an optional containing the value of the parameter identified by {@code id}, if one is present in the
	 * 		vault; an empty optional, otherwise
	 *
	 * @throws NullPointerException if {@code id} is null
	 */
	public Optional<String> get(final String id);

}
