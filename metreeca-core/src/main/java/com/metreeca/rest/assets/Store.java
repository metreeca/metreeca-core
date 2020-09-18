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

package com.metreeca.rest.assets;

import com.metreeca.rest.Context;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import java.util.function.Supplier;

import static com.metreeca.rest.Context.storage;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.*;


/**
 * Blob store.
 *
 * <p>Provides access to a dedicated system store for binary data blobs.</p>
 */
public interface Store {

	/**
	 * Retrieves the default store factory.
	 *
	 * @return the default store factory, which creates {@link FileStore} instances
	 */
	public static Supplier<Store> store() {
		return FileStore::new;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Reads a data blob.
	 *
	 * @param id the identifier of the data blob to be read
	 *
	 * @return an input stream for reading from the data blob identified by {@code id}
	 *
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id} is not a valid data blob identifier for the backing blob store
	 * @throws NoSuchFileException      if {@code id} is not the identifier of an existing data blob
	 * @throws IOException              if an I/O exception occurs while opening the input stream
	 */
	public InputStream read(final String id) throws IOException;

	/**
	 * Writes a data blob.
	 *
	 * <p>Write operations must be atomic, that is existing data must be made available for {@linkplain #read(String)
	 * reading} until the operation is completed by closing the output stream.</p>
	 *
	 * @param id the identifier of the data blob to be written
	 *
	 * @return an output stream for writing to the data blob identified by {@code id}
	 *
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id} is not a valid data blob identifier for the backing blob store
	 * @throws IOException              if an I/O exception occurs while opening the output stream
	 */
	public OutputStream write(final String id) throws IOException;


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Storage blob store.
	 *
	 * <p>Stores data blobs in the {@code store} folder of the system file {@linkplain Context#storage storage}.</p>
	 */
	public static class FileStore implements Store {

		private final Path path=Context.asset(storage()).resolve("store");


		@Override public InputStream read(final String id) throws IOException {

			if ( id == null ) {
				throw new NullPointerException("null id");
			}

			return Files.newInputStream(path.resolve(id), READ);
		}

		@Override public OutputStream write(final String id) throws IOException {

			if ( id == null ) {
				throw new NullPointerException("null id");
			}

			final Path blob=path.resolve(UUID.randomUUID().toString());

			return new FilterOutputStream(Files.newOutputStream(blob, CREATE_NEW, WRITE)) {

				@Override public void close() throws IOException {
					try {

						super.close();

						Files.move(blob, path.resolve(id), REPLACE_EXISTING);

					} catch ( final Throwable e ) {

						Files.deleteIfExists(blob);

						throw e;

					}
				}

			};

		}

	}

}
