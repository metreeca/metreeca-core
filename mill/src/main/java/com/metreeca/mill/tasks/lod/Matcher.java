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

package com.metreeca.mill.tasks.lod;


import com.metreeca.mill.Task;
import com.metreeca.mill._Cell;
import com.metreeca.mill.tasks.Link;
import com.metreeca.mill.tasks.Pipe;
import com.metreeca.mill.tasks.Slice;
import com.metreeca.spec.things.Values;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.IRI;

import java.util.stream.Stream;

import static com.metreeca.spec.things.Values.iri;
import static com.metreeca.tray.Tray.tool;

import static java.util.stream.Collectors.joining;


/**
 * Abstract linked data matching task.
 */
public abstract class Matcher<T extends Matcher<T>> implements Task {

	private static final int ConnectTimeout=2*1000;
	private static final int ReadTimeout=5*1000;


	private static final IRI Match=iri(Values.Internal, "match");


	private final Trace trace=tool(Trace.Factory);

	private Task preprocessor;
	private IRI link;


	protected abstract T self();

	protected abstract Task service();

	protected abstract Task postprocessor();


	public Task preprocessor(final Task preprocessor) {

		this.preprocessor=preprocessor;

		return self();
	}


	public T link(final String link) {

		this.link=link == null ? null : iri(Values.Internal, link);

		return self();
	}

	public T link(final IRI link) {

		this.link=link;

		return self();
	}


	@Override public Stream<_Cell> execute(final Stream<_Cell> items) {
		return new Link(new Pipe(

				preprocessor != null ? preprocessor : new Slice(),

				service(),

				(_items) -> _items.peek(item -> {

					trace.info(this, item.model().stream()
							.filter(s -> s.getSubject().equals(item.focus()))
							.map(s -> '\t'+s.getPredicate().getLocalName()+": "+s.getObject().stringValue())
							.collect(joining("\n", "matching\n\n", "\n")));

				}),

				// !!! retrieval timeouts
				// !!! parallel retrieval with throttling
				// !!! manage errors/timeouts
				// !!! warn about missing matches

				postprocessor()

		)).link(link != null ? link : Match).execute(items);
	}

}
