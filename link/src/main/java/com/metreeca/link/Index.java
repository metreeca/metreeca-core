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

package com.metreeca.link;

import com.metreeca.tray.Tool;

import java.util.*;
import java.util.function.Function;


/**
 * Linked data handlers index {thread-safe}.
 */
public class Index {

	public static final Tool<Index> Tool=loader -> new Index();


	private static boolean matches(final String x, final String y) {
		return x.equals(y) || y.endsWith("/") && x.startsWith(y);
	}

	private static String normalize(final String path) {
		return path.endsWith("?") || path.endsWith("/") || path.endsWith("/*") ? path.substring(0, path.length()-1) : path;
	}


	private final Map<String, Handler> handlers=new TreeMap<>(Comparator.comparingInt(String::length).reversed() // longest paths first
			.thenComparing(String::compareTo)); // then alphabetically


	private Index() {}


	public Optional<Handler> lookup(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return exec(index -> {

			final String key=normalize(path);

			return index.handlers.entrySet().stream().filter(entry -> matches(key, entry.getKey())).map(Map.Entry::getValue).findFirst();

		});
	}

	public Index insert(final String path, final Handler handler) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( path.isEmpty() ) { // !!! test pattern
			throw new IllegalArgumentException("illegal path ["+path+"]");
		}

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		return exec(index -> {

			final String key=normalize(path);

			if ( index.handlers.containsKey(key) ) {
				throw new IllegalStateException("path is already mapped {"+path+"}");
			}

			index.handlers.put(key, handler);

			return index;

		});
	}

	public Index remove(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return exec(index -> {

			index.handlers.remove(normalize(path));

			return index;

		});
	}


	/**
	 * Executes a task within an isolated transaction.
	 *
	 * @param task the task to be executed
	 * @param <R>  the type of the value returned by {@code task}
	 *
	 * @return the value returned by {@code task}
	 */
	public <R> R exec(final Function<Index, R> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		synchronized ( handlers ) { return task.apply(this); }
	}

}
