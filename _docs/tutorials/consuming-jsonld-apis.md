---
title:	        Interacting  with Model‑Driven Linked Data REST APIs
excerpt:        Hands-on guided tour of model-driven linked data REST APIs features
redirect_from: /tutorials/linked-data-interaction
---

This example-driven tutorial introduces the main client-facing features of the Metreeca/Link model-driven linked data framework. Basic familiarity with [linked data](https://www.w3.org/standards/semanticweb/data) concepts and [REST](https://en.wikipedia.org/wiki/Representational_state_transfer) APIs is required.

In the following sections you will learn how to interact with REST APIs published with the framework, leveraging automatic model-driven fine-grained role‑based read/write access control,  faceted search and incoming data validation.

In the tutorial we will work with the linked data server developed in the  [publishing tutorial](publishing-ldp-apis), using a semantic version of the [BIRT](http://www.eclipse.org/birt/phoenix/db/) sample dataset, cross-linked to [GeoNames](http://www.geonames.org/) entities for cities and countries. The BIRT sample is a typical business database, containing tables such as *offices*, *customers*, *products*, *orders*, *order lines*, … for *Classic Models*, a fictional world-wide retailer of scale toy models. Before moving on you may want to familiarize yourself with it walking through the [search and analysis tutorial](https://metreeca.github.io/self/tutorials/search-and-analysis/) of the [Metreeca/Self](https://github.com/metreeca/self) self-service linked data search and analysis tool, which works on the same data.

We will walk through the REST API interaction process focusing on the task of consuming the [Product](https://demo.metreeca.com/apps/self/#endpoint=https://demo.metreeca.com/sparql&collection=https://demo.metreeca.com/terms#Product) catalog.

You may try out the examples using your favorite API testing tool or working from the command line with toos like `curl` or `wget`.

A Maven project with the code for the complete demo app is available on [GitHub](https://github.com/metreeca/demo): clone or [download](https://github.com/metreeca/demo/archive/master.zip) it to your workspace, open in your favorite IDE and launch a local instance of the server. If you are working with IntelliJ IDEA you may want to use the `Demo` pre-configured run configuration to deploy and update the local server instance.

# Model-Driven APIs

The demo linked data server is pre-configured with a small collection of read/write REST APIs able to drive a typical web-based interface like a user-facing [product catalog](https://demo.metreeca.com/apps/shop/).

<p class="warning">The product catalog demo is hosted on a cloud service: it is not expected to provide production-level performance and may experience some delays during workspace initialization.</p>

| REST API                                 | Contents                     |
| :--------------------------------------- | :--------------------------- |
| [/product-lines/](https://demo.metreeca.com/product-lines/) | Product line faceted catalog |
| [/product-lines/*](https://demo.metreeca.com/product-lines/classic-cars) | Product line details         |
| [/products/](https://demo.metreeca.com/products/) | Product faceted catalog      |
| [/products/*](https://demo.metreeca.com/products/S18_3140) | Product sheets               |

Even a simple application like this would usually require extensive back-end development activities in order to connect to the database and perform coordinated custom queries supporting data retrieval, faceted search, facet population, sorting, pagination and so on. User authorization, validation of updates and enforcing of consistency rules would as well require lenghty and error-prone custom back-end development.

Metreeca/Link automates the whole process with a model-driven API engine that compiles high‑level declarative linked data models into read/write REST APIs immediately available for front-end app development, supporting all of the above-mentioned features without custom back-end coding.

You may learn how to publish your own model-driven linked data APIs walking through the [linked data publishing tutorial](publishing-ldp-apis).

# Read Operations

Linked data REST APIs published by Metreeca/Link API engine support controlled read access to  RDF contents managed by the underlying graph storage layer.

User authorization and user-specific content generation are performed according to [role‑based](../references/spec-language#parameters) rules integrated in the linked data model driving the API publishing process.

## Resources

To retrieve the description of a published resource, as specified by the associated data model, just perform a `GET` operation on the URL identifying the resource.

```sh
% curl --include \
	"http://localhost:8080/products/S18_3140"
    
HTTP/1.1 200 
Content-Type: application/json;charset=UTF-8

{
    "id": "/products/S18_3140",
    "type": "/terms#Product",
    "label": "1903 Ford Model A",
    "comment": "Features opening trunk,  working steering system",
    "code": "S18_3140",
    "line": {
        "id": "/product-lines/vintage-cars",
        "label": "Vintage Cars"
    },
    "scale": "1:18",
    "vendor": "Unimax Art Galleries",
    "stock": 3913,
    "price": 136.59
}
```

Metreeca/Link produces and accepts an idiomatic [compacted/framed](../references/jsonld-format) JSON-LD format, which streamlines resource descriptions taking into account the constraints described in the associated linked data models.

To include context information, specify the `application/ld+json` MIME type in the `Accept` HTTP request header.

```sh
% curl --include \
	--header "Accept: application/ld+json" \
	"http://localhost:8080/products/S18_3140"
	
HTTP/1.1 200 
Content-Type: application/ld+json;charset=UTF-8


{
    "id": "/products/S18_3140",
   
   ⋮
   
    "@context": {
        "id": "@id",
        "code": "https://demo.metreeca.com/terms#code",
        "stock": "https://demo.metreeca.com/terms#stock",
        "type": "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
        "label": "http://www.w3.org/2000/01/rdf-schema#label",
        "comment": "http://www.w3.org/2000/01/rdf-schema#comment",
        "line": "https://demo.metreeca.com/terms#line",
        "scale": "https://demo.metreeca.com/terms#scale",
        "vendor": "https://demo.metreeca.com/terms#vendor",
        "price": "https://demo.metreeca.com/terms#sell"
    }
}
```

Retrieved data is automatically trimmed to the allowed envelope specified in the linked data model driving the target REST API for the [roles](../javadocs/com/metreeca/rest/Request.html#roles--) enabled for the current request user. Reserved properties are included only if the request is properly authenticated.

```sh
% curl --include \
	--header "Authorization: Bearer secret" \
	"http://localhost:8080/products/S18_3140"
    
HTTP/1.1 200  
Content-Type: application/json;charset=UTF-8

{
    "id": "/products/S18_3140",
    
    ⋮
    
    "stock": 3913,
    "price": 136.59,
    "buy": 68.3 # << buy price included only if authorized
}
```

## Collections

To retrieve the description of a published collections, as specified by the associated data model, perform a `GET` operation on the URL identifying the collection.

```sh
% curl --include \
	"http://localhost:8080/products/"
    
HTTP/1.1 200 
Content-Type: application/json;charset=UTF-8

{
    "id": "/products/",
    "contains": [
        {
            "id": "/products/S10_1678",
            "type": "/terms#Product",
            "label": "1969 Harley Davidson Ultimate Chopper",
            "comment": "This replica features working kickstand, front suspension,…",
            "code": "S10_1678",
            "line": {
                "id": "/product-lines/motorcycles",
                "label": "Motorcycles"
            },
            "scale": "1:10",
            "vendor": "Min Lin Diecast",
            "stock": 7933,
            "price": 95.7
        },

		⋮
		
	]
}
```

By default, collections descriptions include a digest description of each collection item, but a concise description of the collection itself may be retrieved using the standard LDP `Prefer` HTTP request header.

```sh
% curl --include \
    --header 'Prefer: return=representation; include="http://www.w3.org/ns/ldp#PreferMinimalContainer"' \
    "http://localhost:8080/products/"
    
HTTP/1.1 200 
Preference-Applied: return=representation; include="http://www.w3.org/ns/ldp#PreferMinimalContainer"
Content-Type: application/json;charset=UTF-8

{
    "id": "/products/",
}
```

# Write Operations

Linked data REST APIs published by Metreeca/Link API engine support controlled write access to contents managed by the underlying graph storage layer.

User authorization and user-specific content validation are performed according to [role‑based](../references/spec-language#parameters) rules integrated in the linked data model driving the API publishing process.

## Creating Resources

New resources are created by submitting an description to the REST API of a writable collection using the `POST` HTTP method.

Note that property values that may be inferred from the associated linked data model, like `rdf:type`, may be safely omitted.

```sh
% curl --include --request POST \
    --header 'Authorization: Bearer secret' \
    --header 'Content-Type: application/json' \
    "http://localhost:8080/products/" \
    --data @- <<EOF
{
	"type": "/terms#Product",
	"label": "Piaggio Vespa",
	"comment": "The iconic Piaggio's scooter…",
	"scale": "1:10",
	"vendor": "Autoart Studio Design",
	"buy": 101.0,
	"price": 123.0,
	"line": "/product-lines/motorcycles"
}
EOF

HTTP/2 201 Created
Location: /products/S10_6
```

Note that the `line` property is included in a shorthand form, as it is inferred to be a resource IRI from the associated linked data model.

The newly created resource is immediately available for retrieval at the URL returned in the `Location` HTTP response header.

Submitted data is automatically validated against the constraints specified in the linked data model driving the target REST API. Submiting, for instance, out of range price data would return an error and a structured error report.

```sh
% curl --include --request POST \
    --header 'Authorization: Bearer secret' \
    --header 'Content-Type: application/json' \
  "http://localhost:8080/products/" \
    --data @- <<EOF
{
	"type": "/terms#Product",
	"label": "Piaggio Vespa",
	"comment": "The iconic Piaggio's scooter…",
	"scale": "1:10",
	"vendor": "Autoart Studio Design",
	"buy": -101.0,
	"price": 9999.0,
	"line": "/product-lines/motorcycles"
}
EOF

HTTP/1.1 422 Unprocessable Entity
Location: http://localhost:8080/products/
Content-Type: application/json;charset=UTF-8

{
    "https://demo.metreeca.com/terms#buy": {
        "": {
            "minInclusive(\"0.0\"^^<http://www.w3.org/2001/XMLSchema#decimal>)": [
                "-101.0"
            ]
        }
    },
    "https://demo.metreeca.com/terms#sell": {
        "": {
            "maxExclusive(\"1000.0\"^^<http://www.w3.org/2001/XMLSchema#decimal>)": [
                "9999.0"
            ]
        }
    }
}
```

Submitted data is automatically matched against the allowed envelope specified in the linked data model driving the target REST API for the [roles](../javadocs/com/metreeca/rest/Request.html#roles--) enabled for the current request user. Submiting, for instance, buy price data without valid authorization headers would return an error.

```sh
% curl --include --request POST \
    --header 'Content-Type: application/json' \
   "http://localhost:8080/products/" \
    --data @- <<EOF
{
	"type": "/terms#Product",
    "label": "Piaggio Ciao",
    "comment" : "The sturdy Piaggio's velo bike…",
    "scale": "1:10",
    "vendor": "Autoart Studio Design",
    "buy": 87.0,
    "price": 99.0,
    "line": "/product-lines/motorcycles" 
}
EOF

HTTP/1.1 401 Unauthorized
```

## Updating Resources

Existing writable RDF resources are updated by submitting an RDF description to their REST API using the `PUT` HTTP method.

Note that  server-managed properties like `demo:code` and `demo:stock` are omitted, as they are inherited from the existing description.

```sh
% curl --include --request PUT \
	--header 'Authorization: Bearer secret' \
	--header 'Content-Type: application/json' \
	"http://localhost:8080/products/S18_3140" \
    --data @- <<EOF
{
   	"type": "/terms#Product",
    "label": "1903 Ford Model A",
    "comment": "Features opening trunk, working steering system",
    "line": "/product-lines/vintage-cars",
    "scale": "1:18",
    "vendor": "Unimax Art Galleries",
    "buy": 50.00,
    "price": 130.00
}
EOF

HTTP/2 204 No Content
```

The updated resource is immediately available for retrieval at the existing URL.

As in the case of resource creation, submitted data is automatically validated against constraints and roles specified in the linked data model driving the target REST API.

## Deleting Resources

Existing writable RDF resources are deleted using the `DELETE` HTTP method on their REST API.

```sh
% curl --include --request DELETE \
	--header 'Authorization: Bearer secret' \
	"http://localhost:8080/products/S18_3140"

HTTP/2 204 No Content
```

The deleted resource is immediately no longer available for retrieval.

# Faceted Search

Metreeca/Link REST APIs engine supports model-driven faceted search and related facet-related queries without additional effort.

To retrieve a digest description of collection items matching a set of facet filters, perform a `GET` operation on the URL identifying the collection, appending a URL-encoded JSON query object [describing the filters](../references/faceted-search) to be applied.

```json
{	
  ">= price" : 100, 
  "vendor": "Classic Metal Creations"
}
```

```sh
% curl --include \
    'http://localhost:8080/products/?%3E%3D+price=100&vendor=Classic+Metal+Creations'
    
HTTP/1.1 200 
Content-Type: application/json;charset=UTF-8

{
    "id": "/products/",
    "contains": [
        {
            "id": "/products/S10_1949",
            "type": "/terms#Product",
            "label": "1952 Alpine Renault 1300",
            "comment": "Turnable front wheels; steering function; detailed interior; …",
            "code": "S10_1949",
            "line": {
                "id": "/product-lines/classic-cars",
                "label": "Classic Cars"
            },
            "scale": "1:10",
            "vendor": "Classic Metal Creations",
            "stock": 7305,
            "price": 214.3
        },
      
      ⋮
      
    ]
}
```

Note that collection descriptions are omitted from faceted search results.

## Sorting and Pagination

Faceted search results may be sorted and paginated including [sorting criteria](../references/faceted-search#edges-query) and [pagination limits](../references/faceted-search#edges-query) in the JSON query object.

```json
{
  
  ">= price" : 100, 
  "vendor": "Classic Metal Creations",

  "_order":"-price",
  "_offset":0,
  "_limit":10
  
}
```

## Facet Stats and Options

The faceted search engine supports also introspection queries for retrieving [facet stats](../references/faceted-search#stats-query)  and available [facet options](../references/faceted-search#items-query).

To retrieve datatype, count and range stats for a facet, taking into account applied filters, specify the target property path in the faceted search query object.

```json
{
    
	"_stats": "price"

	">= price" : 100, 
	"vendor": "Classic Metal Creations",
  
}
```

```sh
% curl --include \
    'http://localhost:8080/products/?%3E%3D+price=100&vendor=Classic+Metal+Creations&_stats=price'

HTTP/2 200 OK
Content-Type: application/json

{
    "id": "/products",
    "count": 10,
    "min": 44.8,
    "max": 214.3,
    "stats": [
        {
            "id": "http://www.w3.org/2001/XMLSchema#decimal",
            "count": 10,
            "min": 44.8,
            "max": 214.3
        }
    ]
}
```

To list available options and counts for a facet, taking into account applied filters, specify the target property path in the faceted search query object.

```json
{
	
    "_terms": "line"
    
  	">= price" : 100, 
  	"vendor": "Classic Metal Creations",
  
}
```

```sh
% curl --include \
    'http://localhost:8080/products/?%3E%3D+price=100&vendor=Classic+Metal+Creations&_terms=line'

HTTP/2 200 OK
Content-Type: application/json

{
    "id": "/products/",
    "terms": [
        {
            "value": {
                "id": "/product-lines/classic-cars",
                "label": "Classic Cars"
            },
            "count": 4
        },
        {
            "value": {
                "id": "/product-lines/planes",
                "label": "Planes"
            },
            "count": 1
        }
    ]
}
```

Labels and comments for the selected options are also retrieved to support facet visualization.
