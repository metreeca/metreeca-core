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

package com.metreeca.tray;

import com.metreeca.tray.sys.Trace;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static java.lang.String.format;


/**
 * Tool tray {thread-safe}.
 *
 * <p>Manages the lifecycle of shared tools.</p>
 */
@SuppressWarnings("unchecked")
public final class Tray {

	private static final ThreadLocal<Tray> context=new ThreadLocal<>();


	/**
	 * {@linkplain #get(Supplier) Retrieves} a shared tool from the current context tray.
	 *
	 * @param factory the factory responsible for creating the required tool; must return a non-null and thread-safe
	 *                object
	 * @param <T>     the type of the shared tool created by {@code factory}
	 *
	 * @return the shared tool created by {@code factory} or by its plugin replacement if one was {@linkplain
	 * #set(Supplier, Supplier) specified}
	 *
	 * @throws IllegalArgumentException if {@code factory} is null
	 */
	public static <T> T tool(final Supplier<T> factory) {

		if ( factory == null ) {
			throw new NullPointerException("null factory");
		}

		final Tray tray=context.get();

		if ( tray == null ) {
			throw new IllegalStateException("not running inside a tray");
		}

		return tray.get(factory);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Map<Supplier<?>, Supplier<?>> factories=new HashMap<>();
	private final Map<Supplier<?>, Object> tools=new LinkedHashMap<>(); // preserve initialization order

	private final Object pending=new Object(); // placeholder for detecting circular dependencies


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the shared tool created by a factory.
	 *
	 * <p>The new tool is cached so that further calls for the same factory are idempotent.</p>
	 *
	 * <p>During object construction, nested shared tools dependencies may be retrieved from this tool tray through
	 * the static {@linkplain  #tool(Supplier) service locator} method of the Tray class. The context tray used by the
	 * service locator method is managed through a {@link ThreadLocal} variable, so it won't be available to object
	 * constructors executed on a different thread.</p>
	 *
	 * @param factory the factory responsible for creating the required tool; must return a non-null and thread-safe
	 *                object
	 * @param <T>     the type of the shared tool created by {@code factory}
	 *
	 * @return the shared tool created by {@code factory} or by its plugin replacement if one was {@linkplain
	 * #set(Supplier, Supplier) specified}
	 *
	 * @throws IllegalArgumentException if {@code factory} is null
	 */
	public <T> T get(final Supplier<T> factory) {

		if ( factory == null ) {
			throw new NullPointerException("null tool factory");
		}

		synchronized ( tools ) {

			final T cached=(T)tools.get(factory);

			if ( pending.equals(cached) ) {
				throw new IllegalStateException("circular dependency ["+factory+"]");
			}

			return cached != null ? cached : context(() -> {
				try {

					tools.put(factory, pending); // mark factory as being acquired

					final T acquired=((Supplier<T>)factories.getOrDefault(factory, factory)).get();

					tools.put(factory, acquired); // cache actual resource

					return acquired;

				} catch ( final RuntimeException e ) {

					tools.remove(factory); // roll back acquisition marker

					throw e;

				}
			});

		}
	}

	/**
	 * Replaces a tool factory with a plugin.
	 *
	 * <p>Subsequent calls to {@link #get(Supplier)} using {@code factory} as key will return the shared tool
	 * created by {@code plugin}.</p>
	 *
	 * @param <T>     the type of the shared tool created by {@code factory}
	 * @param factory the factory to be replaced
	 * @param plugin  the replacing factory; must return a non-null and thread-safe object
	 *
	 * @return this tool tray
	 *
	 * @throws IllegalArgumentException if either {@code factory} or {@code plugin} is null
	 * @throws IllegalStateException    if {@code factory} tool was already retrieved
	 */
	public <T> Tray set(final Supplier<T> factory, final Supplier<T> plugin) throws IllegalStateException {

		if ( factory == null ) {
			throw new NullPointerException("null factory");
		}

		if ( plugin == null ) {
			throw new NullPointerException("null plugin");
		}

		synchronized ( tools ) {

			if ( tools.containsKey(factory) ) {
				throw new IllegalStateException("factory already in use");
			}

			factories.put(factory, plugin);

			return this;

		}
	}


	/**
	 * Clears this tool tray.
	 *
	 * <p>All {@linkplain #get(Supplier) cached} tools are purged. {@linkplain AutoCloseable Auto-closeable}
	 * tools are closed in inverse creation order before purging.</p>
	 *
	 * @return this tool tray
	 */
	public Tray clear() {
		synchronized ( tools ) {
			try {

				final Trace trace=get(Trace.Factory); // !!! make sure trace is not released before other tools

				for (final Map.Entry<Supplier<?>, Object> entry : tools.entrySet()) {

					final Supplier<Object> tool=(Supplier<Object>)entry.getKey();
					final Object resource=entry.getValue();

					try {

						if ( resource instanceof AutoCloseable ) {
							((AutoCloseable)resource).close();
						}

					} catch ( final Exception t ) {

						trace.error(this,
								format("error during resource deletion [%s/%s]", tool, resource), t);

					}
				}

				return this;

			} finally {

				factories.clear();
				tools.clear();

			}
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Executes a task using shared tools managed by this tray.
	 *
	 * <p>During task execution, shared tools may be retrieved from this tool tray through
	 * the static {@linkplain  #tool(Supplier) service locator} method of the Tray class. The context tray used by the
	 * service locator method is managed through a {@link ThreadLocal} variable, so it won't be available to methods
	 * executed on a different thread.</p>
	 *
	 * @param task the task to be executed
	 *
	 * @return this tool tray
	 *
	 * @throws NullPointerException if {@code task} is null
	 */
	public Tray exec(final Runnable task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return context(() -> {

			task.run();

			return this;

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private <V> V context(final Supplier<V> task) {

		final Tray current=context.get();

		try {

			context.set(this);

			return task.get();

		} finally {

			context.set(current);

		}

	}

}
