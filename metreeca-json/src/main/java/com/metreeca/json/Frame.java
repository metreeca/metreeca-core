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

import com.metreeca.json.shifts.Path;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.util.*;
import java.util.stream.Stream;

import static com.metreeca.json.Values.*;
import static com.metreeca.json.shifts.Alt.alt;
import static com.metreeca.json.shifts.Seq.seq;
import static com.metreeca.json.shifts.Step.step;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

/**
 * Linked data frame.
 *
 * <p>Describes a linked data graph centered on a focus resource.</p>
 */
public final class Frame {

	private static final Path Labels=alt(
			RDFS.LABEL, DC.TITLE, iri("http://schema.org/", "name")
	);

	private static final Path Notes=alt(
			RDFS.COMMENT, DC.DESCRIPTION, iri("http://schema.org/", "description")
	);


	public static Frame frame(final Resource focus) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		return new Frame(focus, emptySet());
	}

	public static Frame frame(final Resource focus, final Collection<Statement> model) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( model == null || model.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null model or model statement");
		}

		return new Frame(focus, merge(focus, emptySet(), model));
	}


	private static Set<Statement> merge(
			final Resource focus, final Set<Statement> model, final Iterable<Statement> delta
	) {

		final Set<Statement> merged=new LinkedHashSet<>(model);

		final Collection<Resource> visited=new HashSet<>();
		final Queue<Resource> pending=new ArrayDeque<>(singleton(focus));

		while ( !pending.isEmpty() ) {

			final Resource value=pending.poll();

			if ( visited.add(value) ) {
				delta.forEach(s -> {
					if ( value.equals(s.getSubject()) || value.equals(s.getObject()) ) {

						pending.add(s.getSubject());
						pending.add(s.getPredicate());

						if ( s.getObject().isResource() ) {
							pending.add((Resource)s.getObject());
						}

						merged.add(s);
					}
				});
			}
		}

		return merged;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Resource focus;
	private final Set<Statement> model;


	private Frame(final Resource focus, final Set<Statement> model) {
		this.focus=focus;
		this.model=unmodifiableSet(model);
	}


	public Optional<String> label() {
		return string(get(Labels));
	}

	public Optional<String> notes() {
		return string(get(Notes));
	}


	/**
	 * Retrieves the frame focus.
	 *
	 * @return theIRI of the frame focus resource.
	 */
	public Resource focus() {
		return focus;
	}

	public Set<Statement> model() {
		return model;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Stream<Statement> get() {
		return model.stream();
	}


	public Frame add(final Statement... statements) {

		if ( statements == null || Arrays.stream(statements).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null statements");
		}

		return new Frame(focus, merge(focus, model, Arrays.stream(statements).collect(toList())));
	}

	public Frame add(final Collection<Statement> statements) {

		if ( statements == null || statements.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null statements");
		}

		return new Frame(focus, merge(focus, model, statements));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Stream<Value> get(final IRI step) {

		if ( step == null ) {
			throw new NullPointerException("null step");
		}

		return get(step(step));
	}

	public Stream<Value> get(final IRI... steps) {

		if ( steps == null || Arrays.stream(steps).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null steps");
		}

		return get(seq(steps));
	}

	public Stream<Value> get(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return shift.apply(singleton(focus), model);
	}


	public Frame add(final IRI predicate, final Object value) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return value == null ? this : add(predicate, Stream.of(value));
	}

	public Frame add(final IRI predicate, final Optional<?> value) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return value.map(object -> add(predicate, Stream.of(object))).orElse(this);
	}

	public Frame add(final IRI predicate, final Object... values) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		return values.length == 0 ? this : add(predicate, Arrays.stream(values));
	}

	public Frame add(final IRI predicate, final Collection<?> values) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		return values.isEmpty() ? this : add(predicate, values.stream());
	}

	public Frame add(final IRI predicate, final Stream<?> values) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		return new Frame(focus, Stream.concat(model.stream(), values

				.filter(Objects::nonNull)
				.map(Values::promote)

				.flatMap(value -> traverse(predicate,

						direct -> {

							if ( value instanceof Frame ) {

								return Stream.concat(
										link(focus, direct, ((Frame)value).focus),
										((Frame)value).model.stream()
								);

							} else {

								return link(focus, predicate, value(value));

							}

						},

						inverse -> {

							if ( value instanceof Frame ) {

								return Stream.concat(
										link(((Frame)value).focus, inverse, focus),
										((Frame)value).model.stream()
								);

							} else {

								return link(value(value), inverse, focus);

							}

						}

				))

		).collect(toCollection(LinkedHashSet::new)));
	}


	private Stream<Statement> link(final Value subject, final IRI predicate, final Value object) {
		if ( subject instanceof Resource ) {

			return Stream.of(statement((Resource)subject, predicate, object));

		} else {

			throw new IllegalArgumentException("literal value for inverse field");

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || focus.equals(object);
	}

	@Override public int hashCode() {
		return focus.hashCode();
	}

	@Override public String toString() {
		return format(focus)
				+label().map(l -> " : "+l).orElse("")
				+notes().map(l -> " / "+l).orElse("");
	}

}
