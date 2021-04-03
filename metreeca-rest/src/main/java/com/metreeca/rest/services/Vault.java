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

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;


/**
 * Secret vault.
 *
 * <p>Retrieves sensitive configuration parameters from safe system storage.</p>
 */
@FunctionalInterface public interface Vault {

	/**
	 * Retrieves the default vault factory.
	 *
	 * @return the default vault factory, which retrieves parameters either from {@linkplain System#getProperties()
	 * system properties} or {@linkplain System#getenv() environment variables}, in that order
	 */
	public static Supplier<Vault> vault() {
		return () -> id -> {

			if ( id.isEmpty() ) {
				throw new IllegalArgumentException("empty parameter id");
			}

			return Stream

					.<UnaryOperator<String>>of(
							System::getProperty,
							System::getenv
					)

					.map(source -> source.apply(id))
					.filter(Objects::nonNull)
					.findFirst();
		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves a sensitive configuration parameters.
	 *
	 * @param id the unique identifier of the parameter to be retrieved
	 *
	 * @return an optional containing the value of the parameter identified by {@code id}, if one is present in the
	 * vault; an empty optional, otherwise
	 *
	 * @throws NullPointerException     if {@code id} is null
	 * @throws IllegalArgumentException if {@code id} is empty
	 */
	public Optional<String> get(final String id);

}
