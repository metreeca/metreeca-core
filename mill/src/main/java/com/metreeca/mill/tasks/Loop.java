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

package com.metreeca.mill.tasks;

import com.metreeca.mill.Task;
import com.metreeca.mill._Cell;

import org.eclipse.rdf4j.model.Value;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;


/**
 * Iterative expansion task.
 *
 * <p>Starting with the incoming feed, iteratively executes a task until no new focus values in generated cells are
 * found.</p>
 *
 * <p>The incoming feed and all the generated cells are then merged into a single outgoing feed.</p>
 */
public final class Loop implements Task {

	private final Task task;


	public Loop(final Task task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		this.task=task;
	}


	@Override public Stream<_Cell> execute(final Stream<_Cell> items) {

		final Map<Value, _Cell> outgoing=new LinkedHashMap<>(); // preserve order

		items.forEachOrdered(cell -> outgoing.put(cell.focus(), cell));

		for (Collection<_Cell> incoming=new ArrayList<>(outgoing.values()); !incoming.isEmpty(); ) { // concurrent modification
			incoming=task
					.execute(incoming.stream())
					.sequential()
					.filter(cell -> outgoing.putIfAbsent(cell.focus(), cell) == null)
					.collect(toList());
		}

		return outgoing.values().stream();
	}

}
