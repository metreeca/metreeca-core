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

package com.metreeca.mill.tasks.xml;


import com.metreeca.mill.Task;
import com.metreeca.mill._Cell;
import com.metreeca.spec.things.Values;
import com.metreeca.tray.Tool;
import com.metreeca.tray.sys.Trace;
import com.metreeca.tray.sys._Cache;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.query.QueryResult;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AnyURIValue;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.rio.helpers.XMLParserSettings;
import org.xml.sax.*;
import org.xml.sax.helpers.XMLFilterImpl;

import java.io.*;
import java.util.Collection;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import static com.metreeca.mill._Cell.cell;
import static com.metreeca.spec.things.Values.iri;
import static com.metreeca.tray.sys.Trace.clip;


/**
 * XML abstract processing tasks.
 *
 * <p>For each feed cell focused on a IRI:</p>
 *
 * <ul>
 *
 * <li>retrieves the content of the IRI from the {@linkplain _Cache#Tool network cache};</li>
 *
 * <li>parses the retrieved content as XML, possibly using a user-supplied {@linkplain #parser(Supplier) parser};</li>
 *
 * <li>executes an XML {@linkplain #transform(String) transform} using the parsed XML as context node and a {@linkplain
 * #processor(Tool.Loader, String)} processor} provided by the concrete implementation;</li>
 *
 * <li>for each item in the sequence returned by the transform, generates a cell according to the item type.</li>
 *
 * </ul>
 *
 * <p>Feed cells focused on blank nodes or literals are skipped with a warning.</p>
 */
public abstract class XML<T extends XML<T>> implements Task {

	/* !!! document
	* XML node -> parse (leniently) as RDF
	*
	* map -> ??? xsd:anyURL
	*
	* value -> ???
	*
	* otherwise -> ???
	*/

	public static final Supplier<XMLReader> HTML=org.ccil.cowan.tagsoup.Parser::new;


	private String transform;
	private Supplier<XMLReader> parser; // thread-safeness >> generate a new parser per evaluation


	protected abstract T self();

	protected abstract Function<Source, Stream<? extends XdmValue>> processor(
			Tool.Loader tool, final String transform
	);


	public T transform(final String transform) {

		if ( transform == null ) {
			throw new NullPointerException("null transform");
		}

		this.transform=transform;

		return self();
	}

	public T parser(final Supplier<XMLReader> parser) {

		this.parser=parser;

		return self();
	}


	@Override public Stream<_Cell> execute(final Tool.Loader tools, final Stream<_Cell> items) {

		final _Cache cache=tools.get(_Cache.Tool);
		final Trace trace=tools.get(Trace.Tool);

		final Function<Source, Stream<? extends XdmValue>> processor
				=processor(tools, transform != null ? transform : "");

		return items.flatMap(cell -> {

			final Value focus=cell.focus();
			final String url=iri(focus);

			if ( url != null ) {

				try {

					return processor.apply(source(cache.get(url)))
							.map(XdmValue::getUnderlyingValue)
							.map(this::convert);

				} catch ( final IOException|RuntimeException e ) {

					trace.error(this, String.format("unable to transform RDF from <%s>", clip(url)), e);

					return Stream.empty();

				}

			} else {

				trace.warning(this, String.format("skipping focus value <%s>", clip(Values.format(focus))));

				return Stream.empty();

			}

		});
	}


	private Source source(final _Cache.Entry entry) throws IOException {

		final String url=entry.url();
		final Reader reader=entry.reader();

		if ( parser == null ) {

			return new StreamSource(reader, url);

		} else {

			final InputSource source=new InputSource();

			source.setCharacterStream(reader);
			source.setSystemId(url);

			return new SAXSource(parser.get(), source);

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private _Cell convert(final Sequence value) {
		return value instanceof AnyURIValue ? convert((AnyURIValue)value)
				: value instanceof NodeInfo ? convert((NodeInfo)value)
				: convert((Object)value);
	}

	private _Cell convert(final AnyURIValue value) {
		return _Cell.cell(iri(value.getStringValue()));
	}

	private _Cell convert(final NodeInfo value) {

		final XMLReader reader=new XMToRDF(value); // :-O avoid value serialization with a fake XMLReader
		final StatementCollector collector=new StatementCollector();

		try {

			Rio.createParser(RDFFormat.RDFXML)
					.set(XMLParserSettings.CUSTOM_XML_READER, reader)
					.set(XMLParserSettings.PARSE_STANDALONE_DOCUMENTS, true)
					.setRDFHandler(collector)
					.parse(new StringReader(""), Values.Internal);

		} catch ( final IOException unexpected ) {
			throw new UncheckedIOException(unexpected);
		}

		final Collection<Statement> model=collector.getStatements();
		final Resource focus=model.isEmpty() ? iri() : model.iterator().next().getSubject(); // link root node // !!! review

		return cell(focus, model);
	}

	private _Cell convert(final Object value) {
		throw new UnsupportedOperationException("unsupported value type "+value.getClass().getName());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class XMToRDF extends XMLFilterImpl {

		private final NodeInfo value;


		private XMToRDF(final NodeInfo value) {
			this.value=value;
		}


		@Override public void startElement(
				final String uri, final String name, final String qname,
				final Attributes attributes
		) throws SAXException {

			// migrate elements from the default namespace to the work namespace

			super.startElement(uri.isEmpty() ? Values.Internal : uri, name, qname, attributes);
		}


		@Override public void parse(final InputSource input) throws SAXException { parse(input.getSystemId()); }

		@Override public void parse(final String systemId) throws SAXException {
			try {

				// relay serialization SAX events to the content handler ignoring the input source

				QueryResult.serialize(value, new SAXResult(this), new Properties());

			} catch ( final XPathException e ) {
				throw new SAXException(e);
			}
		}

	}

}
