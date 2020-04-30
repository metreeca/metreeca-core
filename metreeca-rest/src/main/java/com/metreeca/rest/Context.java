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

import com.metreeca.rest.services.Logger;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static com.metreeca.rest.services.Logger.logger;
import static java.lang.String.format;


/**
 * Shared service context {thread-safe}.
 *
 * <p>Manages the lifecycle of shared services.</p>
 */
@SuppressWarnings("unchecked")
public final class Context {

	private static final ThreadLocal<Context> context=new ThreadLocal<>();


	/**
	 * {@linkplain #get(Supplier) Retrieves} a shared service from the current context.
	 *
	 * @param factory the factory responsible for creating the required shared service; must return a non-null and
	 *                thread-safe object
	 * @param <T>     the type of the shared service created by {@code factory}
	 *
	 * @return the shared service created by {@code factory} or by its plugin replacement if one was {@linkplain
	 *        #set(Supplier, Supplier) specified}
	 *
	 * @throws IllegalArgumentException if {@code factory} is null
	 */
	public static <T> T service(final Supplier<T> factory) {

		if ( factory == null ) {
			throw new NullPointerException("null factory");
		}

		final Context context=Context.context.get();

		if ( context == null ) {
			throw new IllegalStateException("not running inside a service context");
		}

		return context.get(factory);
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
	 * <p>During object construction, nested shared services dependencies may be retrieved from this context through
	 * the static {@linkplain  #service(Supplier) service locator} method of the Context class. The context used by the
	 * service locator method is managed through a {@link ThreadLocal} variable, so it won't be available to object
	 * constructors executed on a different thread.</p>
	 *
	 * @param factory the factory responsible for creating the required shared service; must return a non-null and
	 *                thread-safe object
	 * @param <T>     the type of the shared service created by {@code factory}
	 *
	 * @return the shared service created by {@code factory} or by its plugin replacement if one was {@linkplain
	 *        #set(Supplier, Supplier) specified}
	 *
	 * @throws IllegalArgumentException if {@code factory} is null
	 */
	public <T> T get(final Supplier<T> factory) {

		if ( factory == null ) {
			throw new NullPointerException("null factory");
		}

		synchronized ( services ) {

			final T cached=(T)services.get(factory);

			if ( pending.equals(cached) ) {
				throw new IllegalStateException("circular dependency ["+factory+"]");
			}

			return cached != null ? cached : context(() -> {
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

	/**
	 * Replaces a service factory with a plugin.
	 *
	 * <p>Subsequent calls to {@link #get(Supplier)} using {@code factory} as key will return the shared service
	 * created by {@code plugin}.</p>
	 *
	 * @param <T>     the type of the shared service created by {@code factory}
	 * @param factory the factory to be replaced
	 * @param plugin  the replacing factory; must return a non-null and thread-safe object
	 *
	 * @return this context
	 *
	 * @throws IllegalArgumentException if either {@code factory} or {@code plugin} is null
	 * @throws IllegalStateException    if {@code factory} service was already retrieved
	 */
	public <T> Context set(final Supplier<T> factory, final Supplier<T> plugin) throws IllegalStateException {

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
	 * Clears this context.
	 *
	 * <p>All {@linkplain #get(Supplier) cached} services are purged. {@linkplain AutoCloseable Auto-closeable}
	 * services are closed in inverse creation order before purging.</p>
	 *
	 * @return this context
	 */
	public Context clear() {
		synchronized ( services ) {
			try {

				final Logger logger=get(logger()); // !!! make sure logger is not released before other services

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
	 * Executes a set of task using shared ervices managed by this context.
	 *
	 * <p>During task execution, shared services may be retrieved from this context through the static {@linkplain
	 * #service(Supplier) service locator} method of the Context class. The context used by the service locator 
	 * method is
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
