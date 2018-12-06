---
title:	    Publishing Model‑Driven Linked Data REST APIs
excerpt:    Hands-on guided tour of model-driven linked data REST APIs publishing
---

This example-driven tutorial introduces the main building blocks of the Metreeca/Link model-driven linked data framework. Basic familiarity with [linked data](https://www.w3.org/standards/semanticweb/data) concepts and [REST](https://en.wikipedia.org/wiki/Representational_state_transfer) APIs is required.

In the following sections you will learn how to use the framework to develop a linked data server and to publish model-driven linked data resources through REST APIs that automatically support fine-grained role‑based read/write access control,  faceted search and incoming data validation.

In the tutorial we will work with a semantic version of the [BIRT](http://www.eclipse.org/birt/phoenix/db/) sample dataset, cross-linked to [GeoNames](http://www.geonames.org/) entities for cities and countries. The BIRT sample is a typical business database, containing tables such as *offices*, *customers*, *products*, *orders*, *order lines*, … for *Classic Models*, a fictional world-wide retailer of scale toy models. Before moving on you may want to familiarize yourself with it walking through the [search and analysis tutorial](https://metreeca.github.io/self/tutorials/search-and-analysis/) of the [Metreeca/Self](https://github.com/metreeca/self) self-service linked data search and analysis tool, which works on the same data.

We will walk through the REST API development process focusing on the task of publishing the [Product](https://demo.metreeca.com/apps/self/#endpoint=https://demo.metreeca.com/sparql&collection=https://demo.metreeca.com/terms#Product) catalog as a [Linked Data Platform](https://www.w3.org/TR/ldp-primer/) (LDP) Basic Container and a collection of associated RDF resources.

You may try out the examples using your favorite API testing tool or working from the command line with toos like `curl` or `wget`.

A Maven project with the code for the complete demo app is available on [GitHub](https://github.com/metreeca/demo/tree/tutorial): clone or [download](https://github.com/metreeca/demo/archive/tutorial.zip) it to your workspace and open in your favorite IDE. If you are working with IntelliJ IDEA you may just use the `Demo` pre-configured run configuration to deploy and update the local server instance.

# Getting Started

To get started, set up a Java 1.8 project, adding required dependencies for the Metreeca/Link [adapter](../javadocs/) for the target deployment server. In this tutorial we will deploy to a Servlet 3.1 container like Tomcat 8,  so using Maven:

```xml
<dependencies>

    <dependency>
    <groupId>com.metreeca</groupId>
    <artifactId>j2ee</artifactId>
    <version>${project.version}</version>
    </dependency>

    <dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>3.1.0</version>
    <scope>provided</scope>
    </dependency>

</dependencies>
```

Then define a minimal server stub looks like:

```java
import com.metreeca.j2ee.Gateway;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.rest.wrappers.Server;

import javax.servlet.annotation.WebListener;


@WebListener public final class Demo extends Gateway {

	public Demo() {
		super("/*", tray -> tray.get(() -> new Server()

				.wrap((Request request) -> request.reply(response ->
						response.status(Response.OK))
				)

		));
	}

}
```

The stub configures the application to handle any resource using a barebone [handler](../javadocs/?com/metreeca/rest/Handler.html) always replying to incoming [requests](../javadocs/?com/metreeca/rest/Request.html) with a [response](../javadocs/?com/metreeca/rest/Response.html) including a `200` HTTP status code. The standard [Server](../javadocs/?com/metreeca/rest/wrappers/Server.html) wrapper provides default pre/post-processing services and shared error handling..

Compile and deploy the web app to your favorite servlet container and try your first request:

```sh
% curl --include 'http://localhost:8080/'

HTTP/1.1 200
```

The [tray](../javadocs/?com/metreeca/tray/Tray.html) argument handled to the app loader lambda manages the shared system-provided tools and can be used to customize them and to run app initialization tasks.

```java
public Demo() {
	super("/*", tray -> tray

			.set(Graph.Factory, RDF4JMemory::new)

			.exec(() -> tool(Graph.Factory).update(connection -> {
                try {
                    connection.add(
                        BIRT.class.getResourceAsStream("BIRT.ttl"),
                        BIRT.Base, RDFFormat.TURTLE
                    );
                } catch ( final IOException e ) {
                    throw new UncheckedIOException(e);
                }
			}))

			.get(() -> new Server()

					.wrap((Request request) -> request.reply(response ->
							response.status(Response.OK))
					)

			)
	);
}
```

Here we are customizing the system-wide [graph](../javadocs/?com/metreeca/tray/rdf/Graph.html) database as an ephemeral heap-based RDF4J store, initializing it with the BIRT dataset.

The static [Tray.tool()](../javadocs/com/metreeca/tray/Tray.html#tool-java.util.function.Supplier-) service locator method provides access to shared tools inside tray initialisation tasks and wrapper/handlers constructors.

Complex initialization tasks can be easily factored to a dedicated class:

```java
public final class BIRT implements Runnable {

	public static final String Base="https://demo.metreeca.com/";
	public static final String Namespace=Base+"terms#";

	@Override public void run() {
		tool(Graph.Factory).update(connection -> {
            try {
                connection.add(
                    getClass().getResourceAsStream("BIRT.ttl"), 
                    BIRT.Base, RDFFormat.TURTLE
                );
            } catch ( final IOException e ) {
                throw new UncheckedIOException(e);
            }
		});
	}

}
```

```java
public Demo() {
	super("/*", tray -> tray

			.exec(new BIRT())

			.get(() -> new Server()
                 
                    .wrap(new Rewriter().base(BIRT.Base))

					.wrap((Request request) -> request.reply(response ->
							response.status(Response.OK))
					)

			)
	);
}
```

[Wrappers](../javadocs/?com/metreeca/rest/Wrapper.html) inspect and possibly alter incoming requests and outgoing responses before they are forwarded to wrapped handlers and returned to wrapping containers.

The [rewriter](../javadocs/?com/metreeca/rest/wrappers/Rewriter.html) wrapper rewrites incoming and outgoing RDF payloads from the external network-visible server base (`http://localhost:8080/`) to an internal canonical base (`https://demo.metreeca.com/`), ensuring data portability between development and production environments and making it possible to load the static RDF dataset during server initialization, while the external and possibly request-dependent base is not yet known. When external and internal bases match, as in production, rewriting is effectively disabled avoiding any performance hit.

# Handling Requests

Requests are dispatched to their final handlers through a hierarchy of wrappers and delegating handlers.

```java
() -> new Server()

    	.wrap(new Rewriter().base(BIRT.Base))

		.wrap(new Router()

				.path("/products/", new Router()
						.path("/", new Worker()
								.get(new Browser())
								.post(new Creator())
						)
						.path("/*", new Worker()
								.get(new Relator())
								.put(new Updater())
								.delete(new Deleter())
						)
				)

		)
```

```sh
% curl --include 'http://localhost:8080/products/S18_4409'

HTTP/1.1 200 
Vary: Accept
Link: <http://www.w3.org/ns/ldp#Resource>; rel="type"
Link: <http://www.w3.org/ns/ldp#RDFSource>; rel="type"
Content-Type: text/turtle;charset=UTF-8

<http://localhost:8080/products/S18_4409> a <http://localhost:8080/terms#Product>;
  <http://www.w3.org/2000/01/rdf-schema#label> "1932 Alfa Romeo 8C2300 Spider Sport";
  <http://localhost:8080/terms#code> "S18_4409";
  <http://localhost:8080/terms#scale> "1:18";
  <http://localhost:8080/terms#vendor> "Exoto Designs";
  <http://localhost:8080/terms#buy> 43.26;
  <http://localhost:8080/terms#sell> 92.03;
  <http://localhost:8080/terms#stock> 6553;
  ⋮
```

## Routers

[Routers](../javadocs/?com/metreeca/rest/handlers/Router.html) dispatch requests on the basis of the [request path](../javadocs/com/metreeca/rest/Request.html#path--), taking into account the portion of the path already matched by wrapping routers.

Requests are forwarded to a registered handler if their path matches the associated path pattern according to the following rules, in order of precedence.

| pattern     | matching paths                                        | handling mode                                                |
| ----------- | ----------------------------------------------------- | ------------------------------------------------------------ |
| `/<path>`   | `/<path>`<br />`/<path>/`                             | exact / matches path exactly, ignoring trailing slashes      |
| `/<path>/`  | `/<path>`<br />`/<path>/`<br />`/<path>/…/<resource>` | prefix / matches any path sharing the given path prefix, ignoring trailing slashes |
| `/<path>/*` | `/<path>/…/<resource>`                                | subtree / matches any path sharing the given prefix with an non-empty suffix |

Lexicographically longer and preceding paths take precedence over shorter and following ones.

If the index doesn't contain a matching handler, no action is performed giving the container adapter a fall-back opportunity to handle the request.

## Workers

[Workers](../javadocs/?com/metreeca/rest/handlers/Worker.html) dispatch requests on the basis of the [request method](../javadocs/com/metreeca/rest/Request.html#method--), providing overridable default implementation for `OPTIONS` and `HEAD` methods.

## Actors

[Actors](../javadocs/?com/metreeca/rest/handlers/actors/package-summary.html) provide a default implementaton for CRUD actions on LDP resources and Basic containers identified by the request [focus item](../javadocs/com/metreeca/rest/Request.html#item--).

| actor                                                        | action                                                       |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [Browser](../javadocs/?com/metreeca/rest/handlers/actors/Browser.html)⃰ | container browsing / retrieves the detailed RDF description of the target item and (optionally) the compact RDF description of the contained resources; supports extended [faceted search](linked-data-interaction#faceted-search), sorting and pagination |
| [Relator](../javadocs/?com/metreeca/rest/handlers/actors/Relator.html) | resource retrieval / retrieves the detailed RDF description of the target item |
| [Creator](../javadocs/?com/metreeca/rest/handlers/actors/Creator.html)⃰ | resource creation / uploads the detailed RDF description of the target item |
| [Updater](../javadocs/?com/metreeca/rest/handlers/actors/Updater.html)⃰ | resource updating / updates the detailed RDF description of the target item |
| [Deleter](../javadocs/?com/metreeca/rest/handlers/actors/Deleter.html)⃰ | resource deletion / deletes the detailed RDF description of the target item |
| [Builder](../javadocs/?com/metreeca/rest/handlers/actors/Builder.html) | virtual resource retrieval / retrieves the detailed RDF description of the virtual target item |

If a [shape](../javadocs/?com/metreeca/form/Shape.html) model is [associated](../javadocs/com/metreeca/rest/Message.html#shape--) to the request, CRUD operations are performed on the graph neighbourhood of the target target item(s)  identified by the model after redaction according to the request user roles and to actor-specific task,  mode and view parameters.

If no shape model is associated to the request, CRUD operations are performed on the (labelled) [symmetric concise bounded description](https://www.w3.org/Submission/CBD/) of the target item(s).

## Delegators

Again, complex handlers can be easily factored to dedicated classes:

```java
() -> new Server()
    
    	.wrap(new Rewriter().base(BIRT.Base))

		.wrap(new Router()

				.path("/products/", new Products())

		)
```

```java
public final class Products extends Delegator {

	public Products() {
		delegate(new Router()

				.path("/", new Worker()
						.get(new Browser())
						.post(new Creator())
				)

				.path("/*", new Worker()
						.get(new Relator())
						.put(new Updater())
						.delete(new Deleter())
				)

		);
	}

}
```

The [Delegator](../javadocs/?com/metreeca/rest/handlers/Delegator.html) abstract handler provides a convenient way of packaging complex handlers assembled as a combination of other handlers and wrappers.

# Model-Driven Handlers

The behaviour of standard resource action handlers can be fine-tuned using high-level declarative models that drive automatic fine‑grained role‑based read/write access control, faceted search,  incoming data validation and bidirectional conversion between RDF and [idiomatic](../references/idiomatic-json.md) JSON payloads, as demonstrated in the [REST APIs interaction tutorial](linked-data-interaction.md).

## Defining Models

Let's start by defining a barebone model stating that all resources of class `Product` are to be published as container items exposing only `rdf:type`, `rdfs:label`  and `rdfs:comment` properties.

```java
public final class Products extends Delegator {

	public Products() {
		delegate(new Driver().shape(and(

				trait(RDF.TYPE),
				trait(RDFS.LABEL),
				trait(RDFS.COMMENT)

		)).wrap(new Router()

				.path("/", new Worker()
						.get(new Browser())
						.post(new Creator()))

				.path("/*", new Worker()
						.get(new Relator())
						.put(new Updater())
						.delete(new Deleter()))
		));
	}
}
```

The [Driver](../javadocs/index.html?com/metreeca/rest/wrappers/Driver.html) wrapper associated a linked data model to incoming requests, driving the operations of nested actors and other model-aware handlers.

Linked data models are defined with a [SHACL](https://www.w3.org/TR/shacl/)-based [specification language](../references/spec-language.md), assembling shape [building blocks](../references/spec-language.md#shapes) using a simple Java DSL.

<p class="note">Direct import of of SHACL specs is planned.</p>

As soon as the server is redeployed, the updated REST API exposes only the data specified in the driving model.

```sh
% curl --include \
	--header 'Accept: application/json' \
	'http://localhost:8080/products/S18_4409'

HTTP/1.1 200 
Content-Type: application/json;charset=UTF-8

{
    "this": "http://localhost:8080/products/S18_4409",
    "type": [
        {
            "this": "http://localhost:8080/terms#Product"
        }
    ],
    "label": [
        "1932 Alfa Romeo 8C2300 Spider Sport"
    ],
    "comment": [
        "This 1:18 scale precision die cast replica features…"
    ]
}
```

We'll now refine the initial barebone model, exposing more properties and detailing properties roles and constraints.

```java
new Driver().shape(and(

        trait(RDF.TYPE, only(BIRT.Product)),
        trait(RDFS.LABEL, verify(required(),
                datatype(XMLSchema.STRING), maxLength(50))),
        trait(RDFS.COMMENT, verify(required(),
                datatype(XMLSchema.STRING), maxLength(500))),

        group(

            server(trait(BIRT.code, verify(required()))),

            trait(BIRT.line, and(

                verify(required(),clazz(BIRT.ProductLine)),

                relate(trait(RDFS.LABEL, verify(required())))

            )),

            trait(BIRT.scale, verify(required(),
                datatype(XMLSchema.STRING),
                placeholder("1:N"),
                pattern("1:[1-9][0-9]{1,2}"))),

            trait(BIRT.vendor, verify(required(),
                datatype(XMLSchema.STRING), maxLength(50)))),

        group(

            server(trait(BIRT.stock, verify(required(),
                datatype(XMLSchema.INTEGER),
                minInclusive(literal(integer(0))),
                maxExclusive(literal(integer(10000)))))),

            trait(BIRT.sell, verify(alias("price"), required(),
                datatype(XMLSchema.DECIMAL),
                minExclusive(literal(decimal(0))),
                maxExclusive(literal(decimal(1000))))),

            role(singleton(BIRT.staff), trait(BIRT.buy, verify(required(),
                datatype(XMLSchema.DECIMAL),
                minInclusive(literal(decimal(0))),
                maxInclusive(literal(decimal(1000))))))

        )

))
```

The extended model makes use of a number of additional blocks to precisely define the expected shape of the RDF description of the associated resource, for instance including `required` and `datatype` and `pattern` constraints to state that `rdfs:label`, `rdfs:comment`, `birt:code`, `birt:scale` and `birt:vendor` values are expected:

- to occur exactly once for each resource;
- to be RDF literals of `xsd:string` datatype;
- to possibly match a specific regular expression pattern.

```sh
% curl --include \
	--header 'Accept: application/json' \
	'http://localhost:8080/products/S18_4409'
	
HTTP/1.1 200 
Content-Type: application/json;charset=UTF-8

{
    "this": "http://localhost:8080/products/S18_4409",
    "type": "http://localhost:8080/terms#Product",
    "label": "1932 Alfa Romeo 8C2300 Spider Sport",
    "comment": "This 1:18 scale precision die cast replica features…",
    "code": "S18_4409",
    "line": {
        "this": "http://localhost:8080/product-lines/vintage-cars",
        "label": "Vintage Cars"
    },
    "scale": "1:18",
    "vendor": "Exoto Designs",
    "stock": 6553,
    "price": 92.03
}
```

The constraints in the extended model are leveraged by the engine in a number of ways, for instance to optimize the JSON representation of the RDF description in order to make it more usable for front-end development, omitting nested arrays where a property is known to occur at most once and so on.

## Parameterizing Models

The `verify` and `server` blocks in the extended model also introduce the central concept of [parametric](../references/spec-language.md#parameters) model.

The `verify` block states that nested constraints are to be used only for validating incoming data and not for selecting existing resources to be exposed as container items. Constraints like `trait(rdf:type)`, defined outside the `verify` block, will be used both for selecting relevant resources and validating incoming data.

The `server` block states that nested properties are server-managed and will be considered only when retrieving or deleting resources, but won't be accepted as valid content on resource creation and updating.

In the most general form, models may be parameterized on for different [axes](../references/spec-language.md#parameters). Constraints specified outside parametric sections are unconditionally enabled.

## Controlling Access

Parametric models support the definition of fine-grained access control rules and role-dependent read/write resource views.

```java
role(singleton(BIRT.staff), trait(BIRT.buy, verify(required(),
        datatype(XMLSchema.DECIMAL),
        minInclusive(literal(decimal(0))),
        maxInclusive(literal(decimal(1000)))
)))
```

This `role` block states that the `birt:buy` price will be visible only if the request is performed by a user in the `birt:staff` role, usually as verified by authtentication/authorization wrappers, like in the following naive sample:

```java
private static boolean authorized(final Request request) {
    return request.header("Authorization").orElse("").equals("Bearer secret");
}
```

```java
() -> new Server()

    .wrap(new Rewriter().base(BIRT.Base))

    .wrap((Wrapper)handler -> request ->
          authorized(request) ? handler.handle(request.roles(BIRT.staff))
              : request.safe() ? handler.handle(request)
              : request.reply(response -> response.status(Response.Unauthorized))
     )

    .wrap(new Router()

            .path("/products/", new Products())

    )
```

```shell
% curl --include \
	--header 'Authorization: Bearer secret' \
	--header 'Accept: application/json' \
	'http://localhost:8080/products/S18_4409'
	
HTTP/1.1 200 
Content-Type: application/json;charset=UTF-8

{
    "this": "http://localhost:8080/products/S18_4409",
    
    ⋮
    
    "vendor": "Exoto Designs",
    "stock": 6553,
    "price": 92.03,
    "buy": 43.26 # << buy price included

}
```

# Pre/Post-Processing

We'll now complete the product catalog, adding:

- a pre-processing slug generator for assigning meaningful names to new resources;
- post-processing scripts for updating server-managed properties and perform other housekeeping tasks when resources are created or modified.

```java
new Router()

    .path("/", new Worker()
        .get(new Browser())
        .post(new Creator()
            .slug(new ScaleSlug())
            .sync(text(Products.class, "ProductsCreate.ql"))))

    .path("/*", new Worker()
        .get(new Relator())
        .put(new Updater())
        .delete(new Deleter()
            .sync(text(Products.class, "ProductsDelete.ql"))))
```

```java
private static final class ScaleSlug implements BiFunction<Request, Model, String> {

    private final Graph graph=tool(Graph.Factory);


    @Override public String apply(final Request request, final Model model) {
        return graph.query(connection -> {

            final Value scale=model.filter(null, BIRT.scale, null)
                    .objects().stream().findFirst()
                    .orElse(literal("1:1"));

            int serial=0;

            try (final RepositoryResult<Statement> matches=connection.getStatements(
                    null, BIRT.scale, scale
            )) {
                for (; matches.hasNext(); matches.next()) { ++serial; }
            }

            String code="";

            do {
                code=String.format("S%s_%d", scale.stringValue().substring(2), serial);
            } while ( connection.hasStatement(
                    null, BIRT.code, literal(code), true
            ) );

            return code;

        });
    }

}
```

The slug generator assigns newly created resources a unique identifier based on their scale.

```spa
prefix birt: <terms#>

prefix owl: <http://www.w3.org/2002/07/owl#>
prefix xsd: <http://www.w3.org/2001/XMLSchema#>

#### assign the unique scale-based product code generated by the slug function ####

insert { $this birt:code $name } where {};


#### initialize stock #############################################################

insert { $this birt:stock 0 } where {};
```

The *ProductsCreate.ql* SPARQL Update post-processing script updates server-managed `birt:code` and `birt:stock` properties after a new product is added to the catalog.

SPARQL Update post-processing scripts are executed after the state-mutating HTTP request is successfully completed, with some [pre-defined bindings](../javadocs/com/metreeca/rest/wrappers/Processor.html#sync-java.lang.String-) like the `$this` variable holding the IRI of the targe resource either as derived from the HTTP request or as defined by the `Location` HTTP header after a POST request.

Request and response RDF payloads may also be [pre](../javadocs/com/metreeca/rest/wrappers/Processor.html#pre-java.util.function.BiFunction-) and [post](../javadocs/com/metreeca/rest/wrappers/Processor.html#post-java.util.function.BiFunction-)-processed using custom filtering functions.

# Next Steps

To complete your tour of the framework:

- walk through the [interaction tutorial](linked-data-interaction.md) to learn how to interact with model-driven REST APIs to power client apps like the demo [online product catalog](https://demo.metreeca.com/apps/shop/);
- explore the standard [library](../javadocs/?overview-summary.html) to learn how to develop your own custom wrappers and handlers and to extend your server with additional services like [SPARQL endpoints](../javadocs/?com/metreeca/rest/handlers/sparql/package-summary.html).
