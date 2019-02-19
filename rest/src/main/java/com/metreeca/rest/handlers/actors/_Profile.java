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

package com.metreeca.rest.handlers.actors;

import com.metreeca.form.Shape;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Traverser;
import com.metreeca.form.shapes.*;
import com.metreeca.form.things.Sets;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.form.Shape.filter;
import static com.metreeca.form.probes.Evaluator.pass;
import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Field.fields;
import static com.metreeca.form.shapes.Memo.memoizable;
import static com.metreeca.form.shapes.Meta.meta;
import static com.metreeca.form.shapes.Meta.metas;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.When.when;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Values.inverse;

import static java.util.stream.Collectors.toList;


/**
 * LDP Container profile.
 *
 * <p>Manages containment/membership triples for LDP containers.</p>
 */
public abstract class _Profile {

	private static final Function<Shape, _Profile> profile=memoizable(shape -> {

		final Map<IRI, Value> metadata=metas(shape);
		final Value type=metadata.get(RDF.TYPE);

		return LDP.BASIC_CONTAINER.equals(type) ? new Basic()
				: LDP.DIRECT_CONTAINER.equals(type) ? new Direct(metadata)
				: new Basic();

	});


	public static Shape anchor(final Resource resource, final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return and(shape, filter().then(profile.apply(shape).anchor(resource)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private _Profile() {} // ADT


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected abstract Shape anchor(final Resource resource);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class None extends _Profile {

		private None() {}


		@Override public Shape anchor(final Resource resource) {
			return and();
		}

	}

	private static final class Basic extends _Profile {

		private Basic() {}


		@Override public Shape anchor(final Resource resource) {
			return field(inverse(LDP.CONTAINS), resource);
		}

	}

	private static final class Direct extends _Profile {

		private final IRI relation;

		private final Resource subject;
		private final Value object;


		private Direct(final Map<IRI, Value> metadata) {

			final Value direct=metadata.get(LDP.HAS_MEMBER_RELATION);
			final Value inverse=metadata.get(LDP.IS_MEMBER_OF_RELATION);

			final Value target=metadata.get(LDP.MEMBERSHIP_RESOURCE);

			if ( direct != null ) {

				this.relation=(direct instanceof IRI) ? (IRI)direct : null;
				this.subject=(target == null) ? LDP.CONTAINER : (target instanceof Resource) ? (Resource)target : null;
				this.object=null;

			} else if ( inverse != null ) {

				this.relation=(inverse instanceof IRI) ? (IRI)inverse : null;
				this.subject=null;
				this.object=(target == null) ? LDP.CONTAINER : target;

			} else {

				this.relation=LDP.MEMBER;
				this.subject=(target == null) ? LDP.CONTAINER : (target instanceof Resource) ? (Resource)target : null;
				this.object=null;

			}

		}

		@Override public Shape anchor(final Resource resource) {
			return relation != null && subject != null ? field(inverse(relation), subject)
					: relation != null && object != null ? field(relation, object)
					: and();
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
