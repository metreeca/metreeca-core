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

package com.metreeca.mill.tasks.rdf;


import com.metreeca.mill.Task;
import com.metreeca.mill._Cell;
import com.metreeca.tray.Tool;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;

import java.util.LinkedHashSet;
import java.util.stream.Stream;

import static com.metreeca.mill._Cell.cell;
import static com.metreeca.spec.things.Values.bnode;
import static com.metreeca.spec.things.Values.statement;

import static java.util.stream.Collectors.toCollection;


/**
 * Graph merging task.
 *
 * <p>Generates a new item with a given {@linkplain #context(Resource)} context} as focus and the union of the models of
 * all items in the feed. All model statements are migrated to the target context.</p>
 */
public final class Graph implements Task {

	private Resource context;


	/**
	 * Configures the target context.
	 *
	 * <p>Defaults to {@code null}, interpreted as a unique blank node generated anew for each execution of the
	 * task.</p>
	 *
	 * @param context the (possibly null) target context
	 *
	 * @return this task
	 */
	public Graph context(final Resource context) {

		this.context=context;

		return this;
	}


	@Override public Stream<_Cell> execute(final Tool.Loader tools, final Stream<_Cell> items) {

		final Resource focus=(context != null) ? context : bnode();

		try (final Stream<Statement> stream=items.flatMap(item -> item.model().stream()).map(statement ->
				statement(statement.getSubject(), statement.getPredicate(), statement.getObject(), context))
		) {
			return Stream.of(cell(focus, stream.collect(toCollection(LinkedHashSet::new))));
		}
	}

}
