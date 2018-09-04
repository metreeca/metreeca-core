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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public final class CacheTest {

	private static final int SyncDelay=1000;


	@Rule public final TemporaryFolder tmp=new TemporaryFolder();


	private void exec(final Consumer<Cache> task) {
		new Tray().lookup(() -> {
			try {
				task.accept(new Cache(tmp.newFolder()));
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}).clear();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testCacheFileURLs() {
		exec(cache -> {
			try {

				final AtomicLong updated=new AtomicLong();

				final File file=tmp.newFile();
				final String url=file.toURI().toString();

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

				cache.exec(url, blob -> {

					assertEquals("content updated", update, blob.text());

					assertTrue("timestamp updated", updated.get() < blob.updated());

					return this;

				});

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}

	@Test public void testCacheHTTPURLs() {
		exec(cache -> {

			final AtomicLong updated=new AtomicLong();

			cache.exec("http://example.com/", blob -> {

				updated.set(blob.updated());

				assertFalse("content retrieved", blob.text().isEmpty());

				return this;

			});

			cache.exec("http://example.com/", blob -> {

				try { Thread.sleep(SyncDelay); } catch ( final InterruptedException ignored ) {}

				assertEquals("content cached", updated.get(), blob.updated());

				return this;

			});

		});
	}

}
