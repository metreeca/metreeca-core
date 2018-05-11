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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import static com.metreeca.tray.sys.Setup.storage;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;


/**
 * Network cache (legacy).
 */
public final class _Cache { // !!! remove

	/**
	 * Cache factory.
	 *
	 * <p>The default cache acquired through this factory stores retrieved data in the {@code cache} folder under the
	 * default storage folder defined by the {@link Setup#StorageProperty} property.</p>
	 */
	public static final Tool<_Cache> Tool=tools ->
			new _Cache(new File(storage(tools.get(Setup.Tool)), "cache"));


	/**
	 * The root storage folder for the cache.
	 */
	private final File storage;


	public _Cache(final File storage) {

		if ( storage == null ) {
			throw new NullPointerException("null storage");
		}

		if ( storage.exists() && storage.isFile() ) {
			throw new IllegalArgumentException("plain file at storage folder path ["+storage+"]");
		}

		this.storage=storage;

		storage.mkdirs();
	}


	public boolean has(final String url) {

		if ( url == null ) {
			throw new NullPointerException("null url");
		}

		return blob(url).exists();
	}


	public Entry get(final String url) {

		if ( url == null ) {
			throw new NullPointerException("null url");
		}

		final File blob=blob(url); // !!! factor

		if ( blob.exists() ) {

			return new Entry() {

				@Override public String url() { return url; }

				@Override public Map<String, String> meta() { return emptyMap(); } // !!! metadata?

				@Override public Reader reader() throws FileNotFoundException {
					return IO.reader(input()); // !!! encoding?
				}

				@Override public InputStream input() throws FileNotFoundException {
					return new FileInputStream(blob);
				}

				@Override public Collection<String> dependencies() {
					return emptySet(); // !!! dependencies?
				}

			};

		} else {
			return url.startsWith("file:") ? file(url)
					: url.startsWith("http:") || url.startsWith("https:") ? http(url)
					// !!! url.startsWith("data:") data URL support / binary with Base64 decoding
					: none(url);
		}

	}


	public _Cache set(final String url, final Reader reader, final String... dependencies) throws IOException {

		if ( url == null ) {
			throw new NullPointerException("null url");
		}

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		return set(new Entry() {

			@Override public String url() { return url; }

			@Override public Reader reader() { return reader; }

			@Override public Collection<String> dependencies() {
				return unmodifiableList(asList(dependencies));
			}

		});
	}

	public _Cache set(final String url, final InputStream input, final String... dependencies) throws IOException {

		if ( url == null ) {
			throw new NullPointerException("null url");
		}

		if ( input == null ) {
			throw new NullPointerException("null input");
		}

		return set(new Entry() {

			@Override public String url() { return url; }

			@Override public InputStream input() { return input; }

			@Override public Collection<String> dependencies() {
				return unmodifiableList(asList(dependencies));
			}

		});
	}

	public _Cache set(final Entry entry) throws IOException {

		if ( entry == null ) {
			throw new NullPointerException("null entry");
		}

		try (final OutputStream output=new FileOutputStream(blob(entry.url()))) { // !!! factor
			IO.data(output, entry.input());
		}

		return this;
	}


	private File blob(final String url) {
		return new File(storage, UUID.nameUUIDFromBytes(url.getBytes(Charset.forName("UTF-8"))).toString());
	}


	public _Cache clear() {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	public _Cache purge() {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}


	//// Fetchers //////////////////////////////////////////////////////////////////////////////////////////////////////

	private Entry file(final String url) {
		return new Entry() {

			@Override public String url() { return url; }

			@Override public Map<String, String> meta() { return emptyMap(); }

			@Override public Reader reader() throws FileNotFoundException {
				return IO.reader(input());
			}

			@Override public InputStream input() throws FileNotFoundException {
				return new FileInputStream(url.substring("file:".length())); // no caching for local files
			}

		};
	}

	private Entry http(final String url) {
		return new Entry() {

			@Override public String url() { return url; }

			@Override public Map<String, String> meta() { return emptyMap(); }

			@Override public Reader reader() throws IOException {
				return IO.reader(input()); // !!! encode according to headers
			}

			@Override public InputStream input() throws IOException {

				final File blob=blob(url);

				if ( blob.createNewFile() ) { // !!! check TTL/headers

					final HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();

					connection.setRequestMethod("GET"); // !!! POST support

					// !!! connection.setConnectTimeout();
					// !!! connection.setReadTimeout();
					// !!! connection.setIfModifiedSince();

					connection.setDoInput(true);
					connection.setDoOutput(false); // !!! POST support

					// !!! headers

					try (final OutputStream output=new FileOutputStream(blob)) { // !!! factor
						IO.data(output, connection.getInputStream());
					}
				}

				return new FileInputStream(blob); // !!! factor

			}

		};
	}

	private Entry none(final String url) {
		return new Entry() {

			@Override public String url() { return url; }

			@Override public Map<String, String> meta() { return emptyMap(); }

			@Override public Reader reader() throws FileNotFoundException {
				return IO.reader(input());
			}

			@Override public InputStream input() throws FileNotFoundException {
				return new ByteArrayInputStream(new byte[] {});
			}

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static interface Entry {

		public String url();


		public default Map<String, String> meta() { return emptyMap(); }

		public default Collection<String> dependencies() { return emptySet(); }


		public default Reader reader() throws IOException { return IO.reader(input()); }

		public default InputStream input() throws IOException { return IO.input(reader()); }

	}

}
