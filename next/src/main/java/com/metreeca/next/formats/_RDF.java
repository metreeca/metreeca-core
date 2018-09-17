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

package com.metreeca.next.formats;

import com.metreeca.next.Format;
import com.metreeca.next.Message;

import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.Optional;


/**
 * RDF body format.
 */
@Deprecated public final class _RDF implements Format<Collection<Statement>> {

	/**
	 * The singleton RDF body format.
	 */
	public static final Format<Collection<Statement>> Format=new _RDF();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private _RDF() {} // singleton


	/**
	 * @return the optional RDF body representation of {@code message}, as retrieved from its {@link _Input#Format}
	 * representation, if present; an empty optional, otherwise
	 */
	@Override public Optional<Collection<Statement>> get(final Message<?> message) {

		return Optional.empty(); // !!!

		//return message.body(_Reader.Format).map(supplier -> { // use reader to activate IRI rewriting
		//
		//	final IRI focus=request.item();
		//	final String type=request.header("content-type").orElse("");
		//
		//	final RDFParser parser=Formats
		//			.service(RDFParserRegistry.getInstance(), RDFFormat.TURTLE, type)
		//			.getParser();
		//
		//	parser.set(JSONAdapter.Shape, message.body(_Shape.Format).orElse(null));
		//	parser.set(JSONAdapter.Focus, focus);
		//
		//	parser.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);
		//	parser.set(BasicParserSettings.NORMALIZE_DATATYPE_VALUES, true);
		//
		//	parser.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, true);
		//	parser.set(BasicParserSettings.NORMALIZE_LANGUAGE_TAGS, true);
		//
		//	final ParseErrorCollector errorCollector=new ParseErrorCollector();
		//
		//	parser.setParseErrorListener(errorCollector);
		//
		//	final Collection<Statement> model=new ArrayList<>();
		//
		//	parser.setRDFHandler(new AbstractRDFHandler() {
		//		@Override public void handleStatement(final Statement statement) { model.add(statement); }
		//	});
		//
		//	try (final Reader reader=supplier.get()) {
		//
		//		parser.parse(reader, focus.stringValue()); // resolve relative IRIs wrt the focus
		//
		//	} catch ( final RDFParseException e ) {
		//
		//		if ( errorCollector.getFatalErrors().isEmpty() ) { // exception possibly not reported by parser…
		//			errorCollector.fatalError(e.getMessage(), e.getLineNumber(), e.getColumnNumber());
		//		}
		//
		//	} catch ( final IOException e ) {
		//
		//		throw new UncheckedIOException(e);
		//
		//	}
		//
		//	// !!! log warnings/error/fatals
		//
		//	final List<String> fatals=errorCollector.getFatalErrors();
		//	final List<String> errors=errorCollector.getErrors();
		//	final List<String> warnings=errorCollector.getWarnings();
		//
		//	if ( fatals.isEmpty() ) {
		//
		//		model.addAll(shape.accept(mode(Form.verify)).accept(new Outliner(focus))); // shape-implied statements
		//
		//		// !!! associate model to message
		//
		//	} else { // !!! structured json error report
		//
		//		// !!! report error
		//
		//		//return new Result<Request, JsonObject>() {
		//		//	@Override public <R> R apply(final Function<Request, R> value, final Function<JsonObject, R> error) {
		//		//		return error.apply(error("data-malformed", new RDFParseException("errors parsing content as "
		//		//				+parser.getRDFFormat().getDefaultMIMEType()+":\n\n"
		//		//				+String.join("\n", fatals)
		//		//				+String.join("\n", errors)
		//		//				+String.join("\n", warnings))));
		//		//	}
		//		//};
		//
		//	}
		//})
	}

	/**
	 * Configures the {@link _Output#Format} representation of {@code message} to write the RDF {@code value} to the
	 * accepted output stream.
	 */
	@Override public void set(final Message<?> message, final Collection<Statement> value) {
		//message.body(_Writer.Format, writer -> { // use writer to activate IRI rewriting
		//
		//	final List<String> types=Formats.types(response.request().headers("Accept"));
		//
		//	final RDFWriterRegistry registry=RDFWriterRegistry.getInstance();
		//	final RDFWriterFactory factory=Formats.service(registry, RDFFormat.TURTLE, types);
		//
		//	// try to set content type to the actual type requested even if it's not the default one
		//
		//	response.header("Content-Type", types.stream()
		//			.filter(type -> registry.getFileFormatForMIMEType(type).isPresent())
		//			.findFirst()
		//			.orElseGet(() -> factory.getRDFFormat().getDefaultMIMEType()))
		//
		//	final RDFWriter rdf=factory.getWriter(writer);
		//
		//	rdf.set(JSONAdapter.Shape, message.body(_Shape.Format).orElse(null));
		//	rdf.set(JSONAdapter.Focus, response.item());
		//
		//	Rio.write(value, rdf);
		//
		//});
	}

}
