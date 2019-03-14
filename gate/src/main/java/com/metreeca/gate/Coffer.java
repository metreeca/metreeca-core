/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.gate;

import java.util.Optional;
import java.util.function.Supplier;


/**
 * Secret source.
 *
 * <p>Retrieves sensitive configuration parameters from safe system storage.</p>
 */
@FunctionalInterface public interface Coffer {

	/**
	 * Retrieves the default coffer factory.
	 *
	 * @return the default coffer factory, which retrieves parameters from {@linkplain System#getProperties() system
	 * properties}
	 */
	public static Supplier<Coffer> coffer() {
		return () -> id -> Optional.ofNullable(System.getProperty(id));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves a sensitive configuration parameters.
	 *
	 * @param id the unique identifier of the parameter to be retrieved
	 *
	 * @return an optional containing the value of the parameter identified by {@code id}, if one is present in the
	 * coffer; an empty optional, otherwise
	 *
	 * @throws NullPointerException if {@code id} is null
	 */
	public Optional<String> get(final String id);

}
