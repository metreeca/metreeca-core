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
import com.metreeca.mill._Template;
import com.metreeca.spec.things.Values;
import com.metreeca.tray.sys.Trace;
import com.metreeca.tray.sys._Cache;

import org.eclipse.rdf4j.model.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.metreeca.mill._Cell.cell;
import static com.metreeca.spec.things.Values.literal;
import static com.metreeca.spec.things.Values.statement;
import static com.metreeca.tray.Tray.tool;

import static java.util.stream.Collectors.toList;


/**
 * Item injection task.
 *
 * <p>Generates a stream of parametrized cells.</p>
 *
 * <p>For each cell in the feed:</p>
 *
 * <ul>
 *
 * <li>computes item {@linkplain #parameter(String, String, String) parameters} evaluating expressions on the cell
 * model;</li>
 *
 * <li>parametrize {@linkplain #iri(String) IRI} and {@linkplain #text(String) text} on the computed parameters; if
 * {@linkplain #iri(String) IRI} is empty, a new unique IRI is generated;</li>
 *
 * <li>if not empty, uploads the parametrized text to the {@linkplain _Cache#Tool cache}, identifying it with the
 * parametrized IRI;</li>
 *
 * <li>generates a new cell focused on the parametrized IRI; downstream tasks may read the uploaded content from the
 * cache using the focus IRIs of the generated cells.</li>
 *
 * </ul>
 */
public final class Item implements Task {

	// !!! path expressions
	// !!! prefixed steps in paths (wrt namespace catalog, to be provided to constructor)
	// !!! SPARQL/Javascript? expression

	// !!! document parameter expression syntax / semantics

	private static final _Template Empty=new _Template("");

	private final _Cache cache=tool(_Cache.Tool);
	private final Trace trace=tool(Trace.Tool);

	private _Template iri=Empty;
	private _Template text=Empty;

	private final Map<String, _Template> expressions=new LinkedHashMap<>();
	private final Map<String, String> fallbacks=new LinkedHashMap<>();


	/*
	 * @param iri the template for the IRI of injected items
	 */
	public Item iri(final String iri) {

		if ( iri == null ) {
			throw new NullPointerException("null iri");
		}

		this.iri=iri.isEmpty() ? Empty : new _Template(iri);

		return this;
	}

	/*
	 * @param text the template for the text of injected items
	 */
	public Item text(final String text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		this.text=text.isEmpty() ? Empty : new _Template(text);

		return this;
	}


	/**
	 * Defines an item parameter.
	 *
	 * @param name       the name of the item parameter to be defined
	 * @param expression the expression to be evaluated on a cell model to compute the value of the parameter
	 *
	 * @return this task
	 */
	public Item parameter(final String name, final String expression) {
		return parameter(name, expression, "");
	}

	/**
	 * Defines an item parameter with a fallback value.
	 *
	 * @param name       the name of the item parameter to be defined
	 * @param expression the expression to be evaluated on the item model to compute the value of the parameter
	 * @param fallback   the fallback value for the parameter, if the evaluation of {@code expression} yields an empty
	 *                   string
	 *
	 * @return this task
	 */
	public Item parameter(final String name, final String expression, final String fallback) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( expression == null ) {
			throw new NullPointerException("null expression");
		}

		if ( fallback == null ) {
			throw new NullPointerException("null fallback");
		}

		expressions.put(name, new _Template(expression));
		fallbacks.put(name, fallback);

		return this;
	}


	@Override public Stream<_Cell> execute(final Stream<_Cell> items) {
		return items.flatMap(item -> {

			// compute parameter values

			final Map<String, String> values=values(item);

			// parametrize IRI

			final IRI iri=this.iri.equals(Empty) ? Values.iri() : Values.iri(this.iri.fill(values::get));

			// parametrize and upload text, unless empty

			if ( !text.equals(Empty) ) {
				try {

					cache.set(Values.iri(iri), new StringReader(text.fill(values::get)));

				} catch ( final IOException e ) {

					trace.error(this, "unable to upload text to cache", e);

					return Stream.empty();

				}
			}

			// insert non-empty computed parameter values in the item model

			return Stream.of(cell(iri, values.entrySet().stream()
					.filter(e -> !e.getValue().isEmpty())
					.map(e -> statement(iri, Values.iri(Values.Internal, e.getKey()), literal(e.getValue())))
					.collect(toList())));

		});
	}


	private Map<String, String> values(final _Cell item) {

		final Map<String, String> values=new LinkedHashMap<>();

		for (final Map.Entry<String, _Template> entry : expressions.entrySet()) {

			final String name=entry.getKey();
			final _Template expression=entry.getValue();

			final String value=expression.fill(placeholder -> {

				final Resource focus=item.focus();
				final Collection<Statement> model=item.model();

				final IRI link=Values.iri(Values.Internal, placeholder);

				return model.stream()
						.filter(s -> s.getSubject().equals(focus) && s.getPredicate().equals(link))
						.map(Statement::getObject)
						.map(Value::stringValue)
						.collect(Collectors.joining(" "));
			});

			values.put(name, value.isEmpty() ? fallbacks.get(name) : value);
		}

		return values;
	}

}
