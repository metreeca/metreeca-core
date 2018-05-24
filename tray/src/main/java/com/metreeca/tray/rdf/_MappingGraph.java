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

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.repository.Repository;

import java.util.function.Supplier;


final class _MappingGraph extends Graph {

	private final String external;
	private final String internal;


	_MappingGraph(final String external, final String internal,
			final String description, final IsolationLevel isolation, final Supplier<Repository> connector) {

		super(description, isolation, () -> {

			final Repository repository=connector.get();

			return new MappingRepository(new _Mapping(external, internal, repository), repository);

		});

		this.external=external;
		this.internal=internal;
	}


	@Override public Graph map(final String external, final String internal) { // idempotent mapping

		if ( external == null ) {
			throw new NullPointerException("null external");
		}

		if ( internal == null ) {
			throw new NullPointerException("null internal");
		}

		return external.equals(this.external) && internal.equals(this.internal) ? this : super.map(external, internal);
	}

	@Override public void close() {} // don't auto-close mapped repository

}
