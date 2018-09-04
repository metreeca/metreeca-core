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

package com.metreeca.form.things;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Operation;

import java.util.HashMap;
import java.util.Map;


/**
 * Operation bindings editor.
 *
 * <p>Provides a fluent interface for defining repository {@linkplain Operation operation} bindings.</p>
 */
public final class Bindings {

	/**
	 * Creates a new bindings editor.
	 *
	 * @return an empty bindings editor
	 */
	public static Bindings bindings() {
		return new Bindings();
	}


	/**
	 * Maps bindings names to bindings values.
	 */
	private final Map<String, Value> bindings=new HashMap<>();


	private Bindings() {}


	/**
	 * Defines an operation binding.
	 *
	 * <p>Binds a specified variable to a supplied value. Any value that was previously bound to the specified value
	 * will be overwritten.</p>
	 *
	 * @param name  the name of the variable to be bound
	 * @param value the (new) value for variable specified by {@code name}
	 *
	 * @return this binding editor
	 *
	 * @throws NullPointerException if either {@code name} or {@code value} is {@code null}
	 */
	public Bindings set(final String name, final Value value) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		bindings.put(name, value);

		return this;
	}


	/**
	 * Transfers bindings to a repository operation.
	 *
	 * @param operation the repository operation bindings are to be transferred to
	 * @param <T>       the type of {@code operation}
	 *
	 * @return the repository {@code operation} with additional bindings defined according to the current state of this
	 * bindings editor
	 *
	 * @throws NullPointerException if {@code operation} is {@code null}
	 */
	public <T extends Operation> T bind(final T operation) {

		if ( operation == null ) {
			throw new NullPointerException("null operation");
		}

		for (final Map.Entry<String, Value> binding : bindings.entrySet()) {
			operation.setBinding(binding.getKey(), binding.getValue());
		}

		return operation;
	}

}
