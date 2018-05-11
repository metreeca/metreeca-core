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

package com.metreeca.link.handlers;

import com.metreeca.jeep.Maps;
import com.metreeca.jeep.Sets;
import com.metreeca.link.*;
import com.metreeca.spec.*;
import com.metreeca.spec.codecs.QueryParser;
import com.metreeca.spec.codecs.ShapeCodec;
import com.metreeca.spec.probes.Inferencer;
import com.metreeca.spec.probes.Optimizer;
import com.metreeca.spec.probes.Outliner;
import com.metreeca.spec.shapes.*;
import com.metreeca.spec.shifts.Step;
import com.metreeca.tray.IO;
import com.metreeca.tray.Tool;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Setup;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.link.Handler.unauthorized;
import static com.metreeca.link.Handler.unsupported;
import static com.metreeca.spec.Cell.cell;
import static com.metreeca.spec.Shape.empty;
import static com.metreeca.spec.Values.iri;
import static com.metreeca.spec.Values.rewrite;
import static com.metreeca.spec.Values.statement;
import static com.metreeca.spec.queries.Items.ItemsShape;
import static com.metreeca.spec.queries.Stats.StatsShape;
import static com.metreeca.spec.shapes.All.all;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Or.or;
import static com.metreeca.spec.shapes.Test.test;
import static com.metreeca.spec.shapes.Trait.trait;
import static com.metreeca.spec.shapes.Trait.traits;
import static com.metreeca.spec.shifts.Step.step;

import static java.util.stream.Collectors.toList;


/**
 * Model-driven LDP Basic Container handler.
 *
 * @see "http://www.w3.org/TR/ldp/"
 */
public final class Container implements Handler {

	private static final Step Contains=step(LDP.CONTAINS);

	private static final Shape.Probe<Shape> trimmer=new Shape.Probe<Shape>() { // prune ldp:contains trait // !!! review

		@Override protected Shape fallback(final Shape shape) { return shape; }

		@Override public Shape visit(final Trait trait) { return trait.getStep().equals(Contains) ? and() : trait; }

		@Override public Shape visit(final Virtual virtual) {
			return virtual.getTrait().getStep().equals(Contains) ? and() : virtual;
		}

		@Override public Shape visit(final And and) {
			return and(and.getShapes().stream().map(s -> s.accept(this)).collect(toList()));
		}

		@Override public Shape visit(final Or or) {
			return or(or.getShapes().stream().map(s -> s.accept(this)).collect(toList()));
		}

		@Override public Shape visit(final Test test) {
			return test(test.getTest().accept(this),
					test.getPass().accept(this), test.getFail().accept(this));
		}

	};

	private static final Pattern RepresentationPattern=Pattern
			.compile("\\s*return\\s*=\\s*representation\\s*;\\s*include\\s*=\\s*\"(?<representation>[^\"]*)\"\\s*");


	private final Graph graph;

	private final Shape shape;

	private final Handler dispatcher=new Dispatcher(Maps.map(
			Maps.entry(Request.GET, this::get),
			Maps.entry(Request.POST, Handler.sysadm(this::post)) // !!! remove after testing shape-based authorization
	));


