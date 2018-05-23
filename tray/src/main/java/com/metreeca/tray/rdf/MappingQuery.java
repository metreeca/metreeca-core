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

import org.eclipse.rdf4j.query.Query;


final class MappingQuery extends MappingOperation<Query> implements Query {

	MappingQuery(final _Mapping mapping, final Query query) { super(mapping, query); }


	@Deprecated @Override public void setMaxQueryTime(final int maxQueryTime) {
		operation.setMaxQueryTime(maxQueryTime);
	}

	@Deprecated @Override public int getMaxQueryTime() {
		return operation.getMaxQueryTime();
	}

}
