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

package com.metreeca.form.queries;

import com.metreeca.form.Form;
import com.metreeca.form.Query;
import com.metreeca.form.Shape;
import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

import java.util.ArrayList;
import java.util.List;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.things.Strings.indent;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;


public final class Items implements Query {

	public static final Shape Shape=and(
			field(Form.items, and(
					datatype(Values.BNodeType),
					field(Form.count, and(maxCount(1), datatype(XMLSchema.INTEGER))),
					field(Form.value, and(maxCount(1),
							field(RDFS.LABEL, and(maxCount(1), datatype(XMLSchema.STRING))),
							field(RDFS.COMMENT, and(maxCount(1), datatype(XMLSchema.STRING)))
					))
			))
	);


	public static Items items(final Shape shape, final List<IRI> path) {
		return new Items(shape, path);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Shape shape;

	private final List<IRI> path;


	private Items(final Shape shape, final List<IRI> path) {

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

	public List<IRI> getPath() {
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
		return this == object || object instanceof Items
				&& shape.equals(((Items)object).shape)
				&& path.equals(((Items)object).path);
	}

	@Override public int hashCode() {
		return shape.hashCode()^path.hashCode();
	}


	@Override public String toString() {
		return format(
				"items {\n\tshape: %s\n\tpath: %s\n}",
				indent(shape, true), path
		);
	}

}
