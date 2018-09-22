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

import com.metreeca.form.things.Transputs;
import com.metreeca.tray.sys.Store.Blob;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.sys.Trace.clip;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;


/**
 * URL cache.
 *
 * <p>Retrieves and caches remote URL content, delegating storage to a backing {@linkplain Store blob store}.</p>
 */
public final class Cache {

	// !!! manage TTL
	// !!! trace blob dependencies and clear derived blobs on expiration/purging


	/**
	 * Cache factory.
	 *
	 * <p>By default creates an uncustomized cache.</p>
	 */
	public static final Supplier<Cache> Factory=Cache::new;


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Store store=new Store().storage(tool(Storage.Factory).area("cache")); // delegate blob store

	private final Map<String, BiConsumer<URL, Blob>> fetchers=new HashMap<>(); // URL schema to fetcher


	/**
	 * Creates a new URL cache.
	 *
	 * <p>The new cache is {@linkplain #fetcher(String, BiConsumer) configured} with default handlers for {@code file:}
	 * and {@code http(s):} URL schemas.</p>
	 */
	public Cache() {
		fetchers.put("file", new FileFetcher());
		fetchers.put("http", new HTTPFetcher());
		fetchers.put("https", new HTTPFetcher());
		// !!! data URL support / binary with Base64 decoding
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * Registers a custom URL fetcher.
	 *
	 * <p>Fetchers are expected to test for themselves the caching status of the target URL, calling for instance
	 * {@link Blob#exists()} and checking other relevant metadata.</p>
	 *
	 * @param schema  the URL schema the custom fetcher will be applied to
	 * @param fetcher the custom fetcher that will be used to retrieve content from URLs with {@code schema}
	 *
	 * @return this cache
	 *
	 * @throws NullPointerException if either {@code schema} or {@code fetcher} is null
	 */
	public Cache fetcher(final String schema, final BiConsumer<URL, Blob> fetcher) {

		if ( schema == null ) {
			throw new NullPointerException("null schema");
		}

		if ( fetcher == null ) {
			throw new NullPointerException("null fetcher");
		}

		return this;
	}

	/**
	 * Configures the backing blob store for this cache.
	 *
	 * <p>Defaults to a blob store storing content to the "{@code cache}" storage area located by the current {@link
	 * Storage#area(String) storage} tool.</p>
	 *
	 * @param store the backing blob store for this cache
	 *
	 * @return this cache
	 *
	 * @throws NullPointerException if {@code store} is null
	 */
	public Cache store(final Store store) {

		if ( store == null ) {
			throw new NullPointerException("null store");
		}

		this.store=store;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Executes a task on the content retrieved from a URL.
	 *
	 * @param url  the URL content is to be retrieved from
	 * @param task the task to be executed on the contents retrieved from {@code url}; takes as argument a store
	 *             {@linkplain Store.Blob blob} providing access to the (possibly cached) content retrieved from {@code
	 *             url} using the {@linkplain #fetcher(String, BiConsumer) registered} URL fetcher for its URL schema or
	 *             an empty blob, if no suitable fetcher is registered
	 * @param <V>  the type of the value returned by task
	 *
	 * @return the value returned by {@code task} when applied to the contents retrieved from {@code url}
	 *
	 * @throws NullPointerException if either {@code url} or {@code task} is null or {@code task} returns a null value
	 * @throws UncheckedIOException if an I/O error occurred while retrieving content from {@code url}
	 */
	public <V> V exec(final URL url, final Function<Blob, V> task) throws UncheckedIOException {

		if ( url == null ) {
			throw new NullPointerException("null url");
		}

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return exec(url, fetchers.getOrDefault(url.getProtocol(), (_url, _blob) -> {}), task);
	}

	/**
	 * Executes a task on the content retrieved from a URL using a specific fetcher.
	 *
	 * @param url     the URL content is to be retrieved from
	 * @param fetcher the URL {@linkplain #fetcher(String, BiConsumer) fetcher}f to be used for retrieving content from
	 *                {@code url}
	 * @param task    the task to be executed on the contents retrieved from {@code url} using {@code fetcher}; takes as
	 *                argument a store {@linkplain Store.Blob blob} providing access to the (possibly cached) content
	 *                retrieved from {@code url} using {@code fetcher}
	 * @param <V>     the type of the value returned by task
	 *
	 * @return the value returned by {@code task} when applied to the contents retrieved from {@code url}
	 *
	 * @throws NullPointerException if any of {@code url}, {@code fetcher} or {@code task} is null or {@code task}
	 *                              returns a null value
	 * @throws UncheckedIOException if an I/O error occurred while retrieving content from {@code url}
	 */
	public <V> V exec(final URL url, final BiConsumer<URL, Blob> fetcher, final Function<Blob, V> task) throws UncheckedIOException {

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

			return requireNonNull(task.apply(blob));

		});
	}


	/**
	 * Clears this cache.
	 *
	 * <p>Removes cached content from the {@linkplain Store backing store} of this cache.</p>
	 *
	 * @return this cache
	 */
	public Cache clear() {

		store.clear();

		return this;
	}


	//// Fetchers //////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * File fetcher.
	 *
	 * <p>Fetches content from {@code file:} URLs identifying local files.</p>
	 */
	public static final class FileFetcher implements BiConsumer<URL, Blob> {

		private final Trace trace=tool(Trace.Factory);

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

	/**
	 * HTTP(S) fetcher.
	 *
	 * <p>Fetches content from {@code http(s):} URLs.</p>
	 */
	public static final class HTTPFetcher implements BiConsumer<URL, Blob> {

		private final Map<String, String> headers=new LinkedHashMap<>();

		private final Trace trace=tool(Trace.Factory);


		/**
		 * Adds a custom HTTP header to be included in remote requests
		 *
		 * @param name  the name of the custom HTTP header
		 * @param value the value of the custom HTTP header
		 *
		 * @return this fetcher
		 *
		 * @throws NullPointerException if either {@code name} or {@code value} is null
		 */
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
