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

package com.metreeca.mill.tasks;

import com.metreeca.mill.Task;
import com.metreeca.mill._Cell;
import com.metreeca.tray.Tool;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * Feed sampling task.
 *
 * <p>Extracts from the feed a sample including a {@link #fraction(double)} of the supplied items.</p>
 */
public final class Sample implements Task {

	private double fraction;


	public Sample fraction(final double fraction) {

		if ( fraction < 0 || fraction > 1 ) {
			throw new IllegalArgumentException("illegal fraction ["+fraction+"]");
		}

		this.fraction=fraction;

		return this;
	}


	@Override public Stream<_Cell> execute(final Tool.Loader tools, final Stream<_Cell> items) {
		return StreamSupport.stream(Spliterators.spliterator(new Iterator<_Cell>() {

			private final Iterator<_Cell> iterator=items.iterator();

			private _Cell next;

			private long feed;
			private long sink;


			@Override public boolean hasNext() {
				if ( next != null ) {

					return true;

				} else {

					while ( iterator.hasNext() ) {

						next=iterator.next();

						if ( ++feed*fraction >= sink ) {
							try { return true; } finally { ++sink; }
						}
					}

					return false;
				}
			}

			@Override public _Cell next() {

				if ( !hasNext() ) {
					throw new NoSuchElementException("no more cells");
				}

				try { return next; } finally { next=null; }

			}

		}, 0, 0), false);
	}
}
