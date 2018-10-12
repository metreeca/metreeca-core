---
title:	    Interacting with Model‑Driven Linked Data REST APIs
excerpt:    Hands-on guided tour of model-driven linked data REST APIs features
---

This example-driven tutorial introduces the main client-facing features of the Metreeca/Link model-driven linked data framework. Basic familiarity with [linked data](https://www.w3.org/standards/semanticweb/data) concepts and [REST](https://en.wikipedia.org/wiki/Representational_state_transfer) APIs is required.

In the following sections you will learn how to interact with REST APIs published with the framework, leveraging automatic model-driven fine-grained role‑based read/write access control,  faceted search and incoming data validation.

In the tutorial we will work with the linked data server developed in the  [publishing tutorial](linked-data-publishing.md), using a semantic version of the [BIRT](http://www.eclipse.org/birt/phoenix/db/) sample dataset, cross-linked to [GeoNames](http://www.geonames.org/) entities for cities and countries. The BIRT sample is a typical business database, containing tables such as *offices*, *customers*, *products*, *orders*, *order lines*, … for *Classic Models*, a fictional world-wide retailer of scale toy models. Before moving on you may want to familiarize yourself with it walking through the [search and analysis tutorial](https://metreeca.github.io/self/tutorials/search-and-analysis/) of the [Metreeca/Self](https://github.com/metreeca/self) self-service linked data search and analysis tool, which works on the same data.

We will walk through the REST API interaction process focusing on the task of consuming the [Product](https://demo.metreeca.com/apps/self/#endpoint=https://demo.metreeca.com/sparql&collection=https://demo.metreeca.com/terms#Product) catalog as a [Linked Data Platform](https://www.w3.org/TR/ldp-primer/) (LDP) Basic Container.

You may try out the examples using your favorite API testing tool or working from the command line with toos like `curl` or `wget`.

A Maven project with the code for the complete demo app is available on [GitHub](https://github.com/metreeca/demo/tree/tutorial): clone or [download](https://github.com/metreeca/demo/archive/tutorial.zip) it to your workspace, open in your favorite IDE and launch a local instance of the server. If you are working with IntelliJ IDEA you may want to use the `Demo` pre-configured run configuration to deploy and update the local server instance.

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

You may learn how to publish your own model-driven linked data APIs walking through the [linked data publishing tutorial](linked-data-publishing.md).

# Read Operations

Linked data REST APIs published by Metreeca/Link API engine support controlled read access to  RDF contents managed by the underlying graph storage layer.

User authorization and user-specific content generation are performed according to [role‑based](../references/spec-language.md#parameters) rules integrated in the linked data model driving the API publishing process.

## RDF Resources

RDF resources managed by the underlying graph storage are exposed by the Metreeca REST API engine as [Linked Data Platform (LDP) RDF Sources](https://www.w3.org/TR/ldp/#ldprs).

To retrieve the RDF description of a published resource, as specified by the associated data model, just perform a `GET` operation on the URL identifying the resource.

```sh
% curl --include "http://localhost:8080/products/S18_3140"

HTTP/1.1 200 
Vary: Accept
Link: <http://www.w3.org/ns/ldp#Resource>; rel="type"
Link: <http://www.w3.org/ns/ldp#RDFSource>; rel="type"
Link: <http://localhost:8080/products/S18_3140?specs>;
		rel=http://www.w3.org/ns/ldp#constrainedBy
Content-Type: text/turtle;charset=UTF-8

<http://localhost:8080/products/S18_3140> a <http://localhost:8080/terms#Product>;
  <http://localhost:8080/terms#code> "S18_3140";
  <http://localhost:8080/terms#line> <http://localhost:8080/product-lines/vintage-cars>;
  <http://localhost:8080/terms#scale> "1:18";
  <http://localhost:8080/terms#sell> 136.59;
  <http://localhost:8080/terms#stock> 3913;
  <http://localhost:8080/terms#vendor> "Unimax Art Galleries";
  <http://www.w3.org/2000/01/rdf-schema#comment> "Features opening trunk,  working steering system";
  <http://www.w3.org/2000/01/rdf-schema#label> "1903 Ford Model A" .

<http://localhost:8080/product-lines/vintage-cars> <http://www.w3.org/2000/01/rdf-schema#label> "Vintage Cars" .
```

Standard content negotiation is supported, so you may ask for resource descriptions in a suitable RDF concrete syntax ([Turtle](https://www.w3.org/TR/turtle/), [N-Triples](https://www.w3.org/TR/n-triples/), [RDF/XML](https://www.w3.org/TR/rdf-syntax-grammar/), …) specifying the associated MIME type in the `Accept` HTTP request header.

```sh
% curl --include --header 'Accept: application/rdf+xml' \
    "http://localhost:8080/products/S18_3140"

HTTP/1.1 200 
Vary: Accept
Link: <http://www.w3.org/ns/ldp#Resource>; rel="type"
Link: <http://www.w3.org/ns/ldp#RDFSource>; rel="type"
Link: <http://localhost:8080/products/S18_3140?specs>;
		rel=http://www.w3.org/ns/ldp#constrainedBy
Content-Type: application/rdf+xml

<?xml version="1.0" encoding="UTF-8"?>
<rdf:RDF
        xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">

<rdf:Description rdf:about="http://localhost:8080/products/S18_3140">
        <rdf:type rdf:resource="http://localhost:8080/terms#Product"/>
        <label xmlns="http://www.w3.org/2000/01/rdf-schema#">1903 Ford Model A</label>
        <comment xmlns="http://www.w3.org/2000/01/rdf-schema#">Features opening trunk,  working steering system</comment>
        <code xmlns="http://localhost:8080/terms#">S18_3140</code>
        <line xmlns="http://localhost:8080/terms#" rdf:resource="http://localhost:8080/product-lines/vintage-cars"/>
</rdf:Description>

<rdf:Description rdf:about="http://localhost:8080/product-lines/vintage-cars">
        <label xmlns="http://www.w3.org/2000/01/rdf-schema#">Vintage Cars</label>
</rdf:Description>

<rdf:Description rdf:about="http://localhost:8080/products/S18_3140">
        <scale xmlns="http://localhost:8080/terms#">1:18</scale>
        <vendor xmlns="http://localhost:8080/terms#">Unimax Art Galleries</vendor>
        <stock xmlns="http://localhost:8080/terms#" rdf:datatype="http://www.w3.org/2001/XMLSchema#integer">3913</stock>
        <sell xmlns="http://localhost:8080/terms#" rdf:datatype="http://www.w3.org/2001/XMLSchema#decimal">136.59</sell>
</rdf:Description>

</rdf:RDF>   
```

JSON-based formats are especially convenient for front-end development: beside the standardised  [JSON-LD](https://www.w3.org/TR/json-ld/) RDF serialisation, Metreeca supports a simpler [idiomatic](../references/idiomatic-json.md) JSON-based format, which streamlines resource descriptions taking into account the constraints described in the associated linked data models.

To ask for resource descriptions in the idiomatic JSON format, specify the `application/json` MIME type in the `Accept` HTTP request header.

```sh
% curl --include --header 'Accept: application/json' \
    "http://localhost:8080/products/S18_3140"
    
HTTP/1.1 200 
Vary: Accept
Link: <http://www.w3.org/ns/ldp#Resource>; rel="type"
Link: <http://www.w3.org/ns/ldp#RDFSource>; rel="type"
Link: <http://localhost:8080/products/S18_3140?specs>;
		rel=http://www.w3.org/ns/ldp#constrainedBy
Content-Type: application/json;charset=UTF-8

{
    "this": "http://localhost:8080/products/S18_3140",
    "type": "http://localhost:8080/terms#Product",
    "label": "1903 Ford Model A",
    "comment": "Features opening trunk,  working steering system",
    "code": "S18_3140",
    "line": {
        "this": "http://localhost:8080/product-lines/vintage-cars",
        "label": "Vintage Cars"
    },
    "scale": "1:18",
    "vendor": "Unimax Art Galleries",
    "stock": 3913,
    "price": 136.59
}
```

If available, the linked data model associated with a resource can be [retrieved and inspected](../references/spec-language.html#rdf-encoding) from the URL provided in the `Link rel="ldp:constrainedBy"`HTTP response header. The information provided by the associated model could be used, for instance, to optimize or dynamically build user interfaces or to automaticaly provide client-side validation on data forms.

Retrieved data is automatically trimmed to the allowed envelope specified in the linked data model driving the target REST API for the [roles](../javadocs/com/metreeca/rest/Request.html#roles--) enabled for the current request user. Reserved properties are included only if the request is properly authenticated.

```sh
% curl --include --header 'Accept: application/json' \
	--header 'Authorization: Bearer secret' \
    "http://localhost:8080/products/S18_3140"
    
HTTP/1.1 200  
Content-Type: application/json;charset=UTF-8

{
    "this": "http://localhost:8080/products/S18_3140",
    
    ⋮
    
	"stock": 3913,
    "price": 136.59,
    "buy": 68.3 # << buy price included only if authorized
}
```

## RDF Collections

RDF resource collections managed by the underlying graph storage are exposed by the Metreeca/Link REST API engine as [Linked Data Platform (LDP) Basic Containers](https://www.w3.org/TR/ldp/#ldpc).

To retrieve the RDF description of a published collections, as specified by the associated data model, perform a `GET` operation on the URL identifying the collection.

```sh
% curl --include --header 'Accept: application/json' \
    "http://localhost:8080/products/"
    
HTTP/1.1 200 
Vary: Accept
Vary: Prefer
Link: <http://www.w3.org/ns/ldp#Container>; rel="type"
Link: <http://www.w3.org/ns/ldp#BasicContainer>; rel="type"
Link: <http://www.w3.org/ns/ldp#Resource>; rel="type"
Link: <http://www.w3.org/ns/ldp#RDFSource>; rel="type"
Link: <http://localhost:8080/products/?specs>;
		rel=http://www.w3.org/ns/ldp#constrainedBy
Content-Type: application/json;charset=UTF-8

{
    "this": "http://localhost:8080/products/",
    "contains": [
        {
            "this": "http://localhost:8080/products/S10_1678",
            "type": "http://localhost:8080/terms#Product",
            "label": "1969 Harley Davidson Ultimate Chopper",
            "comment": "This replica features working kickstand, front suspension, gear-shift lever, footbrake lever, drive chain, wheels and steering. All parts are particularly delicate due to their precise scale and require special care and attention.",
            "code": "S10_1678",
            "line": {
                "this": "http://localhost:8080/product-lines/motorcycles",
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

By default, collection descriptions include a digest description of each collection item, but a concise description of the collection itself may be retrieved using the standard LDP `Prefer` HTTP request header.

```sh
% curl --include --header 'Accept: application/json' \
    --header 'Prefer: return=representation; include="http://www.w3.org/ns/ldp#PreferEmptyContainer"' \
    "http://localhost:8080/products/"
    
HTTP/1.1 200 
Vary: Accept
Vary: Prefer
Link: <http://www.w3.org/ns/ldp#Container>; rel="type"
Link: <http://www.w3.org/ns/ldp#BasicContainer>; rel="type"
Link: <http://www.w3.org/ns/ldp#Resource>; rel="type"
Link: <http://www.w3.org/ns/ldp#RDFSource>; rel="type"
Link: <http://localhost:8080/products/?specs>;
		rel=http://www.w3.org/ns/ldp#constrainedBy
Preference-Applied: return=representation;
		include="http://www.w3.org/ns/ldp#PreferEmptyContainer"
Content-Type: application/json;charset=UTF-8

{
    "this": "http://localhost:8080/products/",
    "label": "Products"
}
```

Again, if available, the linked data model associated with a collection can be retrieved and inspected from the URL provided in the `Link rel="ldp:constrainedBy"` HTTP response header.

# Write Operations

Linked data REST APIs published by Metreeca/Link API engine support controlled write access to  RDF contents managed by the underlying graph storage layer.

User authorization and user-specific content validation are performed according to [role‑based](../references/spec-language.md#parameters) rules integrated in the linked data model driving the API publishing process.

## Creating Resources

New RDF resources are create by submitting an RDF description to the REST API of a writable RDF collection using the `POST` HTTP method.

Standard content negotiation is supported, so you may submit resource descriptions in a suitable RDF concrete syntax ([Turtle](https://www.w3.org/TR/turtle/), [N-Triples](https://www.w3.org/TR/n-triples/), [RDF/XML](https://www.w3.org/TR/rdf-syntax-grammar/), …) specifying the associated MIME type in the `Content-Type` HTTP request header.

Note that property values that may be inferred from the associated linked data model, like `rdf:type`, may be safely omitted.

```sh
% curl --include --request POST \
    --header 'Authorization: Bearer secret' \
	--header 'Content-Type: text/turtle' \
    "http://localhost:8080/products/" \
    --data @- <<EOF
    
@prefix demo: <http://localhost:8080/terms#>.
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.

<>
    rdfs:label "Piaggio Vespa";
    rdfs:comment "The iconic Piaggio's scooter…";
    demo:scale "1:10";
    demo:vendor "Autoart Studio Design";
    demo:buy 101.0;
    demo:sell 123.0;
    demo:line <http://localhost:8080/product-lines/motorcycles>.
EOF

HTTP/2 201 Created
Location: http://localhost:8080/products/S10_6
```

The newly created resource is immediately available for retrieval at the URL returned in the `Location` HTTP response header.

The idiomatic model-driven JSON format is supported also for write operations, specifying the `application/json` MIME type in the `Content-Type` HTTP request header.

Note that the `line` property is included in a shorthand form, as it is inferred to be a resource IRI from the associated linked data model.

```sh
% curl --include --request POST \
    --header 'Authorization: Bearer secret' \
    --header 'Content-Type: application/json' \
    "http://localhost:8080/products/" \
    --data @- <<EOF
{
    "label": "Piaggio Ciao",
    "comment" : "The sturdy Piaggio's velo bike…",
    "scale": "1:10",
    "vendor": "Autoart Studio Design",
    "buy": 87.0,
    "price": 99.0,
    "line": "http://localhost:8080/product-lines/motorcycles" 
}
EOF

HTTP/2 201 Created
Location: https://demo.metreeca.com/products/S10_7
```

Submitted data is automatically validated against the constraints specified in the linked data model driving the target REST API. Submiting, for instance, out of range price data would return an error and a structured error report.

```sh
% curl --include --request POST \
    --header 'Authorization: Bearer secret' \
    --header 'Content-Type: text/turtle' \
    "http://localhost:8080/products/" \
    --data @- <<EOF
    
@prefix demo: <http://localhost:8080/terms#>.
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.

<>
    rdfs:label "Piaggio Vespa";
    rdfs:comment "The iconic Piaggio's scooter…";
    demo:scale "1:10";
    demo:vendor "Autoart Studio Design";
    demo:buy -101.0;
    demo:sell 9999.0;
    demo:line <http://localhost:8080/product-lines/motorcycles>.
    
EOF

HTTP/1.1 422 Unprocessable Entity
Location: http://localhost:8080/products/
Content-Type: application/json;charset=UTF-8

{
    "error": "data-invalid",
    "trace": {
        "<http://localhost:8080/products/>": {
            "<http://localhost:8080/terms#sell>": {
                "errors": [
                    {
                        "cause": "invalid value",
                        "shape": "maxExclusive(1000.0)",
                        "values": [
                            "9999.0"
                        ]
                    }
                ]
            },
            "<http://localhost:8080/terms#buy>": {
                "errors": [
                    {
                        "cause": "invalid value",
                        "shape": "minInclusive(0.0)",
                        "values": [
                            "-101.0"
                        ]
                    }
                ]
            }
        }
    }
}% 

```

Submitted data is automatically matched against the allowed envelope specified in the linked data model driving the target REST API for the [roles](../javadocs/com/metreeca/rest/Request.html#roles--) enabled for the current request user. Submiting, for instance, buy price data without valid authorization headers would return an error.

```sh
% curl --include --request POST \
    --header 'Content-Type: application/json' \
    "http://localhost:8080/products/" \
    --data @- <<EOF
{
    "label": "Piaggio Ciao",
    "comment" : "The sturdy Piaggio's velo bike…",
    "scale": "1:10",
    "vendor": "Autoart Studio Design",
    "buy": 87.0,
    "price": 99.0,
    "line": "http://localhost:8080/product-lines/motorcycles" 
}
EOF

HTTP/1.1 401 Unauthorized
```

## Updating Resource

Existing writable RDF resources are updated by submitting an RDF description to their REST API using the `PUT` HTTP method.

Standard content negotiation is as usual supported through the `Content-Type` HTTP request header, also for the idiomatic JSON format.

Note that  server-managed properties like `demo:code` and `demo:stock` are omitted, as they are inherited from the existing description.

```sh
% curl --include --request PUT \
    --header 'Authorization: Bearer secret' \
    --header 'Content-Type: application/json' \
    "http://localhost:8080/products/S18_3140" \
    --data @- <<EOF
{
    "label": "1903 Ford Model A",
    "comment": "Features opening trunk,  working steering system",
    "line": "http://localhost:8080/product-lines/vintage-cars",
    "scale": "1:18",
    "vendor": "Unimax Art Galleries",
    "buy": 68.3,
    "price": 136.59
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

The deleted resource is immediately no longer available for retrieval at the previous URL.

# Faceted Search

Metreeca/Link REST APIs engine extends [Linked Data Platform (LDP) Containers](https://www.w3.org/TR/ldp/#ldpc) with faceted search and supporting facet-related queries.

To retrieve a digest description of collection items matching a set of facet filters, perform a `GET` operation on the URL identifying the collection, appending a URL-encoded JSON query object [describing the filters](../references/faceted-search.md) to be applied.

```json
{	
  "filter": { 
    "price" : { ">=": 100 }, 
    "vendor": "Classic Metal Creations"
  }
}
```

```sh
% curl --include --header 'Accept: application/json' \
    'http://localhost:8080/products/?%7B%22filter%22%3A%7B%22price%22%3A%7B%22%3E%3D%22%3A100%7D%2C%22vendor%22%3A%22Classic%20Metal%20Creations%22%7D%7D'
    
HTTP/1.1 200 
Content-Type: application/json;charset=UTF-8

{
    "this": "http://localhost:8080/products/",
    "contains": [
        {
            "this": "http://localhost:8080/products/S10_1949",
            "type": "http://localhost:8080/terms#Product",
            "label": "1952 Alpine Renault 1300",
            "comment": "Turnable front wheels; steering function; detailed interior; detailed engine; opening hood; opening trunk; opening doors; and detailed chassis.",
            "code": "S10_1949",
            "line": {
                "this": "http://localhost:8080/product-lines/classic-cars",
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

Note that RDF container descriptions are omitted from faceted search results.

## Sorting and Pagination

Faceted search results may be sorted and paginated including [sorting criteria](../references/faceted-search.md#edges-query) and [pagination limits](../references/faceted-search.md#edges-query) in the JSON query object.

```json
{
  "filter": { 
    "price" : { ">=": 100 }, 
    "vendor": "Classic Metal Creations"
  },
  "order":"-price",
  "offset":0,
  "limit":10
}
```

## Facet Stats and Options

The faceted search engine supports also introspection queries for retrieving [facet stats](../references/faceted-search.md#stats-query)  and available [facet options](../references/faceted-search.md#items-query).

To retrieve datatype, count and range stats for a facet, taking into account applied filters, specify the target property path in the faceted search query object.

```json
{
    "stats": "price",    
    "filter": { 
        "vendor": "Classic Metal Creations"
    }
}
```

```sh
% curl --include --header 'Accept: application/json' \
    'http://localhost:8080/products/?%7B%0A%09%22stats%22%3A%20%22price%22%2C%09%0A%20%20%20%20%22filter%22%3A%20%7B%20%0A%20%20%20%20%20%20%20%20%22vendor%22%3A%20%22Classic%20Metal%20Creations%22%0A%20%20%20%20%7D%0A%7D'

HTTP/2 200 OK
Content-Type: application/json

{
    "this": "http://localhost:8080/products",
    "count": 10,
    "min": 44.8,
    "max": 214.3,
    "stats": [
        {
            "this": "http://www.w3.org/2001/XMLSchema#decimal",
            "count": 10,
            "min": 44.8,
            "max": 214.3
        }
    ]
}
```

To list available item options and counts for a facet, taking into account applied filters, specify the target property path in the faceted search query object.

```json
{
    "items": "line",    
    "filter": { 
        "vendor": "Classic Metal Creations"
    }
}
```

```sh
% curl --include --header 'Accept: application/json' \
    'http://localhost:8080/products/?%7B%0A%09%22items%22%3A%20%22line%22%2C%09%0A%20%20%20%20%22filter%22%3A%20%7B%20%0A%20%20%20%20%20%20%20%20%22vendor%22%3A%20%22Classic%20Metal%20Creations%22%0A%20%20%20%20%7D%0A%7D'

HTTP/2 200 OK
Content-Type: application/json

{
    "this": "http://localhost:8080/products",
    "items": [
        {
            "count": 6,
            "value": {
                "this": "http://localhost:8080/product-lines/classic-cars",
                "label": "Classic Cars"
            }
        },
        {
            "count": 1,
            "value": {
                "this": "http://localhost:8080/product-lines/planes",
                "label": "Planes"
            }
        },
        
        ⋮
      
    ]
}
```

