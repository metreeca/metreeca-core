---
title: 		How To Improve User Experience
excerpt:	Usability guidelines and other configuration tips
tags:		How-To
module:     "Metreeca Linked Data Hub"
version:    "0.46"
---

# Data Usability

Metreeca linked data interfaces will search and navigate generic RDF data from any SPARQL 1.1 endpoint, but user experience can be improved by sticking to the following guidelines.

## Identify Key Collections

When opening a new report, Metreeca will present the user with a springboard page, listing collections identified by introspecting *rdf:type* info from the endpoint.

To fine-tune what's presented to the user or to improve performance on large datasets, you may explicitly identify RDF classes as key collections for your *dataset* by annotating them as:

    <{dataset}> <http://rdfs.org/ns/void#rootResource> <{class}> .

## Provide Labels

Provide human-readable labels for every graph entity by annotating them as:

    <uri> rdfs:label "label" . 
    <uri> rdfs:label "label"@en .

Metreeca will scan for available generic and english-tagged labels, in that order.

If no labels are provided, Metreeca will try to infer human-readable labels for URIs according to well-known slash/hash patterns (e.g. 'property' for [http://example.com/schema\#property](http://example.com/)).

-   *A configurable option for specifying other properties and preferred language tags might be added in a future release.*

## Provide Comments

Where relevant, provide human-readable descriptions for graph entities by annotating them as:

    <uri> rdfs:comment "comment" . 
    <uri> rdfs:comment "comment"@en .

Metreeca will scan for available generic and english-tagged notes, in that order.

-   *A configurable option for specifying other properties and preferred language tags might be added in a future release.*

## Provide Pictures

Where relevant, provide visual depictions for graph entities by annotating them as:

    <uri> db:thumbnail <picture-uri>/"picture-uri".   
    <uri> schema:image <picture-uri>/"picture-uri".    
    <uri> foaf:logo <picture-uri>/"picture-uri".
    <uri> foaf:depiction <picture-uri>/"picture-uri".

Metreeca will scan for available pictures in this order.

-   *Other well-known picture properties and a configurable option for     specifying custom ones might be added in a future release.*

## Provide Coordinates

Where relevant, provide geographic coordinates for graph entities by annotating them using the [Basic Geo (WGS84 lat/long) Vocabulary](http://www.w3.org/2003/01/geo/#vocabulary) as:

    <uri> geo:lat "latitude"; geo:long "longitude" .

## Include Ontologies

Upload relevant ontologies to the endpoint: Metreeca will use human-readable labels and notes for classes and predicates to configure fields and facets names and linked data pages.

## Include Inverse Links

Metreeca supports the creation of property paths involving inverse steps (*\^&lt;predicate&gt;*), but they may be very confusing even for experienced users: wherever feasible, explicitly document bidirectional relationships between graph entities using pairs of inverse predicates.

-   *Automatic handling of inverse steps based on owl:inverseOf annotations might be added in a future release.*

## Avoid Blank Nodes

Until a consistent and efficient way to skolemize SPARQL results sets is found, blank nodes should be avoided, as Metreeca won't be able to navigate past them.

Wherever feasible, replace blank nodes with named resources either identified by dereferenceable *http:* URIs or tagged within a suitable URI namespace (e.g. [RFC 4151 - The 'tag' URI Scheme](http://tools.ietf.org/html/rfc4151), [RFC 4122 - A Universally Unique IDentifier (UUID) URN Namespace](http://tools.ietf.org/html/rfc4122)).

In any case, provide human-readable labels summarizing the information hidden beyond blank nodes or embedded within structured intermediate resources.
