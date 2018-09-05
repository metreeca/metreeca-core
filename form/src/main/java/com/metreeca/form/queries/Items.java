/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.form.queries;

import com.metreeca.form.Form;
import com.metreeca.form.shifts.Step;
import com.metreeca.form.things.Values;
import com.metreeca.form.Query;
import com.metreeca.form.Shape;

import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.util.ArrayList;
import java.util.List;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.shifts.Step.step;

import static java.util.Collections.unmodifiableList;


public final class Items implements Query {

	public static final Shape ItemsShape=and(
			trait(Form.items, and(
					datatype(Values.BNodeType),
					trait(Step.step(Form.count), maxCount(1)),
					trait(Step.step(Form.value), and(
							maxCount(1),
							trait(Step.step(RDFS.LABEL), maxCount(1))
					))
			))
	);


	private final Shape shape;

	private final List<Step> path;


	public Items(final Shape shape, final List<Step> path) {

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


	public Shape getShape() {
		return shape;
	}

	public List<Step> getPath() {
		return unmodifiableList(path);
	}


	@Override public <T> T accept(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.visit(this);
	}

}
