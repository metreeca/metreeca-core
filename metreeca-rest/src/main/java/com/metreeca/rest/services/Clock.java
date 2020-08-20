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

import java.util.function.Supplier;


/**
 * System clock.
 *
 * <p>Provides access to system-specific time.</p>
 */
@FunctionalInterface public interface Clock {

	/**
	 * Retrieves the default clock factory.
	 *
	 * @return the default clock factory, which provides access to the default {@linkplain System#currentTimeMillis()
	 * 		system time}
	 */
	public static Supplier<Clock> clock() {
		return () -> System::currentTimeMillis;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the system time.
	 *
	 * @return the current time in milliseconds
	 */
	public long time();

}
