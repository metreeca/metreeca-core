/*
 * Copyright © 2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.rest.services;

import com.metreeca.rest.Context;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Context.storage;
import static com.metreeca.rest.services.Logger.logger;
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
	 * <p>Caches data blobs in the {@code cache} folder of the system file {@linkplain Context#storage storage}.</p>
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


		@Override public <T> T retrieve(
				final String key, final Function<InputStream, T> decoder, final Function<OutputStream, T> encoder
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

						try ( final InputStream input=Files.newInputStream(file) ) {

							return decoder.apply(input);

						}

					} else {

						try ( final OutputStream output=Files.newOutputStream(file) ) {

							return encoder.apply(output);

						} catch ( final Exception e ) {
							
							Files.delete(file);

							throw e;

						} finally {

							if ( Files.size(file) == 0 ) { Files.delete(file); }

						}

					}

				} catch ( final IOException e ) {

					throw new UncheckedIOException(e);

				}
			}

		}

	}

}
