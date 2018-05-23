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

/**
 * Shared resource factory {thread-safe}.
 *
 * <p>Concrete implementations must be thread-safe.</p>
 *
 * @param <T> the type of the resource created by the factory
 */
@FunctionalInterface public interface Tool<T> {

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
	public T create(final Loader tools);


	/**
	 * Tool loader.
	 *
	 * <p>Supports creation of shared resource from tools.</p>
	 */
	@FunctionalInterface static interface Loader {

		/**
		 * Retrieves the shared resource created by a tool.
		 *
		 * <p>The {@linkplain Tool#create(Loader) new resource} should be cached so that further calls for the
		 * same tool are idempotent.</p>
		 *
		 * @param tool the tool creating the required resource
		 * @param <T>  the type of the shared resource created by {@code tool}
		 *
		 * @return the shared resource created by {@code tool}
		 *
		 * @throws IllegalArgumentException if {@code tool} is {@code null}
		 */
		public <T> T get(final Tool<T> tool);

	}

	/**
	 * Tool binder.
	 *
	 * <p>Supports tool replacement with alternative plugin implementations.</p>
	 */
	@FunctionalInterface static interface Binder {

		/**
		 * Replaces a tool with a plugin.
		 *
		 * <p>Subsequent calls to {@link Loader#get(Tool)} on the underlying implementation using {@code tool} as key
		 * must return the shared resource created by {@code plugin}.</p>
		 *
		 * @param <T>    the type of the shared resource created by {@code tool}
		 * @param tool   the tool to be replaced
		 * @param plugin the replacing tool
		 *
		 * @return this binder
		 *
		 * @throws IllegalArgumentException if either {@code tool} or {@code pluging} is {@code null}
		 * @throws IllegalStateException    if {@code tool} was already replaced with a plugin or its resource
		 *                                  was already created
		 */
		public <T> Binder set(final Tool<T> tool, final Tool<T> plugin) throws IllegalStateException;

	}

}
