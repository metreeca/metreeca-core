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

package com.metreeca.tray;

import com.metreeca.tray.sys.Trace;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static java.lang.String.format;


/**
 * Shared resource repository {thread-safe}.
 */
@SuppressWarnings("unchecked")
public final class Tray implements Tool.Loader, Tool.Binder {

	private static final ThreadLocal<Tray> context=new ThreadLocal<>();


	public static Tray tray() {
		return new Tray();
	}

	public static <T> T tool(final Tool<T> tool) {

		if ( tool == null ) {
			throw new NullPointerException("null tool");
		}

		final Tray tray=context.get();

		if ( tray == null ) {
			throw new IllegalStateException("not running inside a tray");
		}

		return tray.get(tool);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Map<Tool<?>, Tool<?>> tools=new HashMap<>();
	private final Map<Tool<?>, Object> resources=new LinkedHashMap<>(); // preserve initialization order

	private final Object pending=new Object(); // placeholder for detecting circular dependencies


	private Tray() {}


	@Override public <T> T get(final Tool<T> tool) {

		if ( tool == null ) {
			throw new NullPointerException("null tool");
		}

		synchronized ( resources ) {

			final T cached=(T)resources.get(tool);

			if ( pending.equals(cached) ) {
				throw new IllegalStateException("circular dependency ["+tool+"]");
			}

			if ( cached != null ) {

				return cached;

			} else {

				try {

					resources.put(tool, pending); // mark tool as being acquired

					final T acquired=((Tool<T>)tools.getOrDefault(tool, tool)).create(this);

					resources.put(tool, acquired); // cache actual resource

					return acquired;

				} catch ( final RuntimeException e ) {

					resources.remove(tool); // roll back acquisition marker

					throw e;
				}

			}

		}
	}

	@Override public <T> Tray set(final Tool<T> tool, final Tool<T> plugin) throws IllegalStateException {

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

			tools.put(tool, plugin);

			return this;

		}
	}


	/**
	 * Clears this tray.
	 *
	 * <p>All {@linkplain Tool.Loader#get(Tool) cached} resources are purged. {@linkplain AutoCloseable Auto-closeable}
	 * resource are closed in inverse creation order before purging.</p>
	 *
	 * @return this tray
	 */
	public Tray clear() {
		synchronized ( resources ) {
			try {

				final Trace trace=get(Trace.Tool); // !!! make sure trace is not released before other tools

				for (final Map.Entry<Tool<?>, Object> entry : resources.entrySet()) {

					final Tool<Object> tool=(Tool<Object>)entry.getKey();
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

				tools.clear();
				resources.clear();

			}
		}
	}


	public Tray with(final Consumer<Tray> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		final Tray current=context.get();

		try {

			context.set(this);

			task.accept(this);

			return this;

		} finally {
			context.set(current);
		}
	}

	public Tray exec(final Runnable task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return with(tray -> task.run());
	}

}
