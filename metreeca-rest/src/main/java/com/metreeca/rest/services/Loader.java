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

package com.metreeca.rest.services;

import java.net.URL;
import java.util.Optional;
import java.util.function.Supplier;


/**
 * Resource loader.
 *
 * <p>Loads shared resources from a system-specific source.</p>
 */
@FunctionalInterface public interface Loader {

	/**
	 * Retrieves the default loader factory.
	 *
	 * @return the default loader factory, which retrieves system resources from the classpath through {@link
	 * ClassLoader#getResource(String)}
	 */
	public static Supplier<Loader> loader() {
		return () -> path -> {

			if ( path == null ) {
				throw new NullPointerException("null path");
			}

			return Optional.ofNullable(Loader.class.getClassLoader().getResource(path));
		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Loads a shared resource.
	 *
	 * @param path the path the system resource should be loaded from; path syntax is source dependent, but a
	 *             filesystem-like slash-separated hierarchical structure is recommended
	 *
	 * @return an optional URL for the required resource, if one is available at {@code path}; an empty optional,
	 * otherwise
	 *
	 * @throws NullPointerException     if {@code path} is null
	 * @throws IllegalArgumentException if {@code path} syntax is illegal according to source-specific rules
	 */
	public Optional<URL> load(final String path);

}
