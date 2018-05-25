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

import com.metreeca.spec.things.Transputs;
import com.metreeca.tray.Tool;
import com.metreeca.tray.sys.Store.Blob;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.sys.Setup.storage;
import static com.metreeca.tray.sys.Trace.clip;

import static java.lang.String.format;


/**
 * URL cache.
 */
public final class Cache {

	// !!! manage TTL
	// !!! trace blob dependencies and clear derived blobs on expiration/purging


	/**
	 * Cache factory.
	 *
	 * <p>The default cache acquired through this factory stores retrieved data in the {@code cache} folder under the
	 * default storage folder defined by the {@link Setup#StorageProperty} property.</p>
	 */
	public static final Tool<Cache> Tool=tools ->
			new Cache(new File(storage(tools.get(Setup.Tool)), "cache"));


	private static final BiConsumer<URL, Blob> NullFetcher=(url, blob) -> {};


	public static FileFetcher file() {
		return new FileFetcher();
	}

	public static HTTPFetcher http() {
		return new HTTPFetcher();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Store store; // delegate blob store

	private final Map<String, BiConsumer<URL, Blob>> fetchers=new HashMap<>();

	{
		fetchers.put("file", file());
		fetchers.put("http", http());
		fetchers.put("https", http());
		// !!! data URL support / binary with Base64 decoding
	}

	/**
	 * Creates a new URL cache.
	 *
	 * @param storage the root storage folder for the new cache
	 */
	public Cache(final File storage) {

		if ( storage == null ) {
			throw new NullPointerException("null storage");
		}

		this.store=new Store(storage);
	}

	/**
	 * Creates a new URL cache .
	 *
	 * @param store the backing blob store for the new cache
	 */
	public Cache(final Store store) {

		if ( store == null ) {
			throw new NullPointerException("null store");
		}

		this.store=store;
	}


	public <T> T exec(final String url, final Function<Blob, T> task) throws UncheckedIOException {

		if ( url == null ) {
			throw new NullPointerException("null url");
		}

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		try {

			return exec(new URL(url), task);

		} catch ( final MalformedURLException e ) {
			throw new UncheckedIOException(e);
		}
	}

	public <T> T exec(final String url, final BiConsumer<URL, Blob> fetcher, final Function<Blob, T> task) throws UncheckedIOException {

		if ( url == null ) {
			throw new NullPointerException("null url");
		}

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		try {

			return exec(new URL(url), fetcher, task);

		} catch ( final MalformedURLException e ) {
			throw new UncheckedIOException(e);
		}
	}


	/**
	 * @throws UncheckedIOException if unable to retrieve data from {@code url}
	 */
	public <T> T exec(final URL url, final Function<Blob, T> task) throws UncheckedIOException {

		if ( url == null ) {
			throw new NullPointerException("null url");
		}

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return exec(url, fetchers.getOrDefault(url.getProtocol(), NullFetcher), task);
	}

	/**
	 * @throws UncheckedIOException if unable to retrieve data from {@code url}
	 */
	public <T> T exec(final URL url, final BiConsumer<URL, Blob> fetcher, final Function<Blob, T> task) throws UncheckedIOException {

		if ( url == null ) {
			throw new NullPointerException("null url");
		}

		if ( fetcher == null ) {
			throw new NullPointerException("null fetcher");
		}

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return store.exec(url.toExternalForm(), blob -> {

			fetcher.accept(url, blob);

			return task.apply(blob);

		});
	}


	public Cache purge() {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	public Cache clear() {

		store.clear();

		return this;

	}


	//// Fetchers //////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class FileFetcher implements BiConsumer<URL, Blob> {

		private final Trace trace=tool(Trace.Tool);

		@Override public void accept(final URL url, final Blob blob) { // !!! metadata?
			try {

				final File file=new File(url.toURI());

				if ( !blob.exists() || file.lastModified() > blob.updated() ) {

					trace.info(this, format("fetching <%s>", clip(url)));

					try (
							final OutputStream output=blob.output();
							final InputStream input=new FileInputStream(file)
					) {
						Transputs.data(output, input);
					}

				}

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			} catch ( final URISyntaxException unexpected ) {
				throw new RuntimeException(unexpected);
			}
		}

	}

	public static final class HTTPFetcher implements BiConsumer<URL, Blob> {

		private final Map<String, String> headers=new LinkedHashMap<>();

		private final Trace trace=tool(Trace.Tool);


		public HTTPFetcher header(final String name, final String value) {

			if ( name == null ) {
				throw new NullPointerException("null name");
			}

			if ( value == null ) {
				throw new NullPointerException("null value");
			}

			headers.put(name, value);

			return this;
		}


		@Override public void accept(final URL url, final Blob blob) {  // !!! metadata?
			if ( !blob.exists() ) { // !!! check TTL / caching headers / verify last updated from erver
				try {

					trace.info(this, format("fetching <%s>", clip(url)));

					final HttpURLConnection connection=(HttpURLConnection)url.openConnection();

					connection.setRequestMethod("GET"); // !!! POST support

					connection.setDoInput(true);
					connection.setDoOutput(false); // !!! POST support

					// !!! connection.setConnectTimeout();
					// !!! connection.setReadTimeout();
					// !!! connection.setIfModifiedSince();

					connection.setInstanceFollowRedirects(true);

					headers.forEach(connection::setRequestProperty);

					connection.connect();

					final int code=connection.getResponseCode();
					final String encoding=Optional.ofNullable(connection.getContentEncoding()).orElse(Transputs.UTF8.name());

					if ( code/100 == 2 ) {

						// !!! metadata from headers? TTL from headers?
						// !!! encode blob according to headers

						try (
								final OutputStream output=blob.output();
								final InputStream input=connection.getInputStream()
						) {
							Transputs.data(output, input);
						}

					} else {

						final String message=format("unable to retrieve data from <%s> : status %d (%s)",
								clip(url), code, Transputs.text(Transputs.reader(connection.getErrorStream(), encoding)));

						trace.warning(this, message);

						throw new UncheckedIOException(new ProtocolException(message));

					}

				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}
			}
		}

	}

}
