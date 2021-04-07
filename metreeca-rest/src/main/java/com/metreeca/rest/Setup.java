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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;

/**
 * Customizable component.
 *
 * <p>Manages customizable configuration parameters.</p>
 *
 * @param <T> the self-bounded setup type supporting fluent setters
 */
@SuppressWarnings("unchecked")
public abstract class Setup<T extends Setup<T>> implements Config {

	private final Map<Supplier<?>, Object> options=new LinkedHashMap<>();


	protected T self() { return (T)this; }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves an option.
	 *
	 * @param option the option to be retrieved; must return a non-null default value
	 * @param <V>    the type of the option to be retrieved
	 *
	 * @return the value previously {@linkplain #set(Supplier, Object) configured} for {@code option} or its default
	 * value, if no custom value was configured; in the latter case the returned value is cached
	 *
	 * @throws NullPointerException if {@code option} is null or returns a null value
	 */
	public <V> V get(final Supplier<V> option) {

		if ( option == null ) {
			throw new NullPointerException("null option");
		}

		return (V)options.computeIfAbsent(option, key ->
				requireNonNull(key.get(), "null option return value")
		);

	}

	/**
	 * Configures an option.
	 *
	 * @param option the option to be configured; must return a non-null default value
	 * @param value  the value to be configured for {@code option}
	 * @param <V>    the type of the option to be configured
	 *
	 * @return this setup
	 *
	 * @throws NullPointerException if either {@code option} or {@code value} is null
	 */
	public <V> T set(final Supplier<V> option, final V value) {

		if ( option == null ) {
			throw new NullPointerException("null option");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		options.put(option, value);

		return self();
	}

	/**
	 * Updates an option.
	 *
	 * @param option the option to be update; must return a non-null default value
	 * @param mapper a function mapping from the current to the updated value of {@code option}; must return a non-null
	 *               value
	 * @param <V>    the type of the option to be updated
	 *
	 * @return this setup
	 *
	 * @throws NullPointerException if either {@code option} or {@code value} is null
	 */
	public <V> T map(final Supplier<V> option, final UnaryOperator<V> mapper) {

		if ( option == null ) {
			throw new NullPointerException("null option");
		}

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		options.computeIfPresent(option, (key, value) -> mapper.apply((V)value));

		return self();
	}

}
