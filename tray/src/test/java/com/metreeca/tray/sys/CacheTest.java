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

import com.metreeca.form.things.Transputs;
import com.metreeca.tray.Tray;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;
import org.junitpioneer.jupiter.TempDirectory.TempDir;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static com.metreeca.form.things.Transputs.url;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@ExtendWith(TempDirectory.class) final class CacheTest {

	private static final int SyncDelay=1000;



	private void exec(final Path tmp, final Consumer<Cache> task) {
		new Tray().exec(() ->
				task.accept(new Cache().store(new Store().storage(tmp.toFile())))
		).clear();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testCacheFileURLs(@TempDir final Path tmp) {
		exec(tmp, cache -> {
			try {

				final AtomicLong updated=new AtomicLong();

				final File file=new File(tmp.toFile(), "file");
				final URL url=file.toURI().toURL();

				final String create="created!";

				Transputs.text(new FileWriter(file), create);

				cache.exec(url, blob -> {

					updated.set(blob.updated());

					assertEquals("content retrieved", create, blob.text());

					return this;

				});

				cache.exec(url, blob -> {

					try { Thread.sleep(SyncDelay); } catch ( final InterruptedException ignored ) {}

					assertEquals("content cached", updated.get(), blob.updated());

					return this;

				});

				final String update="updated!";

				Transputs.text(new FileWriter(file), update);

				cache.exec(file.toURI().toURL(), blob -> {

					assertEquals("content updated", update, blob.text());

					assertTrue("timestamp updated", updated.get() < blob.updated());

					return this;

				});

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}

	@Test void testCacheHTTPURLs(@TempDir final Path tmp) {
		exec(tmp, cache -> {

			final AtomicLong updated=new AtomicLong();
			final URL url=url("http://example.com/");

			cache.exec(url, blob -> {

				updated.set(blob.updated());

				assertFalse("content retrieved", blob.text().isEmpty());

				return this;

			});

			cache.exec(url, blob -> {

				try { Thread.sleep(SyncDelay); } catch ( final InterruptedException ignored ) {}

				assertEquals("content cached", updated.get(), blob.updated());

				return this;

			});

		});
	}

}
