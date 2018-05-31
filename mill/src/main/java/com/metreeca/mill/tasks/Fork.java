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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;


/**
 * Parallel composite task.
 */
public final class Fork implements Task {

	private final Collection<Task> tasks;


	public Fork(final Task... tasks) {
		this(asList(tasks));
	}

	public Fork(final Collection<Task> tasks) {

		if ( tasks == null ) {
			throw new NullPointerException("null tasks");
		}

		if ( tasks.contains(null) ) {
			throw new NullPointerException("null task");
		}

		this.tasks=new ArrayList<>(tasks);
	}


	@Override public Stream<_Cell> execute(final Stream<_Cell> items) {

		final List<_Cell> buffer=items.collect(toList()); // !!! streaming forking

		return tasks.stream().flatMap(task -> task.execute(buffer.stream()));
	}

}
