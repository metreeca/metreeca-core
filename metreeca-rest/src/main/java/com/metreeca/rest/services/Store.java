/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.rest.services;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Supplier;

import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.services.Storage.storage;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;


/**
 * Blob store.
 *
 * <p>Provides access to dedicated system store for binary data blobs.</p>
 */
public interface Store {

	/**
	 * Retrieves the default store factory.
	 *
	 * @return the default store factory, which stores data blobs in the {@code store} folder of the {@linkplain Storage
	 * system file storage}.
	 */
	public static Supplier<Store> store() {
		return () -> new Store() {

			private final Path path=service(storage()).file("store").toPath();


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

		};
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

}
