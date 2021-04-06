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
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;

/**
 * Alternative path.
 */
public final class Alt extends Path {

	public static Path alt(final IRI... paths) {

		if ( paths == null || stream(paths).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null paths");
		}

		return alt(stream(paths).map(Step::step));
	}

	public static Path alt(final Path... paths) {

		if ( paths == null || stream(paths).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null paths");
		}

		return alt(stream(paths));
	}

	public static Path alt(final Collection<Path> paths) {

		if ( paths == null || paths.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null paths");
		}

		return alt(paths.stream());
	}


	private static Path alt(final Stream<Path> paths) {

		final Set<Path> set=paths.collect(toCollection(LinkedHashSet::new));

		return set.size() == 1 ? set.iterator().next() : new Alt(set);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Set<Path> paths;


	private Alt(final Set<Path> paths) {
		this.paths=unmodifiableSet(paths);
	}


	public Set<Path> paths() {
		return paths;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <V> V map(final Probe<V> probe) {
		return probe.probe(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Alt
				&& paths.equals(((Alt)object).paths);
	}

	@Override public int hashCode() {
		return paths.hashCode();
	}

	@Override public String toString() {
		return paths.stream().map(Object::toString).collect(joining("|", "(", ")"));
	}

}
