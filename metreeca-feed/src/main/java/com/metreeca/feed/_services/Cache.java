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

package com.metreeca.feed._services;

import com.metreeca.rest.Codecs;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.services.Storage.storage;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;


@FunctionalInterface public interface Cache {

	public static Supplier<Cache> cache() { return None::new; }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public InputStream retrieve(final String key, final Supplier<InputStream> source);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class None implements Cache {

		@Override public InputStream retrieve(final String key, final Supplier<InputStream> source) {

			if ( key == null ) {
				throw new NullPointerException("null key");
			}

			if ( source == null ) {
				throw new NullPointerException("null source");
			}

			return source.get();
		}

	}

	public static final class File implements Cache {

		private final Path path=service(storage()).file("cache").toPath();

		private Duration ttl=Duration.ZERO;


		public File ttl(final Duration ttl) {

			if ( ttl == null ) {
				throw new NullPointerException("null ttl");
			}

			if ( ttl.isNegative() ) {
				throw new IllegalArgumentException("negative ttl {"+ttl+"}");
			}

			synchronized ( path ) {

				this.ttl=ttl;

				return this;

			}
		}


		@Override public InputStream retrieve(final String key, final Supplier<InputStream> source) {

			if ( key == null ) {
				throw new NullPointerException("null key");
			}

			if ( source == null ) {
				throw new NullPointerException("null source");
			}

			// !!! inter-process locking using FileLock (https://stackoverflow.com/q/128038/739773)

			synchronized ( path ) {
				try {

					final Path file=Files
							.createDirectories(path)
							.resolve(UUID.nameUUIDFromBytes(key.getBytes(UTF_8)).toString());

					final boolean alive=Files.exists(file)
							&& Files.getLastModifiedTime(file).toInstant().plus(ttl).isAfter(now());

					if ( alive ) {

						return Files.newInputStream(file);

					} else {

						final InputStream input=source.get();

						if ( input instanceof TransientInputStream ) { return input; } else {

							try (final OutputStream output=Files.newOutputStream(file)) {

								Codecs.data(output, input);

								return Files.newInputStream(file);

							} finally {

								input.close();

							}

						}

					}

				} catch ( final IOException e ) {

					throw new UncheckedIOException(e);

				}
			}

		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class TransientInputStream extends FilterInputStream {

		public TransientInputStream(final InputStream input) { super(input); }

	}

}
