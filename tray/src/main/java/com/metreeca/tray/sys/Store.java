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

import com.metreeca.form.things.Codecs;

import java.io.*;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.metreeca.tray.Tray.tool;

import static java.util.Objects.requireNonNull;
import static java.util.UUID.nameUUIDFromBytes;


/**
 * Blob store.
 *
 * <p>Manages and provide access to persistent content {@linkplain Blob blobs}.</p>
 */
public final class Store {

	/**
	 * Store factory.
	 *
	 * <p>By default creates an uncustomized store.</p>
	 */
	public static final Supplier<Store> Factory=Store::new;


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private File storage=tool(Storage.Factory).area("store"); // the root storage folder for the blob store

	private final Object lock=new Object(); // store access lock


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the storage folder for this blob store.
	 *
	 * <p>Defaults to the "{@code store}" area located by the current {@link Storage#area(String) storage} tool.</p>
	 *
	 * @param storage a file providing access to the storage folder to be used by this store
	 *
	 * @return this store
	 *
	 * @throws NullPointerException     if {@code storage} is null
	 * @throws IllegalArgumentException if {@code storage} identifies an existing file that is not a folder
	 */
	public Store storage(final File storage) {

		if ( storage == null ) {
			throw new NullPointerException("null storage");
		}

		if ( storage.exists() && !storage.isDirectory() ) {
			throw new IllegalArgumentException("plain file at storage folder path ["+storage+"]");
		}

		this.storage=storage;

		storage.mkdirs();

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Executes a task on a store blob.
	 *
	 * @param id   the identifier of the store blob the task is to be executed on
	 * @param task the task to be executed on the store blob identified by {@code id}
	 *
	 * @return this blob store
	 *
	 * @throws NullPointerException if either {@code id} or {@code task} is null
	 */
	public Store exec(final String id, final Consumer<Blob> task) {

		if ( id == null ) {
			throw new NullPointerException("null id");
		}

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return exec(id, blob -> {

			task.accept(blob);

			return this;

		});
	}

	/**
	 * Maps a store blob to a value.
	 *
	 * @param id   the identifier of the store blob to be mapped
	 * @param task the mapping function to be applied to the store blob identified by {@code id}
	 * @param <V>  the type of the value returned by {@code task}
	 *
	 * @return the value returned by {@code task} when applied to the store blob identified by {@code id}
	 *
	 * @throws NullPointerException if either {@code id} or {@code task} is null or {@code task} returns a null value
	 */
	public <V> V exec(final String id, final Function<Blob, V> task) {

		if ( id == null ) {
			throw new NullPointerException("null id");
		}

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		// acquire intra-process lock // !!! per-file or per-block

		synchronized ( lock ) {

			final File file=new File(storage, nameUUIDFromBytes(id.getBytes(Codecs.UTF8)).toString());

			// acquire inter-process lock

			// !!! ;( breaks on windows when task tries to read from locked file

			//try (
			//		final RandomAccessFile random=new RandomAccessFile(file, "rw");
			//		final FileChannel channel=random.getChannel();
			//		final FileLock ignored=channel.lock()
			//) {

			return requireNonNull(task.apply(new Blob(file)), "null task return value");

			//} catch ( final IOException e ) {
			//	throw new UncheckedIOException(e);
			//}

		}
	}


	/**
	 * Clears this blob store.
	 *
	 * <p>Removes all blobs from this store.</p>
	 *
	 * @return this blob store
	 */
	public Store clear() {
		synchronized ( lock ) {

			Optional.ofNullable(storage.listFiles()).ifPresent(files -> {
				for (final File file : files) { file.delete(); }
			});

			return this;

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Content blob.
	 */
	public static final class Blob {

		private final File file;


		private Blob(final File file) {
			this.file=file;
		}


		/**
		 * Tests if this blob exists.
		 *
		 * @return {@code true} if this blob was actually operated on and persisted; {@code false} otherwise
		 */
		public boolean exists() {
			return file.length() > 0;
		}

		/**
		 * Retrieves the size of this blob.
		 *
		 * @return the size in bytes of the content of this blob or {@code 0}, if this blob doesn't actually {@link
		 * #exists()}
		 */
		public long size() {
			return file.length();
		}

		/**
		 * Retrieves the timestamp of the last update on this blob.
		 *
		 * @return the timestamp of the last update on this blob, measured in milliseconds since the epoch (00:00:00
		 * GMT, January 1, 1970), or <code>0L</code> if this blob oesn't actually {@link #exists()}
		 */
		public long updated() {
			return file.lastModified();
		}

		/* !!!

		public Map<IRI, Value> metadata() {
			return Collections.emptyMap(); // !!!
		}

		public Blob metadata(final Map<IRI, Value> metadata) {
			return this; // !!!
		}


		public Collection<Blob> dependencies() {
			return Collections.emptySet(); // !!!
		}

		public Blob dependencies(final Blob... dependencies) {
			return dependencies(asList(dependencies));
		}

		public Blob dependencies(final Collection<Blob> dependencies) {
			return this; // !!!
		}

		*/

		// !!! encoding


		/**
		 * Retrieves the binary content of this blob.
		 *
		 * @return the binary content of this blob
		 *
		 * @throws UncheckedIOException if an I/O error occurs while reading from this blob
		 */
		public byte[] data() throws UncheckedIOException {
			try (final InputStream input=input();) {
				return Codecs.data(input);
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}

		/**
		 * Configures the binary content of this blob.
		 *
		 * @param data the binary content to be written to this blob
		 *
		 * @return this blob
		 *
		 * @throws NullPointerException if {@code data} is null
		 * @throws UncheckedIOException if an I/O error occurs while writing to this blob
		 */
		public Blob data(final byte... data) throws UncheckedIOException {
			try (final OutputStream output=output()) {

				Codecs.data(output, data);

				return this;

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}

		/**
		 * Configures the binary content of this blob.
		 *
		 * @param data an input stream providing access to the binary content to be written to this blob
		 *
		 * @return this blob
		 *
		 * @throws NullPointerException if {@code data} is null
		 * @throws UncheckedIOException if an I/O error occurs while reading from {@code data} or writing to this blob
		 */
		public Blob data(final InputStream data) throws UncheckedIOException {
			try (final OutputStream output=output()) {

				Codecs.data(output, data);

				return this;

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}


		/**
		 * Retrieves the textual content of this blob.
		 *
		 * @return the textual content of this blob, as read using the {@linkplain Codecs#UTF8 default encoding}
		 *
		 * @throws UncheckedIOException if an I/O error occurs while reading from this blob
		 */
		public String text() throws UncheckedIOException {
			try (final Reader reader=reader();) {
				return Codecs.text(reader);
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}

		/**
		 * Configures the textual content of this blob.
		 *
		 * @param text the textual content to be written to this blob using the {@linkplain Codecs#UTF8 default
		 *             encoding}
		 *
		 * @return this blob
		 *
		 * @throws NullPointerException if {@code text} is null
		 * @throws UncheckedIOException if an I/O error occurs while writing to this blob
		 */
		public Blob text(final String text) throws UncheckedIOException {
			try (final Writer writer=writer()) {

				Codecs.text(writer, text);

				return this;

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}

		/**
		 * Configures the textual content of this blob.
		 *
		 * @param text reader providing access to the textual content to be written to this blob using the {@linkplain
		 *             Codecs#UTF8 default encoding}
		 *
		 * @return this blob
		 *
		 * @throws NullPointerException if {@code text} is null
		 * @throws UncheckedIOException if an I/O error occurs while reading from {@code text} or writing to this blob
		 */
		public Blob text(final Reader text) throws UncheckedIOException {
			try (final Writer writer=writer()) {

				Codecs.text(writer, text);

				return this;

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}


		/**
		 * Opens an input stream for this blobs.
		 *
		 * @return an input stream supporting direct binary access to this blob
		 *
		 * @throws UncheckedIOException if an I/O error occurs while accessing this blob
		 */
		public InputStream input() throws UncheckedIOException {
			try {

				return file.exists() ? new FileInputStream(file) : Codecs.input();

			} catch ( final FileNotFoundException e ) {

				throw new UncheckedIOException(e);

			}
		}

		/**
		 * Opens an output stream for this blobs.
		 *
		 * @return an output stream supporting direct binary access to this blob
		 *
		 * @throws UncheckedIOException if an I/O error occurs while accessing this blob
		 */
		public OutputStream output() throws UncheckedIOException {
			try {

				return new FileOutputStream(file);

			} catch ( final FileNotFoundException e ) {

				throw new UncheckedIOException(e);

			}
		}


		/**
		 * Opens a reader for this blobs.
		 *
		 * @return a reader supporting direct textual access to this blob
		 *
		 * @throws UncheckedIOException if an I/O error occurs while accessing this blob
		 */
		public Reader reader() throws UncheckedIOException {
			return Codecs.reader(input());
		}

		/**
		 * Opens a writer for this blobs.
		 *
		 * @return a writer supporting direct textual access to this blob
		 *
		 * @throws UncheckedIOException if an I/O error occurs while accessing this blob
		 */
		public Writer writer() throws UncheckedIOException {
			return Codecs.writer(output());
		}

	}

}
