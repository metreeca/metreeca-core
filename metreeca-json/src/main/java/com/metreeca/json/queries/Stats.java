/*
 * Copyright © 2013-2020 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.json.queries;

import com.metreeca.json.Query;
import com.metreeca.json.Shape;

import org.eclipse.rdf4j.model.IRI;

import java.util.*;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;


public final class Stats extends Query {

	public static Stats stats(final Shape shape, final IRI... path) {
		return new Stats(shape, asList(path));
	}

	public static Stats stats(final Shape shape, final List<IRI> path) {
		return new Stats(shape, path);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Shape shape;

	private final List<IRI> path;


	private Stats(final Shape shape, final List<IRI> path) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( path == null || path.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null path or path step");
		}

		this.shape=shape;
		this.path=new ArrayList<>(path);
	}


	public Shape shape() {
		return shape;
	}

	public List<IRI> path() {
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
