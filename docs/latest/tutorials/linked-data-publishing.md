---
title:	    Publishing Model‑Driven Linked Data REST APIs
excerpt:    Hands-on guided tour of model-driven linked data REST APIs publishing
---

This example-driven tutorial introduces the main building blocks of the Metreeca/Link model-driven linked data framework. Basic familiarity with  [linked data](https://www.w3.org/standards/semanticweb/data) concepts and [REST](https://en.wikipedia.org/wiki/Representational_state_transfer) APIs is required.

In the following sections you will learn how to use the framework to develop a linked data server and to publish model-driven linked data resources through REST APIs, automatically supporting fine grained role‑based read/write access control,  faceted search and incoming data validation.

In the tutorial we will work with a semantic version of the [BIRT](http://www.eclipse.org/birt/phoenix/db/) sample dataset, cross-linked to [GeoNames](http://www.geonames.org/) entities for cities and countries. The BIRT sample is a typical business database, containing tables such as *offices*, *customers*, *products*, *orders*, *order lines*, … for *Classic Models*, a fictional world-wide retailer of scale toy models. Before moving on you may want to familiarize yourself with it walking through the [search and analysis tutorial](https://metreeca.github.io/self/tutorials/search-and-analysis/) of the [Metreeca/Self](https://github.com/metreeca/self) self-service linked data search and analysis tool, which works on the same data.

We will walk through the REST API development process focusing on the task of exposing the [Product](https://demo.metreeca.com/apps/self/#endpoint=https://demo.metreeca.com/sparql&collection=https://demo.metreeca.com/terms#Product) catalog as a [Linked Data Platform](https://www.w3.org/TR/ldp-primer/) (LDP) Basic Container and a collection of associated RDF resources.

You may try out the examples using your favorite API testing tool or working from the command line with toos like `curl` or `wget`.

A Maven project with the code for the complete demo app is available on [GitHub](https://github.com/metreeca/demo/tree/tutorial): clone or [download](https://github.com/metreeca/demo/archive/tutorial.zip) it to your workspace and open in your favorite IDE. If you are working with IntelliJ IDEA you may just use the `Demo` pre-configured run configuration to deploy and update the local server instance.

# Getting Started

To get started, set up a Java 1.8 project, adding required dependencies for the Metreeca/Link [adapter](../javadocs/) for the target deployment server.

In this tutorial we will deploy to a Servlet 3.1 container like Tomcat 8,  so using Maven:

```xml
<dependencies>

    <dependency>
    <groupId>com.metreeca</groupId>
    <artifactId>j2ee</artifactId>
    <version>${module.version}</version>
    </dependency>

    <dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>3.1.0</version>
    <scope>provided</scope>
    </dependency>

</dependencies>
```

## Deploying a Server

<u>Then</u> define a server stub like:

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

The stub configures the application to handle any resource using a minimal [handler](../javadocs/?com/metreeca/rest/Handler.html) always replying to incoming [requests](../javadocs/?com/metreeca/rest/Request.html) with a [response](../javadocs/?com/metreeca/rest/Responsehtml) including a `200` HTTP status code. The standard [Server](../javadocs/?com/metreeca/rest/wrappers/Server.html) wrapper provides default pre/post-processing services and shared error handling.

Compile and deploy to your favorite servlet container and try your first request:

```sh
% curl --include http://localhost:8080/

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
… tray.exec(new BIRT()) …
    

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

## Dispatching Requests

```java
() -> new Server()

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

				.path("/product-lines/", new Router()
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

[Browser](../javadocs/?com/metreeca/rest/handlers/actors/Browser.html), [Relator](../javadocs/?com/metreeca/rest/handlers/actors/Relator.html), [Creator](../javadocs/?com/metreeca/rest/handlers/actors/Creator.html), [Updater](../javadocs/?com/metreeca/rest/handlers/actors/Updater.html), [Deleter](../javadocs/?com/metreeca/rest/handlers/actors/Deleterhtml) standard handlers provides…

<p class="warning">Model-less operations aren't yet fully supported by all action handlers</p>

```sh
% curl --include  http://localhost:8080/products/S18_4409

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

Each *linked data port* specifies how linked data resources whose URLs match a server-relative path pattern will be handled by the linked data server. For instance, the linked data port mapped to `/product-lines/*` specifies  how to handle all of the following linked data resources:

```
https://demo.metreeca.com/product-lines/classic-cars
https://demo.metreeca.com/product-lines/motorcycles
https://demo.metreeca.com/product-lines/planes
…
```

The optional trailing character in the path pattern controls how resources are handled by the matching port according to the following schema.

| path pattern | handling mode                                                |
| ------------ | ------------------------------------------------------------ |
| `<path>`     | the resource at `<path>` will be handled as an LDP RDF Resource, exposing RDF properties as specified by the port shape (more on that in the next steps…) |
| *`<path>/`*  | the resource at `<path>/` will be handled as an LDP Basic Container including all of the RDF resources matched by the port shape; an ancillary port mapped to the `<path>/*` pattern is automatically generated to handle container items as LDP RDF Resources, exposing RDF properties as specified by the port shape |
| `<path>/*`   | every resource with an IRI starting with `<path>/` will be handled as an LDP RDF Resource, exposing RDF properties as specified by the port shape |

Again, complex handlers can be easily factored to dedicated classes:

```java
() -> new Server()

		.wrap(new Router()

				.path("/products/", new Products())
				.path("/product-lines/", new ProdutLines())

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

@@@ [Delegator](../javadocs/?com/metreeca/rest/handlers/actors/Delegator.html)

# Model-Driven REST APIs

Engine-managed, demonstrated in the Interaction tutorial

- faceted search
- REST/JSON
- data validation

*All resources handled as LDP Basic Container support [faceted search](../references/faceted-search.md), sorting and pagination out of the box.*



## Modelling RDF Resources

Linked data models are defined with a [SHACL](https://www.w3.org/TR/shacl/)-based [specification language](../references/spec-language.md), assembling building blocks on an interactive drag-and-drop canvas.

<p class="note">Direct import of of SHACL specs is planned.</p>

Let's start defining a barebone port model stating that all resources of class `:Employee` are to be included as container items, exposing only the `rdfs:label` property for each item.

![Edit shapes](linked-data-publishing.mdimages/edit-shapes.png)

![Create port](linked-data-publishing.mdimages/create-port.png)

The relevant [building blocks](../references/spec-language.md#shapes) are selected from the side palette and dragged to the model canvas. Blocks may be rearranged by dragging them around on the canvas or removed by dragging them out of the canvas.

Resource IRIs may be entered as:

- an absolute IRI (e.g. `https://demo.metreeca.com/product-lines/`);
- a server-relative IRI (e.g. `/product-lines/`);
- a qualified name (e.g. `demo:Office`).

RDF values required by other constraint blocks are entered using Turtle syntax for [IRIs](https://www.w3.org/TR/turtle/#sec-iri) and [literals](https://www.w3.org/TR/turtle/#literals). In this context, absolute and server-relative IRIs must by wrapped inside angle brackets (e.g. `</product-lines/>`). Multiple values are entered as comma-separated Turtle [object lists](https://www.w3.org/TR/turtle/#object-lists).



As soon as the new port is created, the system activates the required resource handlers and starts exposing read/write linked data REST APIs at the matching HTTP/S URLs, as specified by the port model.

Exposed containers and resources are immediately available for rapid [linked data development](linked-data-interaction.md) <u>and may be interactively inspected in the linked data navigaton interface.</u>



## Updating Ports

We'll now refine the initial barebone model, exposing more employee properties, like the internal code, forename and surname, and detailing properties roles and constraints.

![Edit port shape](linked-data-publishing.mdimages/edit-port-shape.png)

![Update port specs](linked-data-publishing.mdimages/update-port.png)

The extended model makes use of *occurence* and *value* constraints to state that `rdfs:label`, `:code`, `:forename` and `:surname` values are expected:

- to occur exactly once for each resource;
- to be RDF literals of `xsd:string` datatype;
- to possibly match a specific regular expression pattern.

## Parameterizing Models

The `verify` and `server` blocks in the extended model also introduce the central concept of *[parametric](../references/spec-language.md#parameters)* model.

The `verify` block states that nested constraints are to be used only for validating incoming data and not for selecting existing resources to be exposed as container items. Constraints like the `class`, defined outside the `verify` block, will be used both for selecting relevant resources and validating incoming data.

The `server` block states that nested properties are server-managed and will be considered only when retrieving or deleting resources, but won't be accepted as valid content on resource creation and updating.

In the most general form, models may be parameterized on for different [axes](../references/spec-language.md#parameters), using the wrapping building blocks available under the *Conditions* tabs of the modelling palette. Building blocks specified outside parametric sectios are unconditionally enabled.

## Controlling Resource Access

Parametric models support the definition of fine-grained access control rules and role-dependent read/write resource views.



- access control
- modify model
- add wrapper

## Cross-Linking Resources

We'll now extend the employee directory model, adding cross-links to employee supervisors and subordinates.

![Edit port shape](linked-data-publishing.mdimages/edit-port.png)

![Connect port](linked-data-publishing.mdimages/connect-port.png)

The `relate` blocks state that `rdfs:label`s for linked employees are to be retrieved along with employee properties, e.g. to drive visualisation, but they are to be disallowed or ignored on resource creation, updating and deletion.

The `shapes` block organizes nested blocks into a visualization box, as demonstrated in the final navigable employee directory.

![Inspect directory](linked-data-publishing.mdimages/inspect-directory.png)

![Navigate employee directory](linked-data-publishing.mdimages/navigate-directory.png)

## Post-Processing Updates

We'll now complete the employee directory model, adding post-processing scripts for updating server-managed properties after entries are created or modified.

![Edit port shape](linked-data-publishing.mdimages/edit-port.png)

![Connect port](linked-data-publishing.mdimages/script-port.png)

The *mutate* SPARQL Update post-processing script will update the server-managed `rdfs:label` property according to user-supplied `:forename`/`:surname` properties after an entry resource managed by the employee directory is created or modified.

```sparql
prefix : <terms#>
prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>

delete { ?this rdfs:label ?label_ }
insert { ?this rdfs:label ?label }
where {
    ?this rdfs:label ?label_.
    optional {?this :forename ?forename; :surname ?surname }
    bind (concat(?forename, " ", ?surname) as ?label)
}
```

SPARQL Update post-processing scripts are executed after the corresponding state-mutating HTTP method is successfully applied to the target resource, with the following bindings:

| variable | value                                    |
| -------- | ---------------------------------------- |
| `<base>` | the server base URL of the HTTP request  |
| `?this`  | the IRI of the targe resource either as derived from the HTTP request or as defined by the `Location` HTTP header after a POST request |

| script | HTTP method                              |
| ------ | ---------------------------------------- |
| create | POST                                     |
| update | PUT                                      |
| delete | DELETE                                   |
| mutate | POST/PUT/DELETE (all state-mutating methods) |

# Next Steps

- Interaction tutorial
- expore standard wrappers/handlers
  - eg SPARQL endpoint
