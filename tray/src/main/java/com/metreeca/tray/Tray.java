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
 * <p>Manages shared process resources.</p>
 */
@SuppressWarnings("unchecked")
public final class Tray {

	private static final ThreadLocal<Tray> context=new ThreadLocal<>();


	/**
	 * Creates a shared resource.
	 *
	 * <p>The new resource must be non-null and thread-safe.</p>
	 *
	 * @param tools the tool loader for retrieving dependencies for the new resource
	 *
	 * @return the new shared resource
	 *
	 * @throws IllegalArgumentException if {@code tool} is {@code null}
	 */
	public static <T> T tool(final Supplier<T> factory) {

		if ( factory == null ) {
			throw new NullPointerException("null tool factory");
		}

		final Tray tray=context.get();

		if ( tray == null ) {
			throw new IllegalStateException("not running inside a tray");
		}

		return tray.get(factory);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Map<Supplier<?>, Supplier<?>> factories=new HashMap<>();
	private final Map<Supplier<?>, Object> resources=new LinkedHashMap<>(); // preserve initialization order

	private final Object pending=new Object(); // placeholder for detecting circular dependencies


	/**
	 * Retrieves the shared resource created by a tool factory.
	 *
	 * <p>The new resource is cached so that further calls for the same tool are idempotent.</p>
	 *
	 * @param factory the tool factory creating the required resource
	 * @param <T>     the type of the shared resource created by {@code factory}
	 *
	 * @return the shared resource created by {@code factory}
	 *
	 * @throws IllegalArgumentException if {@code factory} is {@code null}
	 */
	public <T> T get(final Supplier<T> factory) {

		if ( factory == null ) {
			throw new NullPointerException("null tool factory");
		}

		synchronized ( resources ) {

			final T cached=(T)resources.get(factory);

			if ( pending.equals(cached) ) {
				throw new IllegalStateException("circular dependency ["+factory+"]");
			}

			if ( cached != null ) {

				return cached;

			} else {

				try {

					resources.put(factory, pending); // mark factory as being acquired

					final T acquired=((Supplier<T>)factories.getOrDefault(factory, factory)).get();

					resources.put(factory, acquired); // cache actual resource

					return acquired;

				} catch ( final RuntimeException e ) {

					resources.remove(factory); // roll back acquisition marker

					throw e;
				}

			}

		}
	}

	/**
	 * Replaces a tool factory with a plugin replacement.
	 *
	 * <p>Subsequent calls to {@link #get(Supplier)} using {@code tool} as key will return the shared resource created
	 * by {@code plugin}.</p>
	 *
	 * @param <T>    the type of the shared resource created by {@code tool}
	 * @param tool   the tool to be replaced
	 * @param plugin the replacing tool
	 *
	 * @return this binder
	 *
	 * @throws IllegalArgumentException if either {@code tool} or {@code pluging} is {@code null}
	 * @throws IllegalStateException    if {@code tool} was already replaced with a plugin or its resource was already
	 *                                  created
	 */
	public <T> Tray set(final Supplier<T> tool, final Supplier<T> plugin) throws IllegalStateException {

		if ( tool == null ) {
			throw new NullPointerException("null tool");
		}

		if ( plugin == null ) {
			throw new NullPointerException("null plugin");
		}

		synchronized ( resources ) {

			if ( resources.containsKey(tool) ) {
				throw new IllegalStateException("tool already in use");
			}

			factories.put(tool, plugin);

			return this;

		}
	}


	/**
	 * Clears this tray.
	 *
	 * <p>All {@linkplain #get(Supplier) cached} resources are purged. {@linkplain AutoCloseable Auto-closeable}
	 * resource are closed in inverse creation order before purging.</p>
	 *
	 * @return this tray
	 */
	public Tray clear() {
		synchronized ( resources ) {
			try {

				final Trace trace=get(Trace.Factory); // !!! make sure trace is not released before other tools

				for (final Map.Entry<Supplier<?>, Object> entry : resources.entrySet()) {

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
				resources.clear();

			}
		}
	}


	/**
	 * Executes a task inside this tool tray
	 *
	 * @param task the task to be executed
	 *
	 * @return this tool tray
	 */
	public Tray exec(final Runnable task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		final Tray current=context.get();

		try {

			context.set(this);

			task.run();

			return this;

		} finally {
			context.set(current);
		}
	}

}
