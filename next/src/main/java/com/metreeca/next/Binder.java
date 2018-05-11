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

package com.metreeca.next;

import com.metreeca.spec.things.Values;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Operation;

import java.util.HashMap;
import java.util.Map;

import static com.metreeca.spec.things.Values.iri;
import static com.metreeca.spec.things.Values.literal;


public final class Binder {

	public static Binder binder() {
		return new Binder();
	}


	private final Map<String, Value> bindings=new HashMap<>();


	private Binder() {}


	public Binder time() {
		return set("time", Values.time());
	}

	public Binder user(final IRI user) {

		if ( user == null ) {
			throw new NullPointerException("null user");
		}

		return set("user", user);
	}

	public Binder focus(final IRI focus) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		return set("this", focus)
				.set("stem", iri(focus.getNamespace()))
				.set("code", literal(focus.getLocalName()));

	}


	public Binder set(final String name, final Value value) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( value != null ) {
			bindings.put(name, value);
		} else {
			bindings.remove(name);
		}

		return this;
	}


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
