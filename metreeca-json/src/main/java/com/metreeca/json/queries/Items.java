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

import com.metreeca.json.*;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;


public final class Items extends Query {

	public static Items items(final Shape shape) {
		return new Items(shape, emptyList(), 0, 0);
	}

	public static Items items(final Shape shape, final Order... orders) {
		return new Items(shape, asList(orders), 0, 0);
	}

	public static Items items(final Shape shape, final List<Order> orders, final int offset, final int limit) {
		return new Items(shape, orders, offset, limit);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Shape shape;

	private final List<Order> orders;

	private final int offset;
	private final int limit;


	private Items(final Shape shape, final List<Order> orders, final int offset, final int limit) {

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


	public Shape shape() {
		return shape;
	}

	public List<Order> orders() {
		return unmodifiableList(orders);
	}

	public int offset() {
		return offset;
	}

	public int limit() {
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
		return this == object || object instanceof Items
				&& shape.equals(((Items)object).shape)
				&& orders.equals(((Items)object).orders)
				&& offset == ((Items)object).offset
				&& limit == ((Items)object).limit;
	}

	@Override public int hashCode() {
		return shape.hashCode()
				^orders.hashCode()
				^Integer.hashCode(offset)
				^Integer.hashCode(limit);
	}

	@Override public String toString() {
		return format(
				"items {\n\tshape: %s\n\torder: %s\n\toffset: %d\n\tlimit: %d\n}",
				shape.toString().replace("\n", "\n\t"), orders, offset, limit
		);
	}

}
