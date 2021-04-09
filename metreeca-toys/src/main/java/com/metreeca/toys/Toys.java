/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.toys;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.IOException;
import java.io.UncheckedIOException;

import static com.metreeca.json.Values.iri;
import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.rest.Toolbox.service;
import static com.metreeca.rest.Xtream.task;

public final class Toys implements Runnable {

	public static final String Base="https://example.com/";
	public static final String Namespace=Base+"terms#";

	public static final IRI staff=toys("staff");

	public static final IRI Order=toys("Order");
	public static final IRI Product=toys("Product");
	public static final IRI ProductLine=toys("ProductLine");

	public static final IRI amount=toys("amount");
	public static final IRI buy=toys("buy");
	public static final IRI code=toys("code");
	public static final IRI customer=toys("customer");
	public static final IRI line=toys("line");
	public static final IRI product=toys("product");
	public static final IRI order=toys("order");
	public static final IRI scale=toys("scale");
	public static final IRI sell=toys("sell");
	public static final IRI size=toys("size");
	public static final IRI status=toys("status");
	public static final IRI stock=toys("stock");
	public static final IRI vendor=toys("vendor");


	private static IRI toys(final String name) {
		return iri(Namespace, name);
	}


	@Override
	public void run() {
		service(graph()).update(task(connection -> {
			try {

				connection.add(Toys.class.getResourceAsStream("Toys.ttl"), Base, RDFFormat.TURTLE);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}));
	}

}