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

import com.metreeca.tray.Tray;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

import static org.junit.Assert.assertFalse;


public final class StoreTest {

	@Rule public final TemporaryFolder tmp=new TemporaryFolder();


	private void exec(final Consumer<Store> task) {
		new Tray().run(() -> {
			try {
				task.accept(new Store(tmp.newFolder()));
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}).clear();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testNewBlobsDontExist() {
		exec(store -> store.exec("http://example.com/", blob -> {

			assertFalse("", blob.exists());

		}));
	}
}
