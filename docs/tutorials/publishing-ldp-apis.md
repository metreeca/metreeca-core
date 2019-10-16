---
title:	        Publishing Model‑Driven Linked Data REST APIs
excerpt:        Hands-on guided tour of model-driven linked data REST APIs publishing
redirect_from: /tutorials/linked-data-publishing
---

This example-driven tutorial introduces the main building blocks of the Metreeca/Link model-driven linked data framework. Basic familiarity with [linked data](https://www.w3.org/standards/semanticweb/data) concepts and [REST](https://en.wikipedia.org/wiki/Representational_state_transfer) APIs is required.

In the following sections you will learn how to use the framework to develop a linked data server and to publish model-driven linked data resources through REST APIs that automatically support fine-grained role‑based read/write access control,  faceted search and incoming data validation.

In the tutorial we will work with a semantic version of the [BIRT](http://www.eclipse.org/birt/phoenix/db/) sample dataset, cross-linked to [GeoNames](http://www.geonames.org/) entities for cities and countries. The BIRT sample is a typical business database, containing tables such as *offices*, *customers*, *products*, *orders*, *order lines*, … for *Classic Models*, a fictional world-wide retailer of scale toy models. Before moving on you may want to familiarize yourself with it walking through the [search and analysis tutorial](https://metreeca.github.io/self/tutorials/search-and-analysis/) of the [Metreeca/Self](https://github.com/metreeca/self) self-service linked data search and analysis tool, which works on the same data.

We will walk through the REST API development process focusing on the task of publishing the [Product](https://demo.metreeca.com/apps/self/#endpoint=https://demo.metreeca.com/sparql&collection=https://demo.metreeca.com/terms#Product) catalog as a [Linked Data Platform](https://www.w3.org/TR/ldp-primer/) (LDP) Basic Container and a collection of associated RDF resources.

You may try out the examples using your favorite API testing tool or working from the command line with toos like `curl` or `wget`.

A Maven project with the code for the complete demo app is available on [GitHub](https://github.com/metreeca/demo): clone or [download](https://github.com/metreeca/demo/archive/master.zip) it to your workspace and open in your favorite IDE. If you are working with IntelliJ IDEA you may just use the `Demo` pre-configured run configuration to deploy and update the local server instance.

# Getting Started

To get started, set up a Java 1.8 project, adding required dependencies for the Metreeca/Link [adapters](../javadocs/) for the target deployment server and the target graph storage option. In this tutorial we will deploy to a Servlet 3.1 container with a RDF4J Memory store,  so using Maven:

```xml
<dependencies>

    <dependency>
        <groupId>com.metreeca</groupId>
        <artifactId>metreeca-j2ee</artifactId>
        <version>{{ page.version }}</version>
    </dependency>

    <dependency>
	    <groupId>com.metreeca</groupId>
	    <artifactId>metreeca-rdf4j</artifactId>
        <version>{{ page.version }}</version>
    </dependency>

    <dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>javax.servlet-api</artifactId>
        <version>3.1.0</version>
        <scope>provided</scope>
    </dependency>
  
</dependencies>
```

Then define a minimal server stub like:

```java
import com.metreeca.rdf4j.RDF4JMemory;
import com.metreeca.rest.*;
import com.metreeca.rest.wrappers.Server;
import com.metreeca.servlet.Gateway;

import javax.servlet.annotation.WebFilter;

import static com.metreeca.rdf.services.Graph.graph;


@WebFilter(urlPatterns="/*") public final class Demo extends Gateway {

	@Override protected Handler load(final Context context) {
		return context

				.set(graph(), () -> new RDF4JMemory())

				.get(() -> new Server()

						.wrap((Request request) -> request.reply(response ->
								response.status(Response.OK))
						)

				);
	}

}
```

The stub configures the application to handle any resource using a barebone [handler](../javadocs/?com/metreeca/rest/Handler.html) always replying to incoming [requests](../javadocs/?com/metreeca/rest/Request.html) with a [response](../javadocs/?com/metreeca/rest/Response.html) including a `200` HTTP status code. The standard [Server](../javadocs/?com/metreeca/rest/wrappers/Server.html) wrapper provides default pre/postprocessing services and shared error handling..

Compile and deploy the web app to your favorite servlet container and try your first request:

```sh
% curl --include 'http://localhost:8080/'

HTTP/1.1 200
```

The [context](../javadocs/?com/metreeca/rest/Context.html) argument handled to the app loader lambda manages the shared system-provided tools and can be used to customize them and to run app initialization tasks.

```java
@Override protected Handler load(final Context context) {
  return context

      .set(graph(), () -> new RDF4JMemory())

      .exec(() -> service(graph()).exec(connection -> {
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

      );
}
```

Here we are customizing the shared system-wide [graph](../javadocs/?com/metreeca/tray/rdf/Graph.html) database as an ephemeral heap-based RDF4J store, initializing it on demand with the BIRT dataset. The framework includes other adapters for major RDF storage solutions: explore the [Storage Adapters](../javadocs/) package groups  in the API reference to find your one and don't forget to include the Maven dependency specified in the package docs.

The static [Context.service()](../javadocs/com/metreeca/rest/Context.html#service-java.util.function.Supplier-) service locator method provides access to shared tools inside context initialisation tasks and wrapper/handlers **constructors**.

Complex initialization tasks can be easily factored to a dedicated class:

```java
public final class BIRT implements Runnable {

	public static final String Base="https://demo.metreeca.com/";
	public static final String Namespace=Base+"terms#";

	@Override public void run() {
		service(graph()).exec(connection -> {
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
@Override protected Handler load(final Context context) {
  return context

      .set(graph(), RDF4JMemory::new)

      .exec(new BIRT())

      .get(() -> new Server()

          .wrap(new Rewriter(BIRT.Base))

          .wrap((Request request) -> request.reply(response ->
              response.status(Response.OK))
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

    .wrap(new Rewriter(BIRT.Base))

    .wrap(new Router()

        .path("/products/*", new Router()
              
            .path("/", new Worker()
                .get(request -> request.reply(response -> response
                    .status(Response.OK)
                    .body(rdf(), service(graph()).exec(connection -> {
                      return stream(connection
                          	.getStatements(null, RDF.TYPE, iri(BIRT.Namespace, "Product"))
                          )
                          .map(Statement::getSubject)
                          .map(p -> statement(iri(request.item()), LDP.CONTAINS, p))
                          .collect(toList());
                    }))
                ))
            )
              
            .path("/{code}", new Worker()
                .get(request -> request.reply(response -> response
                    .status(Response.OK)
                    .body(rdf(), service(graph()).exec(connection -> {
                      return asList(connection
                          .getStatements(iri(request.item()), null, null)
                      );
                    }))
                ))
            )
              
        )

    )
```

```sh
% curl --include 'http://localhost:8080/products/S18_4409'

HTTP/1.1 200 
Content-Type: text/turtle;charset=UTF-8

@base <http://localhost:8080/products/S18_4409> .

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

## Routers

[Routers](../javadocs/?com/metreeca/rest/handlers/Router.html) dispatch requests on the basis of the [request path](../javadocs/com/metreeca/rest/Request.html#path--), ignoring the leading segment possibly already matched by wrapping routers.

Requests are forwarded to a registered handler if their path is matched by an associated pattern defined by a sequence of steps according to the following rules:

| pattern step | matching path step   | definition                                                   |
| ------------ | -------------------- | ------------------------------------------------------------ |
| `/`          | `/`                  | empty / matches only the empty step                          |
| `/<step>`    | `/<step>`            | literal / matches step verbatim                              |
| `/{}`        | `/<step>`            | wildcard / matches a single step                             |
| `/{<key>}`   | `/<step>`            | placeholder / match a single path step, adding the matched `<key>`/`<step>` entry to request [parameters](../javadocs/com/metreeca/rest/Request.html#parameters—); the matched `<step>` name is URL-decoded before use |
| `/*`         | `/<step>[/<step>/…]` | prefix / matches one or more trailing steps                  |

Registered path patterns are tested in order of definition.

If the router doesn't contain a matching handler, no action is performed giving the container adapter a fall-back opportunity to handle the request.

## Workers

[Workers](../javadocs/?com/metreeca/rest/handlers/Worker.html) dispatch requests on the basis of the [request method](../javadocs/com/metreeca/rest/Request.html#method--), providing overridable default implementation for `OPTIONS` and `HEAD` methods.

## Delegators

Again, complex handlers can be easily factored to dedicated classes:

```java
() -> new Server()

        .wrap(new Rewriter(BIRT.Base))

        .wrap(new Router()

            .path("/products/*", new Products())

        )
```

```java
public final class Products extends Delegator {

	public Products() {
		delegate(new Router()

				.path("/", new Worker()
						.get(request -> request.reply(response -> response
								.status(Response.OK)
								.body(rdf(), service(graph()).exec(connection -> {
									return stream(connection
												.getStatements(null, RDF.TYPE, iri(BIRT.Namespace, "Product"))
											)
											.map(Statement::getSubject)
											.map(p -> statement(iri(request.item()), LDP.CONTAINS, p))
											.collect(toList());
								}))
						))
				)

				.path("/{code}", new Worker()
						.get(request -> request.reply(response -> response
								.status(Response.OK)
								.body(rdf(), service(graph()).exec(connection -> {
									return asList(connection
											.getStatements(iri(request.item()), null, null)
									);
								}))
						))
				)

		);
	}
}
```

The [Delegator](../javadocs/?com/metreeca/rest/handlers/Delegator.html) abstract handler provides a convenient way of packaging complex handlers assembled as a combination of other handlers and wrappers.

# Model-Driven Handlers

Standard resource action handlers can be defined using high-level declarative models that drive automatic fine‑grained role‑based read/write access control, faceted search,  incoming data validation and bidirectional conversion between RDF and [idiomatic](../references/idiomatic-json) JSON payloads, as demonstrated in the [REST APIs interaction tutorial](interacting-with-ldp-apis).

[Actors](../javadocs/?com/metreeca/rest/handlers/handlers/Actor.html) provide default shape-driven implementations for CRUD actions on LDP resources and containers identified by the request [focus item](../javadocs/com/metreeca/rest/Request.html#item--).

| actor                                                        | action                                                       |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [Relator](../javadocs/?com/metreeca/rest/handlers/actors/Relator.html) | container/resource retrieval / retrieves the detailed RDF description of the target item and (optionally) the digest RDF description of the contained resources; on containers, supports extended [faceted search](interacting-with-ldp-apis#faceted-search), sorting and pagination |
| [Creator](../javadocs/?com/metreeca/rest/handlers/actors/Creator.html) | container resource creation / uploads the detailed RDF description of a new resource to be inserted into  the target item |
| [Updater](../javadocs/?com/metreeca/rest/handlers/actors/Updater.html) | resource updating / updates the detailed RDF description of the target item |
| [Deleter](../javadocs/?com/metreeca/rest/handlers/actors/Deleter.html) | resource deletion / deletes the detailed RDF description of the target item |

<p class="warning">Only LDP Basic and Direct Containers are currently supported.</p>

```diff
@Override protected Handler load(final Context context) {
		return context

				.set(graph(), RDF4JMemory::new)
+				.set(engine(), GraphEngine::new)

				.exec(new BIRT())

				.get(() -> new Server()

						.wrap(new Rewriter(BIRT.Base))

						.wrap(new Router()

								.path("/products/*", new Products())

						)
				);
	}
```

Actors delegate transaction management, data validation and trimming and CRUD operations to a customizable LDP engine.

CRUD perations are performed on the graph neighbourhood of the target target item(s)  matched by the  [shape](../javadocs/?com/metreeca/form/Shape.html) model [associated](../javadocs/com/metreeca/rest/Message.html#shape--) to the request, after redaction according to the request user roles and to actor-specific task,  area and mode parameters.

## Defining Models

Let's start by defining a barebone model stating that all resources of class `Product` are to be published as container items exposing only `rdf:type`, `rdfs:label`  and `rdfs:comment` properties.

```java
public final class Products extends Delegator {

	public Products() {
		delegate(new Driver(and(

				field(RDF.TYPE),
				field(RDFS.LABEL),
				field(RDFS.COMMENT)

		)).wrap(new Router()

				.path("/", new Worker()
						.get(new Relator())
						.post(new Creator())
				)

				.path("/*", new Worker()
						.get(new Relator())
						.put(new Updater())
						.delete(new Deleter())
				)

		));
	}

}
```

The [Driver](../javadocs/index.html?com/metreeca/rest/wrappers/Driver.html) wrapper associated a linked data model to incoming requests, driving the operations of nested actors and other model-aware handlers.

Linked data models are defined with a [SHACL](https://www.w3.org/TR/shacl/)-based [specification language](../references/spec-language), assembling shape [building blocks](../references/spec-language#shapes) using a simple Java DSL.

<p class="note">Direct import of of SHACL specs is planned.</p>
As soon as the server is redeployed, the updated REST API exposes only the data specified in the driving model.

```sh
% curl --include \
	--header 'Accept: application/json' \
	'http://localhost:8080/products/S18_4409'

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
new Driver(

    member().then(

        filter().then(
            field(RDF.TYPE, required(), BIRT.Product)
        ),

        convey().then(

            field(RDF.TYPE, required()),

            field(RDFS.LABEL, required(),
                datatype(XMLSchema.STRING),
                maxLength(50)
            ),

            field(RDFS.COMMENT, required(),
                datatype(XMLSchema.STRING),
                maxLength(500)
            ),

            and(

                server().then(field(BIRT.code, required())),

                field(BIRT.line, required(), clazz(BIRT.ProductLine),

                    relate().then(field(RDFS.LABEL, required()))
                ),

                field(BIRT.scale, required(),
                    datatype(XMLSchema.STRING),
                    placeholder("1:N"),
                    pattern("1:[1-9][0-9]{1,2}")
                ),

                field(BIRT.vendor, required(),
                    datatype(XMLSchema.STRING),
                    maxLength(50)
                )

            ),

            and(

                server().then(field(BIRT.stock, required(),
                    datatype(XMLSchema.INTEGER),
                    minInclusive(literal(integer(0))),
                    maxExclusive(literal(integer(10000)))
                )),

                field(BIRT.sell, alias("price"), required(),
                    datatype(XMLSchema.DECIMAL),
                    minExclusive(literal(decimal(0))),
                    maxExclusive(literal(decimal(1000)))
                ),

                role(BIRT.staff).then(field(BIRT.buy, required(),
                    datatype(XMLSchema.DECIMAL),
                    minInclusive(literal(decimal(0))),
                    maxInclusive(literal(decimal(1000)))
                ))

            )

        )

    )

)
```

The `filter` section states that this model describes resources living inside an LDP Basic Container defined by the following LDP properties (that is, a container whose member are the instances of class `birt:Product`):

```turtle
<products/> a ldp:BasicContainer;
	ldp:isMemberOfRelation rdf:type;
	ldp:membershipResource birt:Product.
```

The extended resource model makes use of a number of additional blocks to precisely define the expected shape of the RDF description of the member resources, for instance including `required` and `datatype` and `pattern` constraints to state that `rdfs:label`, `rdfs:comment`, `birt:code`, `birt:scale` and `birt:vendor` values are expected:

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

The constraints in the extended model are leveraged by the engine in a number of ways, for instance to optimize the JSON representation of the RDF description in order to make it more usable for front-end development, omitting nested arrays where a property is known to occur at most once and so on.

## Parameterizing Models

The `member()`, `filter()`, `convey()` and `server()` guards in the extended model also introduce the central concept of [parametric](../references/spec-language#parameters) model.

The `member` guard states that nested constraints define the shape of a container member resource.

The `filter` guard states that nested constraints ae to be used only selecting existing resources to be exposed as container members and not for extracting outgoing data and validating incoming data.

The `convey` guard states that nested constraints are to be used only for extracting outgoing data and validating incoming data and not for selecting existing resources to be exposed as container members. Constraints defined outside the `convey` block, will be used for both operations.

The `server` guard states that guarded properties are server-managed and will be considered only when retrieving or deleting resources, but won't be accepted as valid content on resource creation and updating.

In the most general form, models may be parameterized on for different [axes](../references/spec-language#parameters). Constraints specified outside parametric sections are unconditionally enabled.

## Controlling Access

Parametric models support the definition of fine-grained access control rules and role-dependent read/write resource views.

```java
role(BIRT.staff).then(field(BIRT.buy, and(required(),
		datatype(XMLSchema.DECIMAL),
		minInclusive(literal(decimal(0))),
		maxInclusive(literal(decimal(1000)))
)))
```

This `role` guard states that the `birt:buy` price will be visible only if the request is performed by a user in the `birt:staff` role, usually as verified by authtentication/authorization wrappers, like in the following naive sample:

```java
() -> new Server()

    .wrap(new Rewriter(BIRT.Base))
  
  	// set request roles if user is authorized

    .wrap((Wrapper)handler -> request ->
          authorized(request) ? handler.handle(request.roles(BIRT.staff))
              : request.safe() ? handler.handle(request)
              : request.reply(response -> response.status(Response.Unauthorized))
     )

    .wrap(new Router()

            .path("/products/*", new Products())

    )
```

```java
private static boolean authorized(final Request request) {
    return request.header("Authorization").orElse("").equals("Bearer secret");
}
```

```shell
% curl --include \
	--header 'Authorization: Bearer secret' \
	--header 'Accept: application/json' \
	'http://localhost:8080/products/S18_4409'
	
HTTP/1.1 200 
Content-Type: application/json;charset=UTF-8

{
    "@id": "http://localhost:8080/products/S18_4409",
    
    ⋮
    
    "vendor": "Exoto Designs",
    "stock": 6553,
    "price": 92.03,
    "buy": 43.26 # << buy price included

}
```

# Pre/Postprocessing

We'll now complete the product catalog, adding:

- a slug generator for assigning meaningful names to new resources;
- postprocessing scripts for updating server-managed properties and perform other housekeeping tasks when resources are created or modified.

```java
new Router()

	.path("/", new Worker()
			.get(new Relator())
			.post(new Creator(new ScaleSlug())
        .with(postprocessor(Graph.update(Codecs.text(
          Products.class, "ProductsCreate.ql"
        ))))
       )
	)

	.path("/*", new Worker()
			.get(new Relator())
			.put(new Updater())
			.delete(new Deleter())
	)
```

```java
private static final class ScaleSlug implements Function<Request, String> {

	private final Graph graph=service(graph());


	@Override public String apply(final Request request) {
		return graph.exec(connection -> {

			final Value scale=request.body(rdf())
					.value()
					.flatMap(model -> new LinkedHashModel(model)
							.filter(null, BIRT.scale, null)
							.objects()
							.stream()
							.findFirst()
					)
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

The *ProductsCreate.ql* SPARQL Update postprocessing script updates server-managed `birt:code` and `birt:stock` properties after a new product is added to the catalog.

SPARQL Update postprocessing scripts are executed after the state-mutating HTTP request is successfully completed, with some [pre-defined bindings](../javadocs/com/metreeca/rdf/services/Graph.html#configure-M-O-java.util.function.BiConsumer...-) like the `$this` variable holding the IRI of the targe resource either as derived from the HTTP request or as defined by the `Location` HTTP header after a POST request.

Request and response RDF payloads may also be [pre](../javadocs/com/metreeca/rest/Wrapper.html#preprocessor-java.util.function.Function-) and [post](../javadocs/com/metreeca/rest/Wrapper.html#postprocessor-java.util.function.Function-)-processed using custom filtering functions.

# Next Steps

To complete your tour of the framework:

- walk through the [interaction tutorial](interacting-with-ldp-apis) to learn how to interact with model-driven REST APIs to power client apps like the demo [online product catalog](https://demo.metreeca.com/apps/shop/);
- explore the standard [library](../javadocs/?overview-summary.html) to learn how to develop your own custom wrappers and handlers and to extend your server with additional services like [SPARQL endpoints](../javadocs/?com/metreeca/rdf/handlers/package-summary.html).
