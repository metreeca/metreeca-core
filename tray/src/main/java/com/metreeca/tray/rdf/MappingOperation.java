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

package com.metreeca.tray.rdf;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.Operation;


abstract class MappingOperation<T extends Operation> implements Operation {

	protected final _Mapping mapping;
	protected final T operation;


	MappingOperation(final _Mapping mapping, final T operation) {
		this.mapping=mapping;
		this.operation=operation;
	}


	@Override public void setBinding(final String name, final Value value) {
		operation.setBinding(name, mapping.internal(value));
	}

	@Override public void removeBinding(final String name) {
		operation.removeBinding(name);
	}

	@Override public BindingSet getBindings() {
		return mapping.external(operation.getBindings());
	}

	@Override public void clearBindings() {
		operation.clearBindings();
	}


	@Override public void setDataset(final Dataset dataset) {
		operation.setDataset(mapping.internal(dataset));
	}

	@Override public Dataset getDataset() {
		return mapping.external(operation.getDataset());
	}


	@Override public void setIncludeInferred(final boolean includeInferred) {
		operation.setIncludeInferred(includeInferred);
	}

	@Override public boolean getIncludeInferred() {
		return operation.getIncludeInferred();
	}


	@Override public void setMaxExecutionTime(final int maxExecTime) {
		operation.setMaxExecutionTime(maxExecTime);
	}

	@Override public int getMaxExecutionTime() {
		return operation.getMaxExecutionTime();
	}

}
