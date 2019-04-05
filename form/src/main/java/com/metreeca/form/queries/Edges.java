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

package com.metreeca.form.queries;

import com.metreeca.form.Order;
import com.metreeca.form.Query;
import com.metreeca.form.Shape;

import java.util.ArrayList;
import java.util.List;

import static com.metreeca.form.things.Strings.indent;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;


public final class Edges implements Query {

	public static Edges edges(final Shape shape) {
		return new Edges(shape, emptyList(), 0, 0);
	}

	public static Edges edges(final Shape shape, final Order... orders) {
		return new Edges(shape, asList(orders), 0, 0);
	}

	public static Edges edges(final Shape shape, final List<Order> orders, final int offset, final int limit) {
		return new Edges(shape, orders, offset, limit);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Shape shape;

	private final List<Order> orders;

	private final int offset;
	private final int limit;


	private Edges(final Shape shape, final List<Order> orders, final int offset, final int limit) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( orders == null ) {
			throw new NullPointerException("null orders");
		}

		if ( offset < 0 ) {
			throw new IllegalArgumentException("illegal offset ["+offset+"]");
		}

		if ( limit < 0 ) {
			throw new IllegalArgumentException("illegal limit ["+limit+"]");
		}

		this.shape=shape;
		this.orders=new ArrayList<>(orders);
		this.offset=offset;
		this.limit=limit;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Shape getShape() {
		return shape;
	}

	public List<Order> getOrders() {
		return unmodifiableList(orders);
	}

	public int getOffset() {
		return offset;
	}

	public int getLimit() {
		return limit;
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
		return this == object || object instanceof Edges
				&& shape.equals(((Edges)object).shape)
				&& orders.equals(((Edges)object).orders)
				&& offset == ((Edges)object).offset
				&& limit == ((Edges)object).limit;
	}

	@Override public int hashCode() {
		return shape.hashCode()
				^orders.hashCode()
				^Integer.hashCode(offset)
				^Integer.hashCode(limit);
	}

	@Override public String toString() {
		return format(
				"edges {\n\tshape: %s\n\torder: %s\n\toffset: %d\n\tlimit: %d\n}",
				indent(shape, true), orders, offset, limit
		);
	}

}
