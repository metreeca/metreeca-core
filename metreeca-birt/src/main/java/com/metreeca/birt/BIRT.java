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

package com.metreeca.birt;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;

import java.io.IOException;
import java.io.UncheckedIOException;

import static com.metreeca.json.Values.iri;
import static com.metreeca.rdf4j.assets.Graph.graph;
import static com.metreeca.rest.Context.asset;

public final class BIRT implements Runnable {

	public static final String Base="https://example.com/";
	public static final String Namespace=Base+"terms#";

	public static final IRI staff=birt("staff");

	public static final IRI Order=birt("Order");
	public static final IRI Product=birt("Product");
	public static final IRI ProductLine=birt("ProductLine");

	public static final IRI amount=birt("amount");
	public static final IRI buy=birt("buy");
	public static final IRI code=birt("code");
	public static final IRI customer=birt("customer");
	public static final IRI line=birt("line");
	public static final IRI product=birt("product");
	public static final IRI order=birt("order");
	public static final IRI scale=birt("scale");
	public static final IRI sell=birt("sell");
	public static final IRI size=birt("size");
	public static final IRI status=birt("status");
	public static final IRI stock=birt("stock");
	public static final IRI vendor=birt("vendor");


	private static IRI birt(final String name) {
		return iri(Namespace, name);
	}


	@Override
	public void run() {
		asset(graph()).exec(connection -> {
			try {

				connection.add(BIRT.class.getResourceAsStream("com/metreeca/birt/BIRT.ttl"), Base, RDFFormat.TURTLE);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}

}