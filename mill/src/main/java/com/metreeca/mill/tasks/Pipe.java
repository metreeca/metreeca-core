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
import com.metreeca.tray.Tool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import static java.util.Arrays.asList;


/**
 * Serial composite task.
 */
public final class Pipe implements Task {

	private final Collection<Task> tasks;


	public Pipe(final Task... tasks) {
		this(asList(tasks));
	}

	public Pipe(final Collection<Task> tasks) {

		if ( tasks == null ) {
			throw new NullPointerException("null tasks");
		}

		if ( tasks.contains(null) ) {
			throw new NullPointerException("null task");
		}

		this.tasks=new ArrayList<>(tasks);
	}


	@Override public Stream<_Cell> execute(final Tool.Loader tools, final Stream<_Cell> items) {

		Stream<_Cell> pipe=items;

		for (final Task task : tasks) {
			pipe=task.execute(tools, pipe);
		}

		return pipe;
	}

}
