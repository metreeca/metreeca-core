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

package com.metreeca.rest;

import com.metreeca.rest.assets.Logger;
import com.metreeca.rest.formats.TextFormat;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;

import static com.metreeca.rest.Xtream.copy;
import static com.metreeca.rest.assets.Logger.logger;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * Shared assets manager {thread-safe}.
 *
 * <p>Manages the lifecycle of shared assets.</p>
 */
@SuppressWarnings("unchecked") public final class Context {

	private static final ThreadLocal<Context> context=new ThreadLocal<>();


	/**
	 * Retrieves the default storage factory.
	 *
	 * @return the default storage factory, which returns the current working directory of the process in the host
	 * filesystem
	 */
	public static Supplier<Path> storage() {
		return () -> Paths.get("").toAbsolutePath();
	}


	/**
	 * {@linkplain #get(Supplier) Retrieves} a shared assets from the current context.
	 *
	 * @param factory the factory responsible for creating the required shared asset; must return a non-null and
	 *                thread-safe object
	 * @param <T>     the type of the shared asset created by {@code factory}
	 *
	 * @return the shared asset created by {@code factory} or by its plugin replacement if one was {@linkplain
	 * #set(Supplier, Supplier) specified}
	 *
	 * @throws IllegalArgumentException if {@code factory} is null
	 * @throws IllegalStateException    if called outside an active context or a circular asset dependency is
	 *                                  detected
	 */
	public static <T> T asset(final Supplier<T> factory) {

		if ( factory == null ) {
			throw new NullPointerException("null factory");
		}

		return context().get(factory);
	}

	/**
	 * {@linkplain #get(Supplier, Supplier) Retrieves} a shared assert from the active context.
	 *
	 * @param factory  the factory responsible for creating the required shared asset; must return a non-null and
	 *                 thread-safe object
	 * @param delegate the factory responsible for creating a fallback delegate asset if a circular asset
	 *                 dependency is detected
	 * @param <T>      the type of the shared asset created by {@code factory} and {@code delegate}
	 *
	 * @return the shared asset created by {@code factory} or by its plugin replacement if one was {@linkplain
	 * #set(Supplier, Supplier) specified}
	 *
	 * @throws IllegalArgumentException if either {@code factory} or {@code delegate} is null
	 * @throws IllegalStateException    if called outside an active context
	 */
	public static <T> T asset(final Supplier<T> factory, final Supplier<T> delegate) {

		if ( factory == null ) {
			throw new NullPointerException("null factory");
		}

		if ( delegate == null ) {
			throw new NullPointerException("null delegate");
		}

		return context().get(factory, delegate);
	}


