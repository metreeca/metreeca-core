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

package com.metreeca.next;

import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.IRI;

import java.util.Optional;
import java.util.function.Consumer;


/**
 * HTTP response.
 *
 * <p>Handles state/behaviour for HTTP responses.</p>
 */
public final class Response extends Outbound<Response> implements Lazy<Response> {

	public static final int OK=200; // https://tools.ietf.org/html/rfc7231#section-6.3.1
	public static final int Created=201; // https://tools.ietf.org/html/rfc7231#section-6.3.2
	public static final int Accepted=202; // https://tools.ietf.org/html/rfc7231#section-6.3.3
	public static final int NoContent=204; // https://tools.ietf.org/html/rfc7231#section-6.3.5

	public static final int MovedPermanently=301; // https://tools.ietf.org/html/rfc7231#section-6.4.2
	public static final int SeeOther=303; // https://tools.ietf.org/html/rfc7231#section-6.4.4

	public static final int BadRequest=400; // https://tools.ietf.org/html/rfc7231#section-6.5.1
	public static final int Unauthorized=401; // https://tools.ietf.org/html/rfc7235#section-3.1
	public static final int Forbidden=403; // https://tools.ietf.org/html/rfc7231#section-6.5.3
	public static final int NotFound=404; // https://tools.ietf.org/html/rfc7231#section-6.5.4
	public static final int MethodNotAllowed=405; // https://tools.ietf.org/html/rfc7231#section-6.5.5
	public static final int Conflict=409; // https://tools.ietf.org/html/rfc7231#section-6.5.8
	public static final int UnprocessableEntity=422; // https://tools.ietf.org/html/rfc4918#section-11.2

	public static final int InternalServerError=500; // https://tools.ietf.org/html/rfc7231#section-6.6.1
	public static final int NotImplemented=501; // https://tools.ietf.org/html/rfc7231#section-6.6.2
	public static final int BadGateway=502; // https://tools.ietf.org/html/rfc7231#section-6.6.3
	public static final int ServiceUnavailable=503; // https://tools.ietf.org/html/rfc7231#section-6.6.4
	public static final int GatewayTimeout=504; // https://tools.ietf.org/html/rfc7231#section-6.6.5


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Request request; // the originating request

	private int status; // the HTTP status code
	private Throwable cause; // a (possibly null) optional cause for an error status code


	/**
	 * Creates a new response for a request.
	 *
	 * @param request the originating request for the new response
	 *
	 * @throws NullPointerException if {@code request} is {@code null}
	 */
	public Response(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		this.request=request;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override protected Response self() {
		return this;
	}

	@Override public void accept(final Consumer<Response> consumer) {

		if ( consumer == null ) {
			throw new NullPointerException("null consumer");
		}

		consumer.accept(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the originating request for this response.
	 *
	 * @return the originating request for this response
	 */
	public Request request() {
		return request;
	}


	/**
	 * Checks if this response is successful.
	 *
	 * @return {@code true} if the {@linkplain #status() status} code is in the {@code 2XX} range; {@code false} otherwise
	 */
	public boolean success() {
		return status/100 == 2;
	}

	/**
	 * Checks if this response is an error.
	 *
	 * @return {@code true} if the {@linkplain #status() status} code is in beyond the {@code 3XX} range; {@code false}
	 * otherwise
	 */
	public boolean error() {
		return status/100 > 3;
	}


	/**
	 * Retrieves the focus item IRI of this response.
	 *
	 * @return the absolute IRI included in the {@code Location} header of this response, if defined; the {@linkplain
	 * Request#item() focus item} IRI of the originating request otherwise
	 */
	public IRI item() {
		return header("location")
				.map(Values::iri)
				.orElseGet(() -> request().item());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the status code of this response.
	 *
	 * @return the status code of this response
	 */
	public int status() {
		return status;
	}

	/**
	 * Configures the status code of this response.
	 *
	 * @param status the status code of this response
	 *
	 * @return this response
	 *
	 * @throws IllegalArgumentException if {@code response } is less than 0 or greater than 599
	 */
	public Response status(final int status) {

		if ( status < 100 || status > 599 ) {
			throw new IllegalArgumentException("illegal status code ["+status+"]");
		}

		this.status=status;

		return this;
	}


	/**
	 * Retrieves the cause for the status code.
	 *
	 * @return a optional throwable causing the selection of the status code
	 */
	public Optional<Throwable> cause() { return Optional.ofNullable(cause); }

	/**
	 * Configures the cause for the status code.
	 *
	 * @param cause the throwable causing the selection of the status code
	 *
	 * @return this response
	 *
	 * @throws NullPointerException if {@code cause} is {@code null}
	 */
	public Response cause(final Throwable cause) {

		if ( cause == null ) {
			throw new NullPointerException("null cause");
		}

		this.cause=cause;

		return this;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	//public Model rdf() {
	//	return rdf((Shape)null);
	//}
	//
	//public Model rdf(final Shape shape) {
	//	return rdf(shape, item());
	//}
	//
	//public Model rdf(final Shape shape, final Resource focus) {
	//
	//	final String content=header("Content-Type").orElse("");
	//
	//	final RDFParserFactory factory=Formats.service(RDFParserRegistry.getInstance(), RDFFormat.TURTLE, content);
	//	final RDFParser parser=factory.getParser();
	//
	//	parser.set(JSONAdapter.Shape, shape);
	//	parser.set(JSONAdapter.Focus, focus);
	//
	//	final Model model=new TreeModel();
	//
	//	parser.setRDFHandler(new AbstractRDFHandler() {
	//		@Override public void handleStatement(final Statement statement) { model.add(statement); }
	//	});
	//
	//	try {
	//
	//		parser.parse(new StringReader(text()), focus.stringValue()); // use reader to activate IRI rewriting
	//
	//	} catch ( final IOException e ) {
	//		throw new UncheckedIOException(e);
	//	}
	//
	//	return model;
	//}
	//
	//
	//public Response rdf(final Iterable<Statement> model) {
	//	return rdf(model, null);
	//}
	//
	//public Response rdf(final Iterable<Statement> model, final Shape shape) {
	//	return rdf(model, shape, request.item());
	//}
	//
	//public Response rdf(final Iterable<Statement> model, final Shape shape, final Resource focus) {
	//
	//	if ( model == null ) {
	//		throw new NullPointerException("null model");
	//	}
	//
	//	final List<String> types=Formats.types(request.headers("Accept"));
	//
	//	final RDFWriterRegistry registry=RDFWriterRegistry.getInstance();
	//	final RDFWriterFactory factory=Formats.service(registry, RDFFormat.TURTLE, types);
	//
	//	// try to set content type to the actual type requested even if it's not the default one
	//
	//	return header("Content-Type", types.stream().filter(type -> registry.getFileFormatForMIMEType(type).isPresent()).findFirst().orElseGet(() -> factory.getRDFFormat().getDefaultMIMEType()))
	//
	//			.writer(writer -> {
	//				try (final Writer w=writer) {
	//
	//					final RDFWriter rdf=factory.getWriter(w);
	//
	//					rdf.set(JSONAdapter.Shape, shape);
	//					rdf.set(JSONAdapter.Focus, focus);
	//
	//					Rio.write(model, rdf);
	//
	//				} catch ( final IOException e ) {
	//					throw new UncheckedIOException(e);
	//				}
	//			});
	//}

}
