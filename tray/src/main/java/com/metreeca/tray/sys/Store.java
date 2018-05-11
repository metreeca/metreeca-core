/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.tray.sys;

import com.metreeca.tray.IO;
import com.metreeca.tray.Tool;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.metreeca.tray.sys.Setup.storage;

import static java.util.UUID.nameUUIDFromBytes;


/**
 * Blob store.
 */
public final class Store {

	/**
	 * Store factory.
	 *
	 * <p>The default store acquired through this factory stores blobs in the {@code store} folder under the default
	 * storage folder defined by the {@link Setup#StorageProperty} property.</p>
	 */
	public static final Tool<Store> Tool=tools ->
			new Store(new File(storage(tools.get(Setup.Tool)), "store"));


	/**
	 * The root storage folder for the blob store.
	 */
	private final File storage;


	public Store(final File storage) {

		if ( storage == null ) {
			throw new NullPointerException("null storage");
		}

		if ( storage.exists() && storage.isFile() ) {
			throw new IllegalArgumentException("plain file at storage folder path ["+storage+"]");
		}

		this.storage=storage;

		storage.mkdirs();
	}


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

	public <T> T exec(final String id, final Function<Blob, T> task) {

		if ( id == null ) {
			throw new NullPointerException("null id");
		}

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		// acquire intra-process lock // !!! per-file or per-block

		synchronized ( storage ) {

			final File file=new File(storage, nameUUIDFromBytes(id.getBytes(IO.UTF8Set)).toString());

			// acquire inter-process lock

			// !!! ;( breaks on windows when task tries to read from locked file

			//try (
			//		final RandomAccessFile random=new RandomAccessFile(file, "rw");
			//		final FileChannel channel=random.getChannel();
			//		final FileLock ignored=channel.lock()
			//) {

			return task.apply(new Blob(file));

			//} catch ( final IOException e ) {
			//	throw new UncheckedIOException(e);
			//}

		}
	}


	public Store clear() {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class Blob {

		private final File file;


		private Blob(final File file) {
			this.file=file;
		}


		public boolean exists() {
			return file.length() > 0;
		}

		public long size() {
			return file.length();
		}

		public long updated() {
			return file.lastModified();
		}


		public Map<IRI, Value> metadata() {
			return Collections.emptyMap(); // !!!
		}

		public Blob metadata(final Map<IRI, Value> metadata) {
			return this; // !!!
		}


		public Collection<Blob> dependencies() {
			return Collections.emptySet(); // !!!
		}

		public Blob dependencies(final Collection<Blob> dependencies) {
			return this; // !!!
		}


		// !!! encoding


		public byte[] data() {
			try (final InputStream input=input();) {
				return IO.data(input);
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}

		public Blob data(final byte... data) {
			try (final OutputStream output=output()) {

				IO.data(output, data);

				return this;

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}


		public String text() {
			try (final Reader reader=reader();) {
				return IO.text(reader);
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}

		public Blob text(final String text) {
			try (final Writer writer=writer()) {

				IO.text(writer, text);

				return this;

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}


		public InputStream input() {
			try {

				return file.exists() ? new FileInputStream(file) : IO.input();

			} catch ( FileNotFoundException e ) {

				throw new UncheckedIOException(e);

			}
		}

		public OutputStream output() {

			try {

				return new FileOutputStream(file);

			} catch ( FileNotFoundException e ) {

				throw new UncheckedIOException(e);

			}
		}


		public Reader reader() {
			return IO.reader(input());
		}

		public Writer writer() {
			return IO.writer(output());
		}

	}

}
