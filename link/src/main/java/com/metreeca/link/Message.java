/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.link;

import java.util.*;

import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;


/**
 * HTTP message.
 *
 * @param <T> the self-bounded message type for method chaining
 */
public abstract class Message<T extends Message<T>> {

	private final Map<String, Collection<String>> headers=new LinkedHashMap<>();


	protected abstract T self();


	public Map<String, Collection<String>> getHeaders() {
		return unmodifiableMap(headers);
	}

	public Collection<String> getHeaders(final String name) { // !!! § no null values / case-insensitive

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return headers.entrySet().stream() // case-insensitive search
				.filter(entry -> entry.getKey().equalsIgnoreCase(name))
				.findFirst()
				.map(Map.Entry::getValue)
				.orElseGet(Collections::emptySet);
	}

	public Optional<String> getHeader(final String name) { // !!! § no null values / case-insensitive

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return getHeaders(name).stream().findFirst();
	}


	public void setHeader(final String name, final String value, final Object... args) {
		setHeader(name, String.format(value, args));
	}

	public T setHeader(final String name, final String value) { // !!! § no null values / case-insensitive

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		if ( value.isEmpty() ) {
			headers.remove(name);
		} else {
			headers.put(name, singleton(value));
		}

		return self();
	}

	public void addHeader(final String name, final String value, final Object... args) {
		addHeader(name, String.format(value, args));
	}

	public T addHeader(final String name, final String value) { // !!! § no null values / case-insensitive

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		if ( !value.isEmpty() ) {
			headers.compute(name, (key, current) -> {
				if ( current == null ) { return singleton(value); } else {

					final Collection<String> values=new LinkedHashSet<>(current);

					values.add(value);

					return unmodifiableCollection(values);
				}
			});
		}

		return self();
	}

}
