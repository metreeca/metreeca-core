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

package com.metreeca.mill.tasks.db;


import com.metreeca.mill.Task;
import com.metreeca.mill._Cell;
import com.metreeca.form.things.Values;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.IRI;

import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.metreeca.mill._Cell.cell;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.sys.Trace.clip;

import static java.lang.Math.max;
import static java.lang.String.format;


/**
 * SQL extraction task.
 *
 * <p>Executes a SQL select statement on the JDBC sources identified by the focus values in the feed. Focus
 * values not referring to a JDBC URL are skipped with a warning.</p>
 *
 * <p>Each record in the result set is converted to a cell according to the following template:</p>
 *
 * <pre><code>
 *     [] &lt;{@value Values#Internal}<em>field-name</em>&gt; <em>value</em>;
 *          … .
 *  </code></pre>
 *
 * <p>SQL values in the result set are converted to typed RDF terms using the {@link Values#literal(Object)} factory
 * method. Null values are skipped.</p>
 */
public final class SQL implements Task {

	private final Trace trace=tool(Trace.Factory);

	private String query="";


	public SQL query(final String query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		this.query=query;

		return this;
	}


	@Override public Stream<_Cell> execute(final Stream<_Cell> items) { // !!! refactor

		final AtomicLong count=new AtomicLong();
		final AtomicLong elapsed=new AtomicLong();

		return items.flatMap(item -> {

			final String url=iri(item.focus());

			if ( url != null && url.startsWith("jdbc:") ) {

				if ( query.isEmpty() ) { return Stream.empty(); } else {

					try {

						trace.debug(this, format("opening record stream <%s>", clip(item.focus())));

						final Connection connection=DriverManager.getConnection(url); // !!! review
						final Statement statement=connection.createStatement();
						final ResultSet set=statement.executeQuery(query);

						final ResultSetMetaData meta=set.getMetaData();
						final int cols=meta.getColumnCount();

						return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<_Cell>() {

							@Override public boolean hasNext() {
								try {

									return set.next();

								} catch ( final SQLException e ) {

									trace.error(this, format("unable to retrieve data from <%s>", clip(url)), e);

									return false;
								}
							}

							@Override public _Cell next() {

								final long start=System.currentTimeMillis();

								// !!! normalize column names

								final IRI focus=iri();
								final Collection<org.eclipse.rdf4j.model.Statement> model=new ArrayList<>();

								for (int i=1; i <= cols; ++i) {
									try {

										final String name=meta.getColumnName(i); // !!! make sure field name is a legal IRI component
										final Object object=set.getObject(i);

										if ( object != null ) {
											model.add(statement(focus, iri(Values.Internal, name), literal(object)));
										}

									} catch ( final SQLException e ) {

										trace.error(this, format("unable to retrieve data from <%s>", clip(url)), e);

										throw new RuntimeException(e); // !!! report
									}
								}

								final long stop=System.currentTimeMillis();

								count.incrementAndGet();
								elapsed.addAndGet(stop-start);

								return cell(focus, model);
							}

						}, Spliterator.IMMUTABLE|Spliterator.NONNULL|Spliterator.ORDERED), true).onClose(() -> {

							try {

								trace.debug(this, format("closing record stream <%s>", clip(item.focus())));

								connection.close();
								statement.close();
								set.close();

							} catch ( final SQLException e ) {
								trace.error(this, format("unable to close record stream <%s>", clip(item.focus())), e);
							}

						});

					} catch ( final SQLException e ) {

						trace.error(this, format("unable to retrieve data from <%s>", clip(url)), e);

						return Stream.empty();

					}

				}

			} else {

				trace.warning(this, format("skipped non JDBC URL <%s>", clip(url)));

				return Stream.empty();

			}

		}).onClose(() -> {

			final long c=count.get();
			final long e=elapsed.get();

			trace.info(this, format(
					"mapped %,d records in %d ms (%d ms/record)", c, e, c == 0 ? 0 : max(e/c, 1)));

		});
	}

}
