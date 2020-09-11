/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.json;

import com.metreeca.json.probes.Inspector;
import com.metreeca.json.shapes.*;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;


final class Evaluator extends Inspector<Boolean> {

	static final Evaluator Instance=new Evaluator();


	@Override public Boolean probe(final Meta meta) {
		return true;
	}


	@Override public Boolean probe(final Field field) {
		return null;
	}

	@Override public Boolean probe(final And and) {
		return and.shapes().stream()
				.filter(shape -> !(shape instanceof Meta))
				.map(shape -> shape.map(this))
				.reduce(true, (x, y) -> x == null || y == null ? null : x && y);
	}

	@Override public Boolean probe(final Or or) {
		return or.shapes().stream()
				.filter(shape -> !(shape instanceof Meta))
				.map(shape -> shape.map(this))
				.reduce(false, (x, y) -> x == null || y == null ? null : x || y);
	}

	@Override public Boolean probe(final When when) {

		final Boolean test=when.test().map(this);
		final Boolean pass=when.pass().map(this);
		final Boolean fail=when.fail().map(this);

		return TRUE.equals(test) ? pass
				: FALSE.equals(test) ? fail
				: TRUE.equals(pass) && TRUE.equals(fail) ? TRUE
				: FALSE.equals(pass) && FALSE.equals(fail) ? FALSE
				: null;
	}

}
