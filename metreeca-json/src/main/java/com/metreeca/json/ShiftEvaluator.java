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

package com.metreeca.json;

import com.metreeca.json.shifts.*;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.stream.Stream;

import static com.metreeca.json.Values.traverse;

import static java.util.stream.Collectors.toCollection;

public final class ShiftEvaluator extends Shift.Probe<Stream<Value>> {

	private final Collection<Value> values;
	private final Collection<Statement> statements;


	ShiftEvaluator(final Collection<Value> values, final Collection<Statement> statements) {
		this.values=values;
		this.statements=statements;
	}


	@Override public Stream<Value> probe(final Step step) {
		return traverse(step.iri(),

				direct -> statements.stream()
						.filter(s -> values.contains(s.getSubject()) && direct.equals(s.getPredicate()))
						.map(Statement::getObject),

				inverse -> statements.stream()
						.filter(s -> inverse.equals(s.getPredicate()) && values.contains(s.getObject()))
						.map(Statement::getSubject)

		);
	}

	@Override public Stream<Value> probe(final Seq seq) {

		Collection<Value> focus=values;

		for (final Path path : seq.paths()) {
			focus=path.apply(focus, statements).collect(toCollection(LinkedHashSet::new));
		}

		return focus.stream();
	}

	@Override public Stream<Value> probe(final Alt alt) {
		return alt.paths().stream().flatMap(path -> path.apply(values, statements));
	}


	@Override public Stream<Value> probe(final Shift shift) {
		throw new UnsupportedOperationException(shift.toString());
	}

}
