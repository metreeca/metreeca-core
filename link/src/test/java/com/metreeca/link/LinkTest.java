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

import com.metreeca.spec.Shape;
import com.metreeca.spec.Spec;
import com.metreeca.spec.things.Values;
import com.metreeca.spec.things.ValuesTest;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.io.*;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static com.metreeca.spec.Shape.*;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Clazz.clazz;
import static com.metreeca.spec.shapes.Datatype.datatype;
import static com.metreeca.spec.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.spec.shapes.MaxLength.maxLength;
import static com.metreeca.spec.shapes.MinInclusive.minInclusive;
import static com.metreeca.spec.shapes.Or.or;
import static com.metreeca.spec.shapes.Pattern.pattern;
import static com.metreeca.spec.shapes.Test.test;
import static com.metreeca.spec.shapes.Trait.trait;
import static com.metreeca.spec.shapes.When.when;
import static com.metreeca.spec.things.Strings.indent;
import static com.metreeca.spec.things.Values.integer;
import static com.metreeca.spec.things.Values.literal;
import static com.metreeca.spec.things.ValuesTest.term;
import static com.metreeca.tray.Tray.tool;

import static org.junit.Assert.fail;


public final class LinkTest {

	//// !!! factor to request ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//private Supplier<Map<String, List<String>>> parameters=() -> parameters(method.equals(POST)
	//		&& getHeader("Content-Type").orElse("").startsWith(URLEncodedForm) // ignore charset parameter
	//		? getText() : query);


