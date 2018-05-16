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

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

import static java.util.Collections.singleton;


public class SetupTest {

	@Test public void testRetrieveIntegerProperties() {

		assertEquals("return fallback", (Integer)1000, get(null, 1000, Setup::get));
		assertEquals("ignore malformed values", (Integer)1000, get("none", 1000, Setup::get));
		assertEquals("strip underscores", (Integer)1000, get("1_000", 0, Setup::get));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private <T> T get(final String value, final T fallback, final Getter<T> getter) {

		final Properties properties=new Properties();

		if ( value != null ) {
			properties.setProperty("test", value);
		}

		return getter.get(new Setup(singleton(setup -> properties)), "test", fallback);
	}


	@FunctionalInterface private interface Getter<T> {

		public T get(final Setup setup, final String property, final T fallback);

	}

}
