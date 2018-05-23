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

import java.util.stream.Stream;


/**
 * Feed slicing task.
 *
 * <p>Extracts from the feed a slice defined by the given {@linkplain #limit(long) limit} and {@linkplain #offset(long)
 * offset}.</p>
 */
public final class Slice implements Task {

	private long limit;
	private long offset;


	/**
	 * Sets the slice limit.
	 *
	 * @param limit the maximum number of cells included in the processed feed; if equal to {@code 0}, no cell is
	 *              excluded
	 *
	 * @return this slice task
	 */
	public Slice limit(final long limit) {

		if ( limit < 0 ) {
			throw new IllegalArgumentException("illegal limit ["+limit+"]");
		}

		this.limit=limit;

		return this;
	}

	/**
	 * Sets the slice limit.
	 *
	 * @param offset the number of leading cells to be excluded from the processed feed; if equal to {@code 0}, no cell
	 *               is excluded
	 *
	 * @return this slice task
	 */
	public Slice offset(final long offset) {

		if ( offset < 0 ) {
			throw new IllegalArgumentException("illegal offset ["+offset+"]");
		}

		this.offset=offset;

		return this;
	}


	@Override public Stream<_Cell> execute(final Tool.Loader tools, final Stream<_Cell> items) {
		return limit(offset(items));
	}


	private Stream<_Cell> limit(final Stream<_Cell> feed) {
		return limit == 0 ? feed : feed.limit(limit);
	}

	private Stream<_Cell> offset(final Stream<_Cell> feed) {
		return offset == 0 ? feed : feed.skip(offset);
	}

}
