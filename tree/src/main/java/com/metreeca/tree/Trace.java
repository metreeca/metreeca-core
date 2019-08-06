/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.tree;

import java.util.*;


/**
 * Shape value validation trace.
 */
public final class Trace {

	private final List<String> issues;
	private final Map<String, Trace> fields;


	public Trace(final List<String> issues, final Map<String, Trace> fields) {

		if ( issues == null || issues.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null issues");
		}

		if ( fields == null
				|| fields.keySet().stream().anyMatch(Objects::isNull)
				|| fields.values().stream().anyMatch(Objects::isNull)
		) {
			throw new NullPointerException("null fields");
		}

		this.issues=issues;
		this.fields=fields;
	}



}
