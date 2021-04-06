/*
 * Copyright © 2013-2021 Metreeca srl
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

package com.metreeca.json.shifts;

import org.eclipse.rdf4j.model.IRI;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Sequence path.
 */
public final class Seq extends Path {

	public static Path seq(final IRI... paths) {

		if ( paths == null || stream(paths).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null paths");
		}

		return seq(stream(paths).map(Step::step));
	}

	public static Path seq(final Path... paths) {

		if ( paths == null || stream(paths).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null paths");
		}

		return seq(stream(paths).sequential());
	}

	public static Path seq(final Collection<Path> paths) {

		if ( paths == null || paths.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null paths");
		}

		return seq(paths.stream().sequential());
	}


	private static Path seq(final Stream<Path> paths) {

		final List<Path> list=paths.collect(toList());

		return list.size() == 1 ? list.get(0) : new Seq(list);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final List<Path> paths;


	private Seq(final List<Path> paths) {
		this.paths=unmodifiableList(paths);
	}


	public List<Path> paths() {
		return paths;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <V> V map(final Probe<V> probe) {
		return probe.probe(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Seq
				&& paths.equals(((Seq)object).paths);
	}

	@Override public int hashCode() {
		return paths.hashCode();
	}

	@Override public String toString() {
		return paths.stream().map(Object::toString).collect(joining("/", "(", ")"));
	}

}
