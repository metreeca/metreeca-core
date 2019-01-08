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
import com.metreeca.form.things.Strings;

import static com.metreeca.form.shapes.And.and;


/**
 * Conditional logical constraint.
 *
 * <p>States that the focus set is consistent either with a 'positive' shape, if consistent also with a 'condition'
 * shape, or with a 'negative' shape, otherwise.</p>
 */
public final class Test implements Shape {

	public static Test test(final Shape condition, final Shape positive) {
		return new Test(condition, positive, and());
	}

	public static Test test(final Shape condition, final Shape positive, final Shape negative) {
		return new Test(condition, positive, negative);
	}


	private final Shape test;
	private final Shape pass;
	private final Shape fail;


	public Test(final Shape test, final Shape pass, final Shape fail) {

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
		return this == object || object instanceof Test
				&& test.equals(((Test)object).test)
				&& pass.equals(((Test)object).pass)
				&& fail.equals(((Test)object).fail);
	}

	@Override public int hashCode() {
		return test.hashCode()^pass.hashCode()^fail.hashCode();
	}

	@Override public String toString() {
		return "test(\n"
				+Strings.indent(test.toString())+",\n"
				+Strings.indent(pass.toString())
				+(fail.equals(and()) ? "" : ",\n"+Strings.indent(fail.toString()))
				+"\n)";
	}

}