	private static Context context() {

		final Context context=Context.context.get();

		if ( context == null ) {
			throw new IllegalStateException("not running inside an asset context");
		}

		return context;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves a class resource.
	 *
	 * @param master   the target class or an instance of the target class for the resource to be retrieved
	 * @param resource the path of the resource to be retrieved, relative to the target class
	 *
	 * @return the URL of the given {@code resource}
	 *
	 * @throws MissingResourceException if {@code resource} is not available
	 */
	public static URL resource(final Object master, final String resource) {

		if ( master == null ) {
			throw new NullPointerException("null master");
		}

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		final Class<?> clazz=master instanceof Class ? (Class<?>)master : master.getClass();

		final URL url=clazz.getResource(resource.startsWith(".") ? clazz.getSimpleName()+resource : resource);

		if ( url == null ) {
			throw new MissingResourceException(
					format("unknown resource [%s:%s]", clazz.getName(), resource),
					clazz.getName(),
					resource
			);
		}

		return url;
	}

	/**
	 * Retrieves the input stream for a class resource.
	 *
	 * @param master   the target class or an instance of the target class for the resource to be retrieved
	 * @param resource the path of the resource to be retrieved, relative to the target class
	 *
	 * @return the input stream for the given {@code resource}
	 *
	 * @throws MissingResourceException if {@code resource} is not available
	 */
	public static InputStream input(final Object master, final String resource) {

		if ( master == null ) {
			throw new NullPointerException("null master");
		}

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		try {

			final InputStream input=resource(master, resource).openStream();

			return resource.endsWith(".gz") ? new GZIPInputStream(input) : input;

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Retrieves the reader for a class resource.
	 *
	 * @param master   the target class or an instance of the target class for the resource to be retrieved
	 * @param resource the path of the resource to be retrieved, relative to the target class
	 *
	 * @return the {@code UTF-8} reader for the given {@code resource}
	 *
	 * @throws MissingResourceException if {@code resource} is not available
	 */
	public static Reader reader(final Object master, final String resource) {

		if ( master == null ) {
			throw new NullPointerException("null master");
		}

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		return new InputStreamReader(input(master, resource), UTF_8);
	}

	/**
	 * Retrieves the textual content of a class resource.
	 *
	 * @param master   the target class or an instance of the target class for the resource to be retrieved
	 * @param resource the path of the resource to be retrieved, relative to the target class
	 *
	 * @return the textual content of the given {@code resource}, read using the {@code UTF-8} charset
	 *
	 * @throws MissingResourceException if {@code resource} is not available
	 */
	public static String text(final Object master, final String resource) {

		if ( master == null ) {
			throw new NullPointerException("null master");
		}

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		try ( final Reader reader=reader(master, resource) ) {

			return TextFormat.text(reader);

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Retrieves the binary content of a class resource.
	 *
	 * @param master   the target class or an instance of the target class for the resource to be retrieved
	 * @param resource the path of the resource to be retrieved, relative to the target class
	 *
	 * @return the binary content of the given {@code resource}
	 *
	 * @throws MissingResourceException if {@code resource} is not available
	 */
	public static byte[] data(final Object master, final String resource) {

		if ( master == null ) {
			throw new NullPointerException("null master");
		}

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		try (
				final InputStream input=input(master, resource);
				final ByteArrayOutputStream output=new ByteArrayOutputStream()
		) {

			return copy(output, input).toByteArray();

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Map<Supplier<?>, Supplier<?>> factories=new HashMap<>();
	private final Map<Supplier<?>, Object> assets=new LinkedHashMap<>(); // preserve initialization order

	private final Object pending=new Object(); // placeholder for detecting circular dependencies


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the shared asset created by a factory.
	 *
	 * <p>The new asset is cached so that further calls for the same factory are idempotent.</p>
	 *
	 * <p>During object construction, nested shared asset dependencies may be retrieved from this context through
	 * the static {@linkplain  #asset(Supplier) asset locator} method of the Context class. The context used by the
	 * asset locator method is managed through a {@link ThreadLocal} variable, so it won't be available to object
	 * constructors executed on a different thread.</p>
	 *
	 * @param factory the factory responsible for creating the required shared asset; must return a non-null and
	 *                thread-safe object
	 * @param <T>     the type of the shared asset created by {@code factory}
	 *
	 * @return the shared asset created by {@code factory} or by its plugin replacement if one was {@linkplain
	 * #set(Supplier, Supplier) specified}
	 *
	 * @throws IllegalArgumentException if {@code factory} is null
	 * @throws IllegalStateException    if a circular asset dependency is detected
	 */
	public <T> T get(final Supplier<T> factory) {
		return get(factory, () -> { throw new IllegalStateException("circular asset dependency ["+factory+"]"); });
	}

	/**
	 * Retrieves the shared asset created by a factory.
	 *
	 * <p>The new asset is cached so that further calls for the same factory are idempotent.</p>
	 *
	 * <p>During object construction, nested shared asset dependencies may be retrieved from this context through
	 * the static {@linkplain  #asset(Supplier) asset locator} method of the Context class. The context used by the
	 * asset locator method is managed through a {@link ThreadLocal} variable, so it won't be available to object
	 * constructors executed on a different thread.</p>
	 *
	 * @param factory  the factory responsible for creating the required shared asset; must return a non-null and
	 *                 thread-safe object
	 * @param delegate the factory responsible for creating a fallback delegate asset if a circular asset
	 *                 dependency is detected
	 * @param <T>      the type of the shared asset created by {@code factory} and {@code delegate}
	 *
	 * @return the shared asset created by {@code factory} or by its plugin replacement if one was {@linkplain
	 * #set(Supplier, Supplier) specified}
	 *
	 * @throws IllegalArgumentException if either {@code factory} or {@code delegate} is null
	 */
	public <T> T get(final Supplier<T> factory, final Supplier<T> delegate) {

		if ( factory == null ) {
			throw new NullPointerException("null factory");
		}

		if ( delegate == null ) {
			throw new NullPointerException("null delegate");
		}

		synchronized ( assets ) {

			final T cached=(T)assets.get(factory);

			if ( pending.equals(cached) ) { return delegate.get(); } else {

				return cached != null ? cached : context(() -> {
					try {

						assets.put(factory, pending); // mark factory as being acquired

						final T acquired=((Supplier<T>)factories.getOrDefault(factory, factory)).get();

						assets.put(factory, acquired); // cache actual resource

						return acquired;

					} catch ( final Throwable e ) {

						assets.remove(factory); // roll back acquisition marker

						throw e;

					}
				});
			}

		}
	}

	/**
	 * Replaces an asset factory with a plugin.
	 *
	 * <p>Subsequent calls to {@link #get(Supplier)} using {@code factory} as key will return the shared asset
	 * created by {@code plugin}.</p>
	 *
	 * @param <T>     the type of the shared asset created by {@code factory}
	 * @param factory the factory to be replaced
	 * @param plugin  the replacing factory; must return a non-null and thread-safe object
	 *
	 * @return this context
	 *
	 * @throws IllegalArgumentException if either {@code factory} or {@code plugin} is null
	 * @throws IllegalStateException    if {@code factory} asset was already retrieved
	 */
	public <T> Context set(final Supplier<T> factory, final Supplier<T> plugin) throws IllegalStateException {

		if ( factory == null ) {
			throw new NullPointerException("null factory");
		}

		if ( plugin == null ) {
			throw new NullPointerException("null plugin");
		}

		synchronized ( assets ) {

			if ( assets.containsKey(factory) ) {
				throw new IllegalStateException("factory already in use");
			}

			factories.put(factory, plugin);

			return this;

		}
	}


	/**
	 * Clears this context.
	 *
	 * <p>All {@linkplain #get(Supplier) cached} asset are purged. {@linkplain AutoCloseable Auto-closeable}
	 * asset are closed in inverse creation order before purging.</p>
	 *
	 * @return this context
	 */
	public Context clear() {
		synchronized ( assets ) {
			try {

				final Logger logger=get(logger()); // !!! make sure logger is not released before other assets

				for (final Map.Entry<Supplier<?>, Object> entry : assets.entrySet()) {

					final Supplier<Object> factory=(Supplier<Object>)entry.getKey();
					final Object asset=entry.getValue();

					try {

						if ( asset instanceof AutoCloseable ) {
							((AutoCloseable)asset).close();
						}

					} catch ( final Exception t ) {

						logger.error(this,
								format("error during asset deletion [%s/%s]", factory, asset), t);

					}
				}

				return this;

			} finally {

				factories.clear();
				assets.clear();

			}
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Executes a set of tasks using shared assets managed by this context.
	 *
	 * <p>During task execution, shared asset may be retrieved from this context through the static {@linkplain
	 * #asset(Supplier) asset locator} method of the Context class. The context used by the asset locator method is
	 * managed through a {@link ThreadLocal} variable, so it won't be available to methods executed on a different
	 * thread.</p>
	 *
	 * @param tasks the tasks to be executed
	 *
	 * @return this context
	 *
	 * @throws NullPointerException if {@code task} is null or contains null items
	 */
	public Context exec(final Runnable... tasks) {

		if ( tasks == null ) {
			throw new NullPointerException("null tasks");
		}

		return context(() -> {

			for (final Runnable task : tasks) {

				if ( task == null ) {
					throw new NullPointerException("null task");
				}

				task.run();
			}

			return this;

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private <V> V context(final Supplier<V> task) {

		final Context current=context.get();

		try {

			context.set(this);

			return task.get();

		} finally {

			context.set(current);

		}

	}

}
