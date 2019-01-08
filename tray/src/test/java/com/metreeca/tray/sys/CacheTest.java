/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;
import org.junitpioneer.jupiter.TempDirectory.TempDir;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static com.metreeca.form.things.Codecs.text;
import static com.metreeca.form.things.Codecs.url;

import static org.assertj.core.api.Assertions.assertThat;


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

				text(new FileWriter(file), create);

				cache.exec(url, blob -> {

					updated.set(blob.updated());

					assertThat(blob.text()).as("content retrieved").isEqualTo(create);

					return this;

				});

				cache.exec(url, blob -> {

					try { Thread.sleep(SyncDelay); } catch ( final InterruptedException ignored ) {}

					assertThat(updated.get()).as("content cached").isEqualTo(blob.updated());

					return this;

				});

				final String update="updated!";

				text(new FileWriter(file), update);

				cache.exec(file.toURI().toURL(), blob -> {

					assertThat(blob.text()).as("content updated").isEqualTo(update);
					assertThat(updated.get()).as("timestamp updated").isLessThan(blob.updated());

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

				assertThat(blob.text()).as("content retrieved").isNotEmpty();

				return this;

			});

			cache.exec(url, blob -> {

				try { Thread.sleep(SyncDelay); } catch ( final InterruptedException ignored ) {}

				assertThat(updated.get()).as("content cached").isEqualTo(blob.updated());

				return this;

			});

		});
	}

}
