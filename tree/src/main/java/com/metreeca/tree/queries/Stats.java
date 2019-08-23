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

package com.metreeca.tree.queries;

import com.metreeca.tree.Query;
import com.metreeca.tree.Shape;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;


public final class Stats implements Query {

	public static Stats stats(final Shape shape, final String... path) {
		return new Stats(shape, asList(path));
	}

	public static Stats stats(final Shape shape, final List<String> path) {
		return new Stats(shape, path);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Shape shape;

	private final List<String> path;


	private Stats(final Shape shape, final List<String> path) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( path.contains(null) ) {
			throw new IllegalArgumentException("illegal path step");
		}

		this.shape=shape;
		this.path=new ArrayList<>(path);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Shape getShape() {
		return shape;
	}

	public List<String> getPath() {
		return unmodifiableList(path);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Stats
				&& shape.equals(((Stats)object).shape)
				&& path.equals(((Stats)object).path);
	}

	@Override public int hashCode() {
		return shape.hashCode()^path.hashCode();
	}


	@Override public String toString() {
		return format(
				"stats {\n\tshape: %s\n\tpath: %s\n}",
				shape.toString().replace("\n", "\n\t"), path
		);
	}

}
