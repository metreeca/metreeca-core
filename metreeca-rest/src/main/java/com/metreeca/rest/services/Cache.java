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

import com.metreeca.rest.Result;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.services.Logger.logger;
import static com.metreeca.rest.services.Storage.storage;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;


/**
 * Blob cache.
 *
 * <p>Provides access to a dedicated system cache for binary data blobs.</p>
 */
@FunctionalInterface public interface Cache {

	/**
	 * Retrieves the default cache factory.
	 *
	 * @return the default cache factory, which creates {@link StorageCache} instances
	 */
	public static Supplier<Cache> cache() { return StorageCache::new; }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Result<Supplier<InputStream>, Supplier<OutputStream>> retrieve(final String key);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Storage blob cache.
	 *
	 * <p>Caches data blobs in the {@code cache} folder of the {@linkplain Storage system file storage}.</p>>
	 */
	public static final class StorageCache implements Cache {

		private Duration ttl=Duration.ZERO; // no expiry

		private final Path path=service(storage()).path(Paths.get("cache"));
		private final Logger logger=service(logger());


		public StorageCache ttl(final Duration ttl) {

			if ( ttl == null ) {
				throw new NullPointerException("null ttl");
			}

			if ( ttl.isNegative() ) {
				throw new IllegalArgumentException("negative ttl");
			}

			synchronized ( path ) {
				this.ttl=ttl;
			}

			return this;
		}


		@Override public Result<Supplier<InputStream>, Supplier<OutputStream>> retrieve(
				final String key
		) {

			if ( key == null ) {
				throw new NullPointerException("null key");
			}

			// !!! inter-process locking using FileLock (https://stackoverflow.com/q/128038/739773)

			synchronized ( path ) {
				try {

					final Path file=Files
							.createDirectories(path)
							.resolve(UUID.nameUUIDFromBytes(key.getBytes(UTF_8)).toString());

					final boolean alive=Files.exists(file) && (
							ttl.isZero() || Files.getLastModifiedTime(file).toInstant().plus(ttl).isAfter(now())
					);

					if ( alive ) {

						logger.info(Cache.class, format("retrieving <%s>", key));

						return Value(() -> {
							try {

								return Files.newInputStream(file);

							} catch ( final IOException e ) {

								throw new UncheckedIOException(e);

							}
						});

					} else {

						return Error(() -> {
							try {

								return Files.newOutputStream(file);

							} catch ( final IOException e ) {

								throw new UncheckedIOException(e);

							}
						});

					}

				} catch ( final IOException e ) {

					throw new UncheckedIOException(e);

				}
			}

		}

	}

}
