/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rest.services;

import com.metreeca.rest.Toolbox;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.metreeca.rest.Toolbox.service;
import static com.metreeca.rest.Toolbox.storage;
import static com.metreeca.rest.services.Logger.logger;

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
	 * @return the default cache factory, which creates {@link FileCache} instances
	 */
	public static Supplier<Cache> cache() { return FileCache::new; }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves an item from this cache.
	 *
	 * @param key     the key of the item to be retrieved
	 * @param decoder a function decoding a cached item from its binary representation; takes as argument an input
	 *                stream for reading the binary representation of the item to be retrieved and is expected to
	 *                return a non null object of type {@code T}
	 * @param encoder a function encoding an item to be cached to its binary representation; takes as argument an
	 *                output stream for writing the binary representation of the item to be cached and is expected
	 *                to return a non null object of type {@code T}
	 * @param <T>     the type of the cached items
	 *
	 * @return an object of type {@code T}, returned either from the {@code decoder}, if a binary blob matching {@code
	 * key} was found in the cache, or by the {@code encoder}, otherwise
	 *
	 * @throws NullPointerException if any argument is null
	 */
	public <T> T retrieve(final String key,
			final Function<InputStream, T> decoder, final Function<OutputStream, T> encoder
	);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Storage blob cache.
	 *
	 * <p>Caches data blobs in the {@code cache} folder of the system file {@linkplain Toolbox#storage storage}.</p>
	 */
	public static final class FileCache implements Cache {

		private Duration ttl=Duration.ZERO; // no expiry

		private final Path path=service(storage()).resolve("cache");
		private final Logger logger=service(logger());


		/**
		 * Configures the time-to-live for this cache (defaults to {@link Duration#ZERO}).
		 *
		 * @param ttl the time-to-live for items stored in this cache; if {@link Duration#isZero() zero}, items will be
		 *            retained indefinitely
		 *
		 * @return this cache
		 *
		 * @throws NullPointerException     if {@code ttl} is null
		 * @throws IllegalArgumentException if {@code ttl} is negative
		 */
		public FileCache ttl(final Duration ttl) {

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


		@Override public <T> T retrieve(final String key,
				final Function<InputStream, T> decoder, final Function<OutputStream, T> encoder
		) {

			if ( key == null ) {
				throw new NullPointerException("null key");
			}

			if ( decoder == null ) {
				throw new NullPointerException("null decoder");
			}

			if ( encoder == null ) {
				throw new NullPointerException("null encoder");
			}

			try {

				final Path file=Files
						.createDirectories(path)
						.resolve(UUID.nameUUIDFromBytes(key.getBytes(UTF_8)).toString())
						.toAbsolutePath();

				synchronized ( file.toString().intern() ) { // see https://stackoverflow.com/a/13957003/739773

					// !!! inter-process locking using FileLock? (https://stackoverflow.com/q/128038/739773)

					final boolean alive=Files.exists(file) && (
							ttl.isZero() || Files.getLastModifiedTime(file).toInstant().plus(ttl).isAfter(now())
					);

					if ( alive ) {

						logger.info(Cache.class, key);

						try ( final InputStream input=Files.newInputStream(file) ) {

							return decoder.apply(input);

						} catch ( final Exception e ) { // possibly corrupted/stale cache entry

							Files.delete(file); // trash

							return retrieve(key, decoder, encoder); // reload
						}

					} else {

						try ( final OutputStream output=Files.newOutputStream(file) ) {

							return encoder.apply(output);

						} catch ( final Exception e ) {

							Files.delete(file);

							throw e;

						} finally {

							if ( Files.exists(file) && Files.size(file) == 0 ) { Files.delete(file); }

						}

					}

				}

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			}

		}

	}

}
