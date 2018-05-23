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

package com.metreeca.tray.rdf;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;

import java.util.Iterator;
import java.util.Set;


final class MappingBindingSet implements BindingSet {

	private final _Mapping mapping;
	private final BindingSet bindings;


	MappingBindingSet(final _Mapping mapping, final BindingSet bindings) {
		this.mapping=mapping;
		this.bindings=bindings;
	}


	@Override public Iterator<Binding> iterator() {
		return mapping.external(mapping::external, bindings.iterator());
	}

	@Override public Set<String> getBindingNames() {
		return bindings.getBindingNames();
	}

	@Override public Binding getBinding(final String bindingName) {
		return mapping.external(bindings.getBinding(bindingName));
	}

	@Override public boolean hasBinding(final String bindingName) {
		return bindings.hasBinding(bindingName);
	}

	@Override public Value getValue(final String bindingName) {
		return mapping.external(bindings.getValue(bindingName));
	}

	@Override public int size() {
		return bindings.size();
	}

}
