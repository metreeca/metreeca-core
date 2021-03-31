---
title:      Publishing Model‑Driven REST/JSON-LD APIs
---

[comment]: <> (excerpt:    Hands-on guided tour of model-driven REST/JSON-LD APIs publishing)


This example-driven tutorial introduces the main building blocks of the Metreeca/Link model-driven REST/JSON framework.
Basic familiarity with [linked data](https://www.w3.org/standards/semanticweb/data) concepts
and [REST](https://en.wikipedia.org/wiki/Representational_state_transfer) APIs is required.

In the following sections you will learn how to use the framework to publish linked data resources through REST/JSON-LD
APIs that automatically support CRUD operations, faceted search, data validation and fine‑grained role‑based access
control.

In the tutorial we will work with a linked data version of the [BIRT](http://www.eclipse.org/birt/phoenix/db/) sample
dataset, cross-linked to [GeoNames](http://www.geonames.org/) entities for cities and countries.

The BIRT sample is a typical business database, containing tables such as *offices*, *customers*, *products*, *orders*, *
order lines*, … for *Classic Models*, a fictional world-wide retailer of scale toy models: we will walk through the REST
API development process focusing on the task of publishing
the [Product](https://demo.metreeca.com/self/#endpoint=https://demo.metreeca.com/toys/sparql&collection=https://demo.metreeca.com/toys/terms#Product)
catalog.

You may try out the examples using your favorite API testing tool or working from the command line with toos like `curl`
or `wget`.

A Maven project with the code for the complete sample app is available
on [GitHub](https://github.com/metreeca/link/tree/main/metreeca-toys): [download](https://downgit.github.io/#/home?url=https://github.com/metreeca/link/tree/main/metreeca-toys&fileName=metreeca%E2%A7%B8link%20sample)
it to your workspace, open in your favorite IDE, compile and launch a local instance of the server.

# Getting Started

To get started, set up a Maven Java 1.8 project, importing the BOM module for Metreeca/Link:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">

	<modelVersion>4.0.0</modelVersion>

	<groupId>com.example</groupId>
	<artifactId>sample</artifactId>
	<version>1.0</version>
	<packaging>war</packaging>

	<properties>

		<maven.compiler.target>1.8</maven.compiler.target>
		<maven.compiler.source>1.8</maven.compiler.source>

	</properties>

	<dependencyManagement>

		<dependencies>

			<dependency>
				<groupId>com.metreeca</groupId>
				<artifactId>metreeca-link</artifactId>
				<version>${project.version}</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>

		</dependencies>

	</dependencyManagement>

</project>
```

Then, add the required dependencies for the Metreeca/Link [connectors](../javadocs/) for the target deployment server and
the target graph storage option; in this tutorial we will deploy to a Servlet 3.1 container with an RDF4J Memory store,
so we add:

```xml

<dependencies>

	<dependency>
		<groupId>com.metreeca</groupId>
		<artifactId>metreeca-jee</artifactId>
	</dependency>

	<dependency>
		<groupId>com.metreeca</groupId>
		<artifactId>metreeca-rdf4j</artifactId>
	</dependency>


	<dependency>
		<groupId>org.eclipse.rdf4j</groupId>
		<artifactId>rdf4j-repository-sail</artifactId>
	</dependency>

	<dependency>
		<groupId>org.eclipse.rdf4j</groupId>
		<artifactId>rdf4j-sail-memory</artifactId>
	</dependency>


	<dependency>
		<groupId>javax.servlet</groupId>
		<artifactId>javax.servlet-api</artifactId>
		<version>3.1.0</version>
		<scope>provided</scope>
	</dependency>

</dependencies>
```

Note that the Metreeca/Link BOM module re-exports the BOM module for the target RDF4J version, so we don't need to
specify version numbers explicitly.

Finally, define a minimal server stub like:

```java
@WebFilter(urlPatterns="/*")
public final class Sample extends JEEServer {

	public Sample() {
		delegate(context -> context.get(() ->

				gateway().wrap(request -> request.reply(response ->
						response.status(OK)
				))

		));
	}

}
```

The stub configures the application to handle any resource using a
barebone [handler](../javadocs/?com/metreeca/rest/Handler.html) always replying to
incoming [requests](../javadocs/?com/metreeca/rest/Request.html) with
a [response](../javadocs/?com/metreeca/rest/Response.html) including a `200` HTTP status code. The
standard [Gateway](../javadocs/?com/metreeca/rest/wrappers/Gateway.html) wrapper provides default pre/postprocessing
services and shared error handling.

Compile and deploy the web app to your favorite servlet container and try your first request:

```shell
% curl --include 'http://localhost:8080/'

HTTP/1.1 200
```

The [context](../javadocs/?com/metreeca/rest/Context.html) argument handled to the app loader lambda manages the shared
system-provided assets and can be used to customize them and to run app initialization tasks.
Copy [BIRT.ttl](https://github.com/metreeca/link/tree/main/metreeca-toys/src/main/resources/com/metreeca/birt/BIRT.ttl)
to the `src/main/resources/` directory and extend the stub as follows:

```java
public final class Sample extends JEEServer {

	public Sample() {
		delegate(context -> context

				.set(graph(), () -> new Graph(new SailRepository(new MemoryStore())))

				.exec(() -> asset(graph()).exec(connection -> {
					try {

			  connection.add(
					  Sample.class.getResourceAsStream("Toys.ttl"),
					  "https://example.com/", RDFFormat.TURTLE
						);

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				}))

				.get(() -> gateway()

						.wrap(request -> request.reply(status(OK)))

				)
		);
	}

}
```

Here we are customizing the shared system-wide [graph](../javadocs/?com/metreeca/rdf4j/assets/Graph.html) database as an
ephemeral memory-based RDF4J store, initializing it on demand with the BIRT dataset.

The static [Context.asset()](../javadocs/?com/metreeca/rest/Context.html#asset-java.util.function.Supplier-) locator
method provides access to shared assets.

Complex initialization tasks can be easily factored to a dedicated class:

```java
public final class Toys implements Runnable {

	public static final String Base="https://example.com/";
	public static final String Namespace=Base+"terms#";

	@Override
	public void run() {
		asset(graph()).exec(connection -> {
			if ( !connection.hasStatement(null, null, null, false) ) {
				try {

			connection.setNamespace("demo", Namespace);
			connection.add(getClass().getResourceAsStream("Toys.ttl"), Base, TURTLE);

		} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}
			}
		});
	}

}
```

```java
@WebFilter(urlPatterns="/*")
public final class Sample extends JEEServer {

	public Sample() {
		delegate(context -> context

			.set(graph(), () -> new Graph(new SailRepository(new MemoryStore())))

			.exec(new Toys())

			.get(() -> gateway()

						.wrap(request -> request.reply(status(OK)))

				)
		);
	}

}
```

[Wrappers](../javadocs/?com/metreeca/rest/Wrapper.html) inspect and possibly alter incoming requests and outgoing
responses before they are forwarded to wrapped handlers and returned to wrapping containers.

The preprocessor rebases RDF payloads from the external network-visible server base (`http://localhost:8080/`) to an
internal canonical base (`https://demo.metreeca.com/`), ensuring data portability between development and production
environments and making it possible to load the static RDF dataset during server initialization, while the external and
possibly request-dependent base is not yet known. When external and internal bases match, as in production, rewriting is
effectively disabled avoiding any performance hit.

# Handling Requests

Requests are dispatched to their final handlers through a hierarchy of wrappers and delegating handlers.

```java
@WebFilter(urlPatterns="/*")
public final class Sample extends JEEServer {

	public Sample() {
		delegate(context -> context

			.set(graph(), () -> new Graph(new SailRepository(new MemoryStore())))

			.exec(new Toys())

			.get(() -> gateway()

					.with(preprocessor(request -> request.base(Toys.Base)))

					.wrap(router()

								.path("/products/*", router()

										.path("/", router().get(request -> request.reply(response -> response
												.status(OK)
												.body(rdf(), asset(graph()).exec(connection -> {
							return stream(connection.getStatements(
									null, RDF.TYPE, iri(Toys.Namespace, "Product")
							))
															.map(Statement::getSubject)
															.map(p -> statement(
																	iri(request.item()), LDP.CONTAINS, p)
															)
															.collect(toList());
												}))))
										)

										.path("/{code}", router().get(request -> request.reply(response -> response
												.status(OK)
												.body(rdf(), asset(graph()).exec(connection -> {
													return asList(connection.getStatements(
															iri(request.item()), null, null
													));
												}))))
										)

								)

						)

				)
		);
	}

}
```

```shell
% curl --include 'http://localhost:8080/products/S18_4409'

HTTP/1.1 200 
Content-Type: text/turtle;charset=UTF-8

@base <https://example.com/products/S18_4409> .

<> a </terms#Product>;
  </terms#buy> 43.26;
  </terms#code> "S18_4409";
  </terms#scale> "1:18";
  </terms#sell> 92.03;
  </terms#stock> 6553;
  </terms#vendor> "Exoto Designs";
  <http://www.w3.org/2000/01/rdf-schema#comment> "This 1:18 scale precision die cast replica …";
  <http://www.w3.org/2000/01/rdf-schema#label> "1932 Alfa Romeo 8C2300 Spider Sport"
  ⋮
```

## Request Routing

[Routers](../javadocs/?com/metreeca/rest/handlers/Router.html) dispatch requests on the basis of
the [request path](../javadocs/?com/metreeca/rest/Request.html#path--) and
the [request method](../javadocs/?com/metreeca/rest/Request.html#method--), ignoring leading path segments possibly
already matched by wrapping routers.

Requests are forwarded to a registered handler if their path is matched by an associated pattern defined by a sequence of
steps according to the following rules:

| pattern step | matching path step   | definition                                                   |
| ------------ | -------------------- | ------------------------------------------------------------ |
| `/`          | `/`                  | empty / matches only the empty step                          |
| `/<step>`    | `/<step>`            | literal / matches step verbatim                              |
| `/{}`        | `/<step>`            | wildcard / matches a single step                             |
| `/{<key>}`   | `/<step>`            | placeholder / match a single path step, adding the matched `<key>`/`<step>` entry to request [parameters](../javadocs/?com/metreeca/rest/Request.html#parameters—); the matched `<step>` name is URL-decoded before use |
| `/*`         | `/<step>[/<step>/…]` | prefix / matches one or more trailing steps                  |

Registered path patterns are tested in order of definition.

If the router doesn't contain a matching handler, no action is performed giving the container adapter a fall-back
opportunity to handle the request.

## Combo Handlers

Again, complex handlers can be easily factored to dedicated classes:

```java
@WebFilter(urlPatterns="/*")
public final class Sample extends JEEServer {

	public Sample() {
		delegate(context -> context

			.set(graph(), () -> new Graph(new SailRepository(new MemoryStore())))

			.exec(new Toys())

			.get(() -> gateway()

					.with(preprocessor(request -> request.base(Toys.Base)))

					.wrap(router()

								.path("/products/*", new Products())

						)

				)
		);
	}

}
```

```java
public final class Products extends Delegator {

	public static final IRI Product=iri(Toys.Namespace, "Product");

	public Products() {
		delegate(router()

				.path("/", router()
						.get(request -> request.reply(response -> response
								.status(OK)
								.body(rdf(), asset(graph()).exec(connection -> {
									return stream(connection.getStatements(
											null, RDF.TYPE, Product
									))
											.map(Statement::getSubject)
											.map(product -> statement(
													iri(request.item()), LDP.CONTAINS, product
											))
											.collect(toList());
								}))
						))
				)

				.path("/{code}", router()
						.get(request -> request.reply(response -> response
								.status(OK)
								.body(rdf(), asset(graph()).exec(connection -> {
									return asList(connection.getStatements(
											iri(request.item()), null, null
									));
								}))
						))
				)

		);
	}

}
```

The [Delegator](../javadocs/?com/metreeca/rest/handlers/Delegator.html) abstract handler provides a convenient way of
packaging complex handlers assembled as a combination of other handlers and wrappers.

# Model-Driven Handlers

Standard resource action handlers can be defined using high-level declarative models that drive automatic fine‑grained
role‑based read/write access control, faceted search, incoming data validation and bidirectional conversion between RDF
and idiomatic [compacted/framed](../references/jsonld -format) JSON-LD payloads, as demonstrated in
the [REST APIs interaction tutorial](consuming-jsonld-apis.md).

Actors provide default shape-driven implementations for CRUD actions on resources and containers identified by the
request [focus item](../javadocs/?com/metreeca/rest/Request.html#item--).

| actor                                                        | action                                                       |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [Relator](../javadocs/?com/metreeca/rest/handlers/Relator.html) | resource retrieval / retrieves the detailed RDF description of the target resource |
| [Browser](../javadocs/?com/metreeca/rest/handlers/Browser.html) | container browsing / retrieves the digest RDF description of the contained resources; supports extended [faceted search](consuming-jsonld-apis.md#faceted-search), sorting and pagination |
| [Creator](../javadocs/?com/metreeca/rest/handlers/Creator.html) | container resource creation / uploads the detailed RDF description of a new resource to be inserted into the target container |
| [Updater](../javadocs/?com/metreeca/rest/handlers/Updater.html) | resource updating / updates the detailed RDF description of the target resource |
| [Deleter](../javadocs/?com/metreeca/rest/handlers/Deleter.html) | resource deletion / deletes the detailed RDF description of the target resource |

```diff
@WebFilter(urlPatterns="/*")
public final class Sample extends JEEServer {

	public Sample() {
		delegate(context -> context

				.set(graph(), () -> new Graph(new SailRepository(new MemoryStore())))
+				.set(engine(), GraphEngine::new)

				.exec(new Toys())

				.get(() -> gateway()

						.with(preprocessor(request -> request.base(Toys.Base)))

						.wrap(router()

								.path("/products/*", new Products())

						)

				)
		);
	}

}
```

Actors delegate transaction management, data validation and trimming and CRUD operations to a customizable engine.

CRUD perations are performed on the graph neighbourhood of the target target item(s)  matched by
the  [shape](../javadocs/?com/metreeca/form/Shape.html)
model [associated](../javadocs/?com/metreeca/rest/Message.html#shape--) to the request, after redaction according to the
request user roles and to actor-specific task, area and mode parameters.

## Defining Models

Let's start by defining a barebone model stating that all resources of class `Product` are to be published as container
items exposing only `rdf:type`, `rdfs:label`  and `rdfs:comment` properties.

```java
public final class Products extends Delegator {

	public Products() {
		delegate(driver(

				field(RDF.TYPE),
				field(RDFS.LABEL),
				field(RDFS.COMMENT)

		).wrap(router()

				.path("/", router()
						.get(browser())
						.post(creator())
				)

				.path("/*", router()
						.get(relator())
						.put(updater())
						.delete(deleter())
				)

		));
	}

}
```

The [Driver](../javadocs/index.html?com/metreeca/rest/wrappers/Driver.html) wrapper associated a linked data model to
incoming requests, driving the operations of nested actors and other model-aware handlers.

Linked data models are defined with a shape-based [specification language](../references/spec-language.md), assembling
shape [building blocks](../references/spec-language.md#shapes) using a simple Java DSL.

As soon as the server is redeployed, the updated REST API exposes only the data specified in the driving model.

```shell
% curl --include 'http://localhost:8080/products/S18_4409'

HTTP/1.1 200 
Content-Type: application/json;charset=UTF-8

{
    "@id": "/products/S18_4409",
    "type": [
        "/terms#Product"
    ],
    "label": [
        "1932 Alfa Romeo 8C2300 Spider Sport"
    ],
    "comment": [
        "This 1:18 scale precision die cast replica features the 6 front headlights…"
    ]
}
```

We'll now refine the initial barebone model, exposing more properties and detailing properties roles and constraints.

```java
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
		asset(graph()).exec(connection -> {
			try {

				connection.add(Toys.class.getResourceAsStream("Toys.ttl"), Base, RDFFormat.TURTLE);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}

}
```

```java
public final class Products extends Delegator {

	public Products() {
		delegate(driver(or(relate(), role(Toys.staff)).then(

				filter(clazz(Toys.Product)),

				field(RDF.TYPE, exactly(Toys.Product)),

				field(RDFS.LABEL, required(), datatype(XSD.STRING), maxLength(50)),
				field(RDFS.COMMENT, required(), datatype(XSD.STRING), maxLength(500)),

				server(field(Toys.code, required())),

				field(Toys.line, required(), convey(clazz(Toys.ProductLine)),

						relate(field(RDFS.LABEL, required()))

				),

				field(Toys.scale, required(),
						datatype(XSD.STRING),
						pattern("1:[1-9][0-9]{1,2}")
				),

				field(Toys.vendor, required(),
						datatype(XSD.STRING),
						maxLength(50)
				),

				field("price", Toys.sell, required(),
						datatype(XSD.DECIMAL),
						minExclusive(literal(decimal(0))),
						maxExclusive(literal(decimal(1000)))
				),

				role(Toys.staff).then(field(Toys.buy, required(),
						datatype(XSD.DECIMAL),
						minInclusive(literal(decimal(0))),
						maxInclusive(literal(decimal(1000)))
				)),

				server().then(field(Toys.stock, required(),
						datatype(XSD.INTEGER),
						minInclusive(literal(integer(0))),
						maxExclusive(literal(integer(10_000)))
				))

		)).wrap(router()

				.path("/", router()
						.get(browser())
						.post(creator())
				)

				.path("/*", router()
						.get(relator())
						.put(updater())
						.delete(deleter())
				)

		));
	}

}
```

The `filter` section states that this model describes a container whose member are the instances of the `toys:Product`
class.

The extended resource model makes use of a number of additional blocks to precisely define the expected shape of the RDF
description of the member resources, for instance including `required` and `datatype` and `pattern` constraints to state
that `rdfs:label`, `rdfs:comment`, `toys:code`, `toys:scale` and `toys:vendor` values are expected:

- to occur exactly once for each resource;
- to be RDF literals of `xsd:string` datatype;
- to possibly match a specific regular expression pattern.

```shell
% curl --include 'http://localhost:8080/products/S18_4409'
    
HTTP/1.1 200 
Content-Type: application/json;charset=UTF-8

{
    "@id": "/products/S18_4409",
    "type": "/terms#Product",
    "label": "1932 Alfa Romeo 8C2300 Spider Sport",
    "comment": "This 1:18 scale precision die cast replica features the 6 front headlights …",
    "code": "S18_4409",
    "stock": 6553,
    "line": {
        "@id": "/product-lines/vintage-cars",
        "label": "Vintage Cars"
    },
    "scale": "1:18",
    "vendor": "Exoto Designs",
    "price": 92.03
}
```

The constraints in the extended model are leveraged by the engine in a number of ways, for instance to optimize the JSON
representation of the RDF description in order to make it more usable for front-end development, omitting nested arrays
where a property is known to occur at most once and so on.

## Parameterizing Models

The`filter()` and `server()` guards in the extended model also introduce the concept
of [parametric](../references/spec-language.md#parameters) model.

The `filter` guard states that nested constraints ae to be used only selecting existing resources to be exposed as
container members and not for extracting outgoing data and validating incoming data.

The `convey` guard states that nested constraints are to be used only for extracting outgoing data and validating
incoming data and not for selecting existing resources to be exposed as container members.

The `server` guard states that guarded properties are server-managed and will be considered only when retrieving or
deleting resources, but won't be accepted as valid content on resource creation and updating.

In the most general form, models may be parameterized on for different [axes](../references/spec-language.md#parameters).
Constraints specified outside parametric sections are unconditionally enabled.

## Controlling Access

Parametric models support the definition of resource-level task/role-based access control rules.

```java
or(relate(),role(Toys.staff)).then(…)
```

This guard states that the product catalog and the contained resources are accessible only if the request **either** has
a `GET` method **or** is performed by a user in the `toys:staff` role.

Parametric models support also the definition of fine-grained access control rules and role-dependent read/write resource
views.

```java
role(Toys.staff).then(field(Toys.buy,required(),
		datatype(XSD.DECIMAL),
		minInclusive(literal(decimal(0))),
		maxInclusive(literal(decimal(1000)))
		))
```

This `role` guard states that the `toys:buy` price will be visible only if the request is performed by a user in
the `toys:staff` role.

User roles are usually granted to requests by authentication/authorization wrappers, like in the following naive sample:

```diff
@WebFilter(urlPatterns="/*")
public final class Sample extends JEEServer {

	public Sample() {
		delegate(context -> context

				.set(graph(), () -> new Graph(new SailRepository(new MemoryStore())))
				.set(engine(), GraphEngine::new)

				.exec(new Toys())

				.get(() -> gateway()

						.with(preprocessor(request -> request.base(Toys.Base)))

+						.with(bearer("secret", Toys.staff))

						.wrap(router()

								.path("/products/*", new Products())

						)

				)
		);
	}

}
```

```shell
% curl --include 'http://localhost:8080/products/S18_4409'

HTTP/1.1 200 
Content-Type: application/json;charset=UTF-8

{
    "@id": "http://localhost:8080/products/S18_4409",
    
    ⋮
    
    "price": 92.03,
}
```

```shell
% curl --include \
    --header 'Authorization: Bearer secret' \
    'http://localhost:8080/products/S18_4409'
    
HTTP/1.1 200 
Content-Type: application/json;charset=UTF-8

{
    "@id": "http://localhost:8080/products/S18_4409",
    
    ⋮
    
    "price": 92.03,
    "buy": 43.26 # << buy price included
}
```

```shell
% curl --include --request DELETE \
    'http://localhost:8080/products/S18_4409'
    
HTTP/1.1 403 Forbidden # << user not authenticated in the `toys:staff` role

```

# Pre/Postprocessing

We'll now complete the product catalog, adding:

- a slug generator for assigning meaningful names to new resources;
- postprocessing scripts for updating server-managed properties and perform other housekeeping tasks when resources are
  created or modified.

Copy [ProductsCreate.ql](https://github.com/metreeca/link/tree/main/metreeca-toys/src/main/resources/com/metreeca/toys/ProductsCreate.ql)
to the `src/main/resources/` directory and extend `Products` as follows:

```diff
public final class Products extends Delegator {

	public Products() {
		delegate(driver(or(relate(), role(Toys.staff)).then(
				
				// …
				
		)).wrap(router()

				.path("/", router()
						.get(browser())
+						.post(creator(new ProductsSlug())
+								.with(postprocessor(update(text(Products.class, "ProductsCreate.ql"))))
+						)
				)

				.path("/*", router()
						.get(relator())
						.put(updater())
						.delete(deleter())
				)

		));
	}

}
```

```java
public final class ProductsSlug implements Function<Request, String> {

	private final Graph graph=asset(graph());

	@Override
	public String apply(final Request request) {
		return graph.exec(connection -> {

			final Value scale=request.body(jsonld()).get()
			  .flatMap(model -> new LinkedHashModel(model)
					  .filter(null, Toys.scale, null)
					  .objects()
							.stream()
							.findFirst()
					)
					.orElse(literal("1:1"));

			int serial=0;

		try ( final RepositoryResult<Statement> matches=connection.getStatements(
				null, Toys.scale, scale
		) ) {
				for (; matches.hasNext(); matches.next()) { ++serial; }
			}

			String code="";

			do {
				code=String.format("S%s_%d", scale.stringValue().substring(2), serial);
	  } while ( connection.hasStatement(
			  null, Toys.code, literal(code), true
	  ) );

			return code;

		});
	}

}
```

The slug generator assigns newly created resources a unique identifier based on their scale.

!!! warning "Slug Synchronization"
Real-world slug generators depending on shared state would take care that operations are synchronized among different
transactions in order to prevent the creation of duplicate identifiers.

```sparql
prefix toys: <terms#>

prefix owl: <http://www.w3.org/2002/07/owl#>
prefix xsd: <http://www.w3.org/2001/XMLSchema#>

#### assign the unique scale-based product code generated by the slug function ####

insert { $this toys:code $name } where {};


#### initialize stock #############################################################

insert { $this toys:stock 0 } where {};
```

The *ProductsCreate.ql* SPARQL Update postprocessing script updates server-managed `toys:code` and `toys:stock`
properties after a new product is added to the catalog.

SPARQL Update postprocessing scripts are executed after the state-mutating HTTP request is successfully completed, with
some [pre-defined bindings](../javadocs/?com/metreeca/rdf/assets/Graph.html#configure-M-O-java.util.function.BiConsumer...-)
like the `$this` variable holding the IRI of the targe resource either as derived from the HTTP request or as defined by
the `Location` HTTP header after a POST request.

Request and response RDF payloads may also
be [pre](../javadocs/?com/metreeca/rest/Wrapper.html#preprocessor-java.util.function.Function-)
and [post](../javadocs/?com/metreeca/rest/Wrapper.html#postprocessor-java.util.function.Function-)-processed using custom
filtering functions.

# Localization

Multi-lingual content retrieval is fully supported,
with [compact rendering](https://www.w3.org/TR/json-ld11/#language-indexing) for shapes including
either [`localized()`](../javadocs/?com/metreeca/json/shapes/Localized.html)
or  [`lang()`](../javadocs/?com/metreeca/json/shapes/Lang.html) constraints.

Retrieved localizations may be limited to a predefined set of language tags using a `convey` language constraint, like
for instance:

```java
convey(lang("en","it","de"))
```

Additional language constraints may be introduced at query time using the `Accept-Language` HTTP header, like for
instance:

```http request
GET http://localhost:8080/products/S18_3140
Accept-Language: en
```

# Next Steps

To complete your tour of the framework:

- walk through the [consuming tutorial](consuming-jsonld-apis.md) to learn how to interact with model-driven REST APIs to
  power client apps like the demo [online product catalog](https://demo.metreeca.com/apps/shop/);
- explore the [framework](../javadocs/?overview-summary.html) to learn how to develop your own custom wrappers and
  handlers and to extend your server with additional services.