	private static Map<String, List<String>> parameters(final String query) {

		final Map<String, List<String>> parameters=new LinkedHashMap<>();

		final int length=query.length();

		for (int head=0, tail; head < length; head=tail+1) {
			try {

				final int equal=query.indexOf('=', head);
				final int ampersand=query.indexOf('&', head);

				tail=(ampersand >= 0) ? ampersand : length;

				final boolean split=equal >= 0 && equal < tail;

				final String label=URLDecoder.decode(query.substring(head, split ? equal : tail), "UTF-8");
				final String value=URLDecoder.decode(query.substring(split ? equal+1 : tail, tail), "UTF-8");

				parameters.compute(label, (name, values) -> {

					final List<String> strings=(values != null) ? values : new ArrayList<>();

					strings.add(value);

					return strings;

				});

			} catch ( final UnsupportedEncodingException unexpected ) {
				throw new UncheckedIOException(unexpected);
			}
		}

		return Collections.unmodifiableMap(parameters);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final IRI Manager=term("roles/manager");
	public static final IRI Salesman=term("roles/salesman");

	public static final Shape Employee=test(

			or(

					and(when(Spec.role, Manager)),
					and(when(Spec.role, Salesman), when(Spec.task, Spec.create, Spec.relate, Spec.update))
			),

			and(
					clazz(term("Employee")), // implies ?this a :Employee
					verify(
							server(
									trait(RDF.TYPE, and(required(), datatype(Values.IRIType))),
									trait(RDFS.LABEL, and(required(), datatype(XMLSchema.STRING))),
									trait(term("code"), and(required(), datatype(XMLSchema.STRING), pattern("\\d+")))
							),
							and(
									trait(term("forename"), and(required(), datatype(XMLSchema.STRING), maxLength(80))),
									trait(term("surname"), and(required(), datatype(XMLSchema.STRING), maxLength(80))),
									trait(term("email"), and(required(), datatype(XMLSchema.STRING), maxLength(80))),
									trait(term("title"), and(required(), datatype(XMLSchema.STRING), maxLength(80)))
							),
							test(when(Spec.role, Manager), and(

									trait(term("seniority"), and(required(), datatype(XMLSchema.INTEGER),
											minInclusive(literal(integer(1))), maxInclusive(literal(integer(5))))),

									trait(term("supervisor"), and(optional(), datatype(Values.IRIType), clazz(term("User")))),
									trait(term("subordinate"), and(optional(), datatype(Values.IRIType), clazz(term("User"))))

							))
					)
			)
	);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Testbed testbed() { return new Testbed(); }


	public static String json(final String json) {
		return json.replace('\'', '"');
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private LinkTest() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class Testbed {

		private final Tray tray=new Tray();

		private Supplier<Handler> handler=() -> tool(Server.Factory);

		private Consumer<Request.Writer> request;


		public Testbed toolkit(final Toolkit toolkit) {

			if ( toolkit == null ) {
				throw new NullPointerException("null toolkit");
			}

			tray.update(toolkit::load);

			return this;
		}

		public Testbed service(final Supplier<Service> service) {

			if ( service == null ) {
				throw new NullPointerException("null service");
			}

			tray.lookup(() -> service.get().load());

			return this;
		}

		public Testbed dataset(final Iterable<Statement> model, final Resource... contexts) {

			if ( model == null ) {
				throw new NullPointerException("null model");
			}

			if ( contexts == null ) {
				throw new NullPointerException("null contexts");
			}

			tray.lookup(() -> {
				try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
					connection.add(model, contexts);
				}
			});

			return this;
		}


		public Testbed handler(final Supplier<Handler> handler) {

			if ( handler == null ) {
				throw new NullPointerException("null handler");
			}

			this.handler=new Supplier<Handler>() {

				private Handler cache;

				@Override public Handler get() {
					return cache != null ? cache : (cache=handler.get());
				}

			};

			return this;
		}


		public Testbed request(final Consumer<Request.Writer> request) {

			if ( request == null ) {
				throw new NullPointerException("null request");
			}

			try {

				return (this.request == null) ? this : exec(this.request, reader -> {});

			} finally {

				this.request=request;
			}

		}

		public Testbed response(final Consumer<Response.Reader> response) {

			if ( response == null ) {
				throw new NullPointerException("null response");
			}

			if ( request == null ) {
				throw new IllegalStateException("no pending request");
			}

			try {

				return exec(request, response);

			} finally {

				this.request=null;

			}
		}


		public Testbed exec(final Consumer<Request.Writer> request, final Consumer<Response.Reader> response) {

			final AtomicBoolean invoked=new AtomicBoolean();

			exec(() -> handler.get()

					.wrap(parser()) // parse query string
					.wrap(persistor()) // make response body getters idempotent

					.handle(

							writer ->

									request.accept(writer.base(ValuesTest.Base)),

							reader -> {

								invoked.set(true);

								if ( reader.binary() ) {

									final ByteArrayOutputStream buffer=new ByteArrayOutputStream();

									reader.output(buffer);

									if ( !reader.success() ) {
										Logger.getGlobal().severe("status code "+reader.status()
												+"\n\n"+buffer.toByteArray().length+" bytes");
									}

								} else if ( reader.textual() ) {

									final StringWriter buffer=new StringWriter();

									reader.writer(buffer);

									if ( !reader.success() ) {
										Logger.getGlobal().severe("status code "+reader.status()
												+"\n\n"+indent(buffer.toString().trim()));
									}

								}

								try {
									response.accept(reader);
								} catch ( final RuntimeException e ) {
									throw new AssertionError("exception in test code", e);
								}

							}

					));

			if ( !invoked.get() ) {
				fail("no response");
			}

			return this;
		}

		public Testbed exec(final Runnable task) {

			if ( task == null ) {
				throw new NullPointerException("null task");
			}

			tray.lookup(task);

			return this;
		}


		private Wrapper parser() {
			return handler -> (request, response) -> handler.handle(

					writer -> {

						writer.copy(request);

						if ( !request.query().isEmpty() && request.parameters().noneMatch(entry -> true) ) {
							parameters(request.query()).forEach(writer::parameter);
						}

						writer.done();
					},

					reader ->

							response.copy(reader).done()

			);
		}

		private Wrapper persistor() {
			return handler -> (request, response) -> handler.handle(

					writer ->

							writer.copy(request).done(),

					reader -> {

						response.copy(reader);

						if ( reader.binary() ) {
							response.data(reader.data());
						} else if ( reader.textual() ) {
							response.text(reader.text());
						} else {
							response.done();
						}

					}

			);
		}

	}

}
