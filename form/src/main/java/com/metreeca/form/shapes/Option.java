/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.form.shapes;

import com.metreeca.form.Shape;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.things.Strings.indent;


/**
 * Conditional logical constraint.
 *
 * <p>States that the focus set is consistent either with a {@linkplain #getPass() positive} shape, if consistent also
 * with a {@linkplain #getTest() test} shape, or with a {@linkplain #getFail() negative} shape, otherwise.</p>
 */
public final class Option implements Shape {

	public static Option condition(final Shape test, final Shape pass) {
		return new Option(test, pass, and());
	}

	public static Option condition(final Shape test, final Shape pass, final Shape fail) {
		return new Option(test, pass, fail);
	}


	private final Shape test;
	private final Shape pass;
	private final Shape fail;


	public Option(final Shape test, final Shape pass, final Shape fail) {

		if ( test == null ) {
			throw new NullPointerException("null test shape");
		}

		if ( pass == null ) {
			throw new NullPointerException("null pass shape");
		}

		if ( fail == null ) {
			throw new NullPointerException("null fail shape");
		}

		this.test=test;
		this.pass=pass;
		this.fail=fail;
	}


	public Shape getTest() {
		return test;
	}

	public Shape getPass() {
		return pass;
	}

	public Shape getFail() {
		return fail;
	}


	@Override public <T> T accept(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.visit(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Option
				&& test.equals(((Option)object).test)
				&& pass.equals(((Option)object).pass)
				&& fail.equals(((Option)object).fail);
	}

	@Override public int hashCode() {
		return test.hashCode()^pass.hashCode()^fail.hashCode();
	}

	@Override public String toString() {
		return "condition(\n"
				+indent(test.toString())+",\n"
				+indent(pass.toString())
				+(fail.equals(and()) ? "" : ",\n"+indent(fail.toString()))
				+"\n)";
	}

}