	public Container(final Tool.Loader tools, final Shape shape) {

		if ( tools == null ) {
			throw new NullPointerException("null tools");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		final Setup setup=tools.get(Setup.Tool);

		this.graph=tools.get(Graph.Tool);

		this.shape=shape.accept(new Optimizer()); // merge multiple ldp:contains traits
	}


	@Override public void handle(final Tool.Loader tools,
			final Request request, final Response response, final BiConsumer<Request, Response> sink) {

		dispatcher.handle(tools, request, response, (_request, _response) -> {

			if ( _response.getStatus() == Response.OK ) {
				_response

						.addHeader("Link", "<"+Link.ShapedContainer.stringValue()+">; rel=\"type\"")
						.addHeader("Link", "<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"type\"")
						.addHeader("Link", "<http://www.w3.org/ns/ldp#Container>; rel=\"type\"")
						.addHeader("Link", "<http://www.w3.org/ns/ldp#RDFResource>; rel=\"type\"")
						.addHeader("Link", "<http://www.w3.org/ns/ldp#Resource>; rel=\"type\"")

						.addHeader("Link", String.format("<%s?specs>; rel=\"%s\"", request.getTarget(), LDP.CONSTRAINED_BY));
			}

			sink.accept(_request, _response);

		});

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void get(final Tool.Loader tools,
			final Request request, final Response response, final BiConsumer<Request, Response> sink) {

		final String representation=request.getHeaders("Prefer")
				.stream()
				.map(value -> {

					final Matcher matcher=RepresentationPattern.matcher(value);

					return matcher.matches() ? matcher.group("representation") : "";

				})
				.filter(value -> !value.isEmpty())
				.findFirst()
				.orElse("");

		final IRI target=iri(request.getTarget());
		final String query=request.getQuery();

		final Shape relating=shape.accept(Shape.task(Spec.relate));
		final Shape authorized=relating.accept(Shape.role(request.getRoles()));

		if ( empty(relating) ) {

			unsupported(tools, request, response, sink);

		} else if ( empty(authorized) ) {

			unauthorized(tools, request, response, sink);

		} else if ( query.equals("specs") ) { // !!! review / factor

			// !!! specs query can't be processed as a regular query as it requires visibility on all tasks
			// !!! user redaction must be performed before task redaction (ie reversed wrt regular processing)

			final IRI iri=iri(target+"?"+query);
			final Collection<Statement> model=new ArrayList<>();

			model.add(statement(target, LDP.CONSTRAINED_BY, iri));

			final Shape shape=this.shape
					.accept(Shape.role(request.getRoles()))
					.accept(Shape.mode(Spec.verify))
					.accept(new Inferencer())
					.accept(new Optimizer());

			final ShapeCodec codec=new ShapeCodec();

			final Shape relate=shape.accept(Shape.task(Spec.relate)); // container relation shape

			if ( !empty(relate) ) {
				model.add(statement(iri, Spec.relate, codec.encode(relate, model)));
			}

			final Shape create=traits(shape).getOrDefault(Contains, and()) // extract resource creation shape
					.accept(Shape.task(Spec.create));

			if ( !empty(create) ) {
				model.add(statement(iri, Spec.create, codec.encode(create, model)));
			}

			response.setStatus(Response.OK);

			new Transfer(request, response).model(model, and() /* !!! SpecsShape*/);

			sink.accept(request, response);

		} else if ( representation.equals("http://www.w3.org/ns/ldp#PreferMinimalContainer")
				|| representation.equals("http://www.w3.org/ns/ldp#PreferEmptyContainer") ) {

			// !!! handle multiple uris in include parameter
			// !!! handle omit parameter (with multiple uris)
			// !!! handle other representation values in https://www.w3.org/TR/ldp/#prefer-parameters

			// split container/resource shapes and augment them with system generated properties

			final Shape container=and(all(target), authorized.accept(trimmer));

			response.setStatus(Response.OK);

			new Transfer(request, response).model( // !!! re/factor

					container.accept(Shape.mode(Spec.verify)).accept(new Outliner()), container

			);

			sink.accept(request, response);

		} else {

			// split container/resource shapes and augment them with system generated properties

			final Shape container=and(all(target), authorized.accept(trimmer));
			final Shape resource=traits(authorized).getOrDefault(Contains, and());

			// construct and process configured query, merging constraints from the query string

			final Query filter;

			try {
				filter=new QueryParser(resource).parse(IO.decode(query));
			} catch ( final RuntimeException e ) {
				throw new LinkException(Response.BadRequest, "malformed query: "+e.getMessage(), e);
			}

			// retrieve filtered content from repository

			final Cell cell=request.map(graph).get(filter);

			if ( filter instanceof com.metreeca.spec.queries.Graph ) {
				cell.reverse(LDP.CONTAINS).insert(target);
			}

			// signal successful retrieval of the filtered container

			response.setStatus(Response.OK);

			new Transfer(request, response).model( // !!! re/factor

					query.isEmpty()

							// base container: convert its shape to RDF and merge into results

							? Sets.union(cell.model(), container.accept(Shape.mode(Spec.verify)).accept(new Outliner()))

							// filtered container: return selected data

							: filter instanceof com.metreeca.spec.queries.Graph ? cell.model()

							// introspection query: rewrite query results to the target IRI

							: rewrite(cell.model(), Spec.meta, target),

					// merge all possible shape elements to properly drive response formatting

					or(container, trait(LDP.CONTAINS, resource), StatsShape, ItemsShape)

			);

			sink.accept(request, response);

		}

	}

	private void post(final Tool.Loader tools,
			final Request request, final Response response, final BiConsumer<Request, Response> sink) {

		final Graph graph=request.map(this.graph);

		final Shape cCreating=shape.accept(Shape.task(Spec.create));
		final Shape cAuthorized=cCreating.accept(Shape.role(request.getRoles()));

		final Shape creating=traits(cCreating).getOrDefault(Contains, and());
		final Shape authorized=traits(cAuthorized).getOrDefault(Contains, and());

		if ( empty(creating) ) {

			unsupported(tools, request, response, sink); // !!! doesn't play nice with OPTIONS: forbidden?

		} else if ( empty(authorized) || !graph.isTransactional() && !request.isSysAdm() ) {

			unauthorized(tools, request, response, sink);

		} else {

			final IRI target=iri(request.getTarget());
			final Shape resource=and(all(target), authorized);

			final Collection<Statement> model=new ArrayList<>();

			model.addAll(new Transfer(request, response).model(resource)); // add user-submitted statements (use target with trailing /)
			model.addAll(resource.accept(Shape.mode(Spec.verify)).accept(new Outliner())); // add implied statements

			// !!! move static operations outside transaction (how to generate short incremental ids?)

			final IRI iri=graph.update(connection -> {

				// assign an IRI to the resource to be created

				// !!! custom iri stem/pattern
				// !!! client naming hints (http://www.w3.org/TR/ldp/ §5.2.3.10 -> https://tools.ietf.org/html/rfc5023#section-9.7)
				// !!! normalize slug (https://tools.ietf.org/html/rfc5023#section-9.7)
				// !!! support UUID hint
				// !!! 409 Conflict https://tools.ietf.org/html/rfc7231#section-6.5.8 for clashing slug?

				final String stem=request.getTarget();

				final IRI id=id(connection, stem+(stem.endsWith("/") ? "" : "/"));

				// rewrite statements to the assigned IRI

				final Collection<Statement> cell=rewrite(model, target, id);

				// upload and validate submitted statements

				final Report report=graph.set(authorized, cell(cell).insert(id));

				if ( report.assess(Issue.Level.Error) ) { // shape violations

					// !!! rewrite report value references to original target iri

					throw new LinkException(Response.UnprocessableEntity, report // !!! convert to status code outside update txn
							.prune(Issue.Level.Warning).map(Report::toString).orElse("") // prune for readability
					);

				} else { // valid data

					return id;

				}
			});

			// signal successful creation of the new resource

			sink.accept(request, response
					.setStatus(Response.Created)
					.addHeader("Location", iri.stringValue()));
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final Random random=new Random();


	private static IRI id(final RepositoryConnection connection, final String stem) {
		for (int range=128; true; range=range < Integer.MAX_VALUE/2 ? range*2 : range) {

			final IRI iri=iri(stem+random.nextInt(range));

			if ( !connection.hasStatement(iri, null, null, true)
					&& !connection.hasStatement(null, null, iri, true) ) {
				return iri;
			}

		}
	}

	private static IRI migrate(final RepositoryConnection connection, final IRI iri) {

		final Collection<IRI> aliases=new ArrayList<>();

		final IRI aliasing=OWL.SAMEAS; // !!! review

		try (final RepositoryResult<Statement> statements=connection.getStatements(null, aliasing, iri)) {
			while ( statements.hasNext() ) {

				final Value subject=statements.next().getSubject();

				if ( subject instanceof IRI ) { aliases.add((IRI)subject); }

			}
		} finally {
			connection.remove((Resource)null, aliasing, iri);
		}

		final IRI alias=aliases.size() == 1 ? aliases.iterator().next() : iri;

		if ( !alias.equals(iri) ) {

			final Collection<Statement> statements=new ArrayList<>();

			try (final RepositoryResult<Statement> direct=connection.getStatements(iri, null, null)) {
				while ( direct.hasNext() ) { statements.add(direct.next()); }
			}

			try (final RepositoryResult<Statement> inverse=connection.getStatements(null, null, iri)) {
				while ( inverse.hasNext() ) { statements.add(inverse.next()); }
			}

			connection.remove(statements);
			connection.add(rewrite(statements, iri, alias));
		}

		return alias;
	}

}
