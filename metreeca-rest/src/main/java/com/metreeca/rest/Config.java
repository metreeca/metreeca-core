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

import java.util.function.Supplier;

/**
 * Component configuration.
 *
 * <p>Provides access to customizable configuration parameters.</p>
 */
@FunctionalInterface public interface Config {

	/**
	 * Retrieves an option.
	 *
	 * @param option the option to be retrieved; must return a non-null default value
	 * @param <V>    the type of the option to be retrieved
	 *
	 * @return the custom value configured for {@code option} or its default value, if no custom value is configured
	 *
	 * @throws NullPointerException if {@code option} is null or returns a null value
	 */
	public <V> V get(final Supplier<V> option);

}
