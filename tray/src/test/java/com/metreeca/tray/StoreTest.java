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

package com.metreeca.tray;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;


final class StoreTest {

	private void exec(final Path tmp, final Consumer<Store> task) {
		new Tray().exec(() ->
				task.accept(new Store().storage(tmp.toFile()))
		).clear();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testNewBlobsDontExist(@TempDir final Path tmp) {
		exec(tmp, store -> store.exec("http://example.com/", blob -> {

			assertThat(blob.exists()).isFalse();

		}));
	}
}
