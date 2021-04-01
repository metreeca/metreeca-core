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

package com.metreeca.rest;

import com.metreeca.rest.services.Logger;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * Service manager {thread-safe}.
 *
 * <p>Manages the lifecycle of shared services.</p>
 */
@SuppressWarnings("unchecked") public final class Toolbox {

	private static final ThreadLocal<Toolbox> scope=new ThreadLocal<>();


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
	 * {@linkplain #get(Supplier) Retrieves} a shared service from the current toolbox.
	 *
	 * @param factory the factory responsible for creating the required shared service; must return a non-null and
	 *                thread-safe object
	 * @param <T>     the type of the shared service created by {@code factory}
	 *
	 * @return the shared service created by {@code factory} or by its plugin replacement if one was {@linkplain
	 * #set(Supplier, Supplier) specified}
	 *
	 * @throws IllegalArgumentException if {@code factory} is null
	 * @throws IllegalStateException    if called outside an active toolbox or a circular service dependency is
	 *                                  detected
	 */
	public static <T> T service(final Supplier<T> factory) {

		if ( factory == null ) {
			throw new NullPointerException("null factory");
		}

		return toolbox().get(factory);
	}

	/**
	 * {@linkplain #get(Supplier, Supplier) Retrieves} a shared assert from the active toolbox.
	 *
	 * @param factory  the factory responsible for creating the required shared service; must return a non-null and
	 *                 thread-safe object
	 * @param delegate the factory responsible for creating a fallback delegate service if a circular service
	 *                 dependency is detected
	 * @param <T>      the type of the shared service created by {@code factory} and {@code delegate}
	 *
	 * @return the shared service created by {@code factory} or by its plugin replacement if one was {@linkplain
	 * #set(Supplier, Supplier) specified}
	 *
	 * @throws IllegalArgumentException if either {@code factory} or {@code delegate} is null
	 * @throws IllegalStateException    if called outside an active toolbox
	 */
	public static <T> T service(final Supplier<T> factory, final Supplier<T> delegate) {

		if ( factory == null ) {
			throw new NullPointerException("null factory");
		}

		if ( delegate == null ) {
			throw new NullPointerException("null delegate");
		}

		return toolbox().get(factory, delegate);
	}


	private static Toolbox toolbox() {

		final Toolbox toolbox=scope.get();

		if ( toolbox == null ) {
			throw new IllegalStateException("not running inside a service toolbox");
		}

		return toolbox;
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

			return Xtream.text(reader);

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

			return Xtream.data(output, input).toByteArray();

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Map<Supplier<?>, Supplier<?>> factories=new HashMap<>();
	private final Map<Supplier<?>, Object> services=new LinkedHashMap<>(); // preserve initialization order

	private final Object pending=new Object(); // placeholder for detecting circular dependencies


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the shared service created by a factory.
	 *
	 * <p>The new service is cached so that further calls for the same factory are idempotent.</p>
	 *
	 * <p>During object construction, nested shared service dependencies may be retrieved from this toolbox through
	 * the static {@linkplain  #service(Supplier) service locator} method of the toolbox class. The toolbox used by the
	 * service locator method is managed through a {@link ThreadLocal} variable, so it won't be available to object
	 * constructors executed on a different thread.</p>
	 *
	 * @param factory the factory responsible for creating the required shared service; must return a non-null and
	 *                thread-safe object
	 * @param <T>     the type of the shared service created by {@code factory}
	 *
	 * @return the shared service created by {@code factory} or by its plugin replacement if one was {@linkplain
	 * #set(Supplier, Supplier) specified}
	 *
	 * @throws IllegalArgumentException if {@code factory} is null
	 * @throws IllegalStateException    if a circular service dependency is detected
	 */
	public <T> T get(final Supplier<T> factory) {
		return get(factory, () -> { throw new IllegalStateException("circular service dependency ["+factory+"]"); });
	}

	/**
	 * Retrieves the shared service created by a factory.
	 *
	 * <p>The new service is cached so that further calls for the same factory are idempotent.</p>
	 *
	 * <p>During object construction, nested shared service dependencies may be retrieved from this toolbox through
	 * the static {@linkplain  #service(Supplier) service locator} method of the toolbox class. The toolbox used by the
	 * service locator method is managed through a {@link ThreadLocal} variable, so it won't be available to object
	 * constructors executed on a different thread.</p>
	 *
	 * @param factory  the factory responsible for creating the required shared service; must return a non-null and
	 *                 thread-safe object
	 * @param delegate the factory responsible for creating a fallback delegate service if a circular service
	 *                 dependency is detected
	 * @param <T>      the type of the shared service created by {@code factory} and {@code delegate}
	 *
	 * @return the shared service created by {@code factory} or by its plugin replacement if one was {@linkplain
	 * #set(Supplier, Supplier) specified}
	 *
	 * @throws IllegalArgumentException if either {@code factory} or {@code delegate} is null
	 */
	public <T> T get(final Supplier<T> factory, final Supplier<? extends T> delegate) {

		if ( factory == null ) {
			throw new NullPointerException("null factory");
		}

		if ( delegate == null ) {
			throw new NullPointerException("null delegate");
		}

		synchronized ( services ) {

			final T cached=(T)services.get(factory);

			if ( pending.equals(cached) ) { return delegate.get(); } else {

				return cached != null ? cached : toolbox(() -> {
					try {

						services.put(factory, pending); // mark factory as being acquired

						final T acquired=((Supplier<T>)factories.getOrDefault(factory, factory)).get();

						services.put(factory, acquired); // cache actual resource

						return acquired;

					} catch ( final Throwable e ) {

						services.remove(factory); // roll back acquisition marker

						throw e;

					}
				});
			}

		}
	}

	/**
	 * Replaces an service factory with a plugin.
	 *
	 * <p>Subsequent calls to {@link #get(Supplier)} using {@code factory} as key will return the shared service
	 * created by {@code plugin}.</p>
	 *
	 * @param <T>     the type of the shared service created by {@code factory}
	 * @param factory the factory to be replaced
	 * @param plugin  the replacing factory; must return a non-null and thread-safe object
	 *
	 * @return this toolbox
	 *
	 * @throws IllegalArgumentException if either {@code factory} or {@code plugin} is null
	 * @throws IllegalStateException    if {@code factory} service was already retrieved
	 */
	public <T> Toolbox set(final Supplier<T> factory, final Supplier<T> plugin) throws IllegalStateException {

		if ( factory == null ) {
			throw new NullPointerException("null factory");
		}

		if ( plugin == null ) {
			throw new NullPointerException("null plugin");
		}

		synchronized ( services ) {

			if ( services.containsKey(factory) ) {
				throw new IllegalStateException("factory already in use");
			}

			factories.put(factory, plugin);

			return this;

		}
	}


	/**
	 * Clears this toolbox.
	 *
	 * <p>All {@linkplain #get(Supplier) cached} service are purged. {@linkplain AutoCloseable Auto-closeable}
	 * service are closed in inverse creation order before purging.</p>
	 *
	 * @return this toolbox
	 */
	public Toolbox clear() {
		synchronized ( services ) {
			try {

				final Logger logger=get(Logger.logger()); // !!! make sure logger is not released before other services

				for (final Map.Entry<Supplier<?>, Object> entry : services.entrySet()) {

					final Supplier<Object> factory=(Supplier<Object>)entry.getKey();
					final Object service=entry.getValue();

					try {

						if ( service instanceof AutoCloseable ) {
							((AutoCloseable)service).close();
						}

					} catch ( final Exception t ) {

						logger.error(this,
								format("error during service deletion [%s/%s]", factory, service), t);

					}
				}

				return this;

			} finally {

				factories.clear();
				services.clear();

			}
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Executes a set of tasks using shared services managed by this toolbox.
	 *
	 * <p>During task execution, shared service may be retrieved from this toolbox through the static {@linkplain
	 * #service(Supplier) service locator} method of the toolbox class. The toolbox used by the service locator method is
	 * managed through a {@link ThreadLocal} variable, so it won't be available to methods executed on a different
	 * thread.</p>
	 *
	 * @param tasks the tasks to be executed
	 *
	 * @return this toolbox
	 *
	 * @throws NullPointerException if {@code task} is null or contains null items
	 */
	public Toolbox exec(final Runnable... tasks) {

		if ( tasks == null ) {
			throw new NullPointerException("null tasks");
		}

		return toolbox(() -> {

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

	private <V> V toolbox(final Supplier<V> task) {

		final Toolbox current=scope.get();

		try {

			scope.set(this);

			return task.get();

		} finally {

			scope.set(current);

		}

	}

}
