---
title:    Consuming Model‑Driven REST/JSON-LD APIs
---

[comment]: <> (excerpt:  Hands-on guided tour of model-driven lREST/JSON-LD APIs features)


This example-driven tutorial introduces the main client-facing features of the Metreeca/Link model-driven REST/JSON
framework. Basic familiarity with [linked data](https://www.w3.org/standards/semanticweb/data) concepts
and [REST](https://en.wikipedia.org/wiki/Representational_state_transfer) APIs is required.

In the following sections you will learn how to consume REST APIs published with the framework, leveraging automatic
model-driven faceted search, data validation and fine‑grained role‑based access control.

In the tutorial we will work with the linked data server developed in
the  [publishing tutorial](publishing-jsonld-apis.md), using a linked data version of
the [BIRT](http://www.eclipse.org/birt/phoenix/db/) sample dataset, cross-linked to [GeoNames](http://www.geonames.org/)
entities for cities and countries.

The BIRT sample is a typical business database, containing tables such as *offices*, *customers*, *products*, *orders*, *
order lines*, … for *Classic Models*, a fictional world-wide retailer of scale toy models: we will walk through the REST
API interaction process focusing on the task of consuming
the [Product](https://demo.metreeca.com/self/#endpoint=https://demo.metreeca.com/toys/sparql&collection=https://demo.metreeca.com/toys/terms#Product)
catalog.

You may try out the examples using your favorite API testing tool or working from the command line with toos like `curl`
or `wget`.

A Maven project with the code for the complete sample app is available
on [GitHub](https://github.com/metreeca/link/tree/main/metreeca-toys): [download](https://downgit.github.io/#/home?
url=https://github.com/metreeca/link/tree/main/metreeca-toys&fileName=metreeca%E2%A7%B8link%20sample)
it to your workspace, open in your favorite IDE, compile and launch a local instance of the server.

# Model-Driven APIs

The sample linked data server is pre-configured with a small collection of read/write REST APIs able to drive a typical
web-based interface like a user-facing [product catalog](https://demo.metreeca.com/toys/).

| REST API                                 | Contents                     |
| :--------------------------------------- | :--------------------------- |
| [/product-lines/](https://demo.metreeca.com/toys/product-lines/) | Product line faceted catalog |
| [/product-lines/*](https://demo.metreeca.com/toys/product-lines/classic-cars) | Product line details         |
| [/products/](https://demo.metreeca.com/toys/products/) | Product faceted catalog      |
| [/products/*](https://demo.metreeca.com/toys/products/S18_3140) | Product sheets               |

!!! warning "Sample Performance"
Linked samples are hosted on a cloud service, are not expected to provide production-level performance and may experience
some delays during workspace initialization.

Even a simple application like this would usually require extensive back-end development activities in order to connect
to the database and perform coordinated custom queries supporting data retrieval, faceted search, facet population,
sorting, pagination and so on. User authorization, validation of updates and enforcing of consistency rules would as well
require lenghty and error-prone custom back-end development.

Metreeca/Link automates the whole process with a model-driven API engine that compiles high‑level declarative linked data
models into read/write REST APIs immediately available for front-end app development, supporting all of the
above-mentioned features without custom back-end coding.

You may learn how to publish your own model-driven linked data APIs walking through
the [linked data publishing tutorial](publishing-jsonld-apis.md).

# Read Operations

Linked data REST APIs published by Metreeca/Link API engine support controlled read access to RDF contents managed by the
underlying graph storage layer.

User authorization and user-specific content generation are performed according
to [role‑based](../references/spec-language.md#parameters) rules integrated in the linked data model driving the API
publishing process.

## Resources

To retrieve the description of a published resource, as specified by the associated data model, just perform a `GET`
operation on the URL identifying the resource.

```shell
% curl --include "http://localhost:8080/products/S18_3140"
    
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

Metreeca/Link produces and accepts an idiomatic [compacted/framed](../references/jsonld-format.md) JSON-LD format, which
streamlines resource descriptions taking into account the constraints described in the associated linked data models.

To include context information, specify the `application/ld+json` MIME type in the `Accept` HTTP request header.

```shell
% curl --include "http://localhost:8080/products/S18_3140" \
	--header "Accept: application/ld+json" 
	
HTTP/1.1 200 
Content-Type: application/ld+json;charset=UTF-8


{
    "id": "/products/S18_3140",
   
   ⋮
   
    "@context": {
        "id": "@id",
        "type": {
            "@id": "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
            "@type": "@id"
        },
        "label": {
            "@id": "http://www.w3.org/2000/01/rdf-schema#label",
            "@type": "http://www.w3.org/2001/XMLSchema#string"
        },

		⋮
		
		"stock": {
            "@id": "https://demo.metreeca.com/toys/terms#stock",
            "@type": "http://www.w3.org/2001/XMLSchema#integer"
        }
    }
}
```

Retrieved data is automatically trimmed to the allowed envelope specified in the linked data model driving the target
REST API for the [roles](../javadocs/com/metreeca/rest/Request.html#roles--) enabled for the current request user.
Reserved properties are included only if the request is properly authenticated.

```shell
% curl --include "http://localhost:8080/products/S18_3140" \
	--header "Authorization: Bearer secret"
    
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

To retrieve the description of a published collections, as specified by the associated data model, perform a `GET`
operation on the URL identifying the collection.

```shell
% curl --include "http://localhost:8080/products/"
    
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

# Write Operations

Linked data REST APIs published by Metreeca/Link API engine support controlled write access to content managed by the
underlying graph storage layer.

User authorization and user-specific content validation are performed according
to [role‑based](../references/spec-language.md#parameters) rules integrated in the linked data model driving the API
publishing process.

## Creating Resources

New resources are created by submitting an description to the REST API of a writable collection using the `POST` HTTP
method.

Note that property values that may be inferred from the associated linked data model, like `rdf:type`, may be safely
omitted.

```shell
% curl --include --request POST "http://localhost:8080/products/" \
    --header 'Authorization: Bearer secret' \
    --header 'Content-Type: application/json' \
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

Note that the `line` property is included in a shorthand form, as it is inferred to be a resource IRI from the associated
linked data model.

The newly created resource is immediately available for retrieval at the URL returned in the `Location` HTTP response
header.

Submitted data is automatically validated against the constraints specified in the linked data model driving the target
REST API. Submiting, for instance, out of range price data would return an error and a structured error report.

```shell
% curl --include --request POST "http://localhost:8080/products/" \
    --header 'Authorization: Bearer secret' \
    --header 'Content-Type: application/json' \
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
    "https://example.com/terms#buy": {
        "@errors": [
            "-101.0 is not greater than or equal to 0.0"
        ]
    },
    "https://example.com/terms#sell": {
        "@errors": [
            "9999.0 is not strictly less than 1000.0"
        ]
    }
} 
```

Submitted data is automatically matched against the allowed envelope specified in the linked data model driving the
target REST API for the [roles](../javadocs/com/metreeca/rest/Request.html#roles--) enabled for the current request user.
Submiting, for instance, buy price data without valid authorization headers would return an error.

```shell
% curl --include --request POST "http://localhost:8080/products/" \
    --header 'Content-Type: application/json' \
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
WWW-Authenticate: Bearer realm="https://example.com/"
```

## Updating Resources

Existing writable RDF resources are updated by submitting an RDF description to their REST API using the `PUT` HTTP
method.

Note that server-managed properties like `demo:code` and `demo:stock` are omitted, as they are inherited from the
existing description.

```shell
% curl --include --request PUT "http://localhost:8080/products/S18_3140" \
	--header 'Authorization: Bearer secret' \
	--header 'Content-Type: application/json' \
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

As in the case of resource creation, submitted data is automatically validated against constraints and roles specified in
the linked data model driving the target REST API.

## Deleting Resources

Existing writable RDF resources are deleted using the `DELETE` HTTP method on their REST API.

```shell
% curl --include --request DELETE "http://localhost:8080/products/S18_3140" \
	--header 'Authorization: Bearer secret'

HTTP/2 204 No Content
```

The deleted resource is immediately no longer available for retrieval.

# Faceted Search

Metreeca/Link REST APIs engine supports model-driven faceted search and related facet-related queries without additional
effort.

To retrieve a digest description of collection items matching a set of facet filters, perform a `GET` operation on the
URL identifying the collection, appending a URL-encoded JSON query
object [describing the filters](../references/faceted-search.md) to be applied.

```json
{
	">= price": 100,
	"vendor": "Classic Metal Creations"
}
```

```shell
% curl --include 'http://localhost:8080/products/?%3E%3D+price=100&vendor=Classic+Metal+Creations'
    
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

Faceted search results may be sorted and paginated
including [sorting criteria](../references/faceted-search.md#items-query)
and [pagination limits](../references/faceted-search.md#items-query) in the JSON query object.

```json
{
	">= price": 100,
	"vendor": "Classic Metal Creations",
	".order": "-price",
	".offset": 0,
	".limit": 10
}
```

## Facet Stats and Options

The faceted search engine supports also introspection queries for
retrieving [facet stats](../references/faceted-search.md#stats-query)  and
available [facet options](../references/faceted-search.md#items-query).

To retrieve datatype, count and range stats for a facet, taking into account applied filters, specify the target property
path in the faceted search query object.

```json
{
	".stats": "price",
	">= price": 100,
	"vendor": "Classic Metal Creations"
}
```

```shell
% curl --include 'http://localhost:8080/products/?%3E%3D+price=100&vendor=Classic+Metal+Creations&.stats=price'

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

To list available options and counts for a facet, taking into account applied filters, specify the target property path
in the faceted search query object.

```json
{
	".terms": "line",
	">= price": 100,
	"vendor": "Classic Metal Creations"
}
```

```shell
% curl --include 'http://localhost:8080/products/?%3E%3D+price=100&vendor=Classic+Metal+Creations&.terms=line'

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

# Localization

Multi-lingual content retrieval is fully supported,
with [optional](publishing-jsonld-apis.md#localization) [compact rendering](https://www.w3.org/TR/json-ld11/#language-indexing)
.

Retrieved localizations may be limited to a predefined set of language tags at query time using the `Accept-Language`
HTTP header, like for instance:

```http request
GET http://localhost:8080/products/S18_3140
Accept-Language: en, it, de
```