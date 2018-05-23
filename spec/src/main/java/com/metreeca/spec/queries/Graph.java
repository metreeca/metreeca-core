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

package com.metreeca.spec.queries;

import com.metreeca.spec.Query;
import com.metreeca.spec.Shape;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;


public final class Graph implements Query { // !!! rename to Edges to avoid clashes? / review docs

	private final Shape shape;

	private final List<Order> orders;

	private final int offset;
	private final int limit;


	public Graph(final Shape shape) {
		this(shape, emptyList(), 0, 0);
	}

	public Graph(final Shape shape, final List<Order> orders, final int offset, final int limit) {

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


	@Override public <T> T accept(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.visit(this);
	}

}
