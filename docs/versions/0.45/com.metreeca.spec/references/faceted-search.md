---
title:      Faceted Search Reference
excerpt:    "Faceted search support and query serializations"
tags:       Reference
module:     "Metreeca Shapes Engine"
version:    "0.45"
---

REST APIs publishing LDP Containers support system-managed faceted search capabilities, driven by structural and typing constraints specified in the underlying linked data model.

Faceted searches are performed with a `GET` operation on the URL identifying the target resource, appending a URL-encoded JSON query object describing the submitted query.

<!-- document support for other query serializations -->

Faceted searches return an RDF description of the query-specific result set. Standard content negotiation is supported for the RDF payload through the `Accept` HTTP request header. In the following sections, RDF structures for query responses are outlined using the idiomatic [JSON](idiomatic-json) format (`application/json`  MIME type).

# Queries

Linked data [queries](../apidocs/com/metreeca/spec/Query.html) define what kind of results is expected from faceted searches on [readable](../../com.metreeca.data/tutorials/linked-data-development/#read-operations) linked data REST APIs.

JSON query serialization extends the idiomatic [JSON](idiomatic-json) format with  query-specific objects for serializing facet [filters](#facet-filters) and property [paths](#property-paths). Standard JSON serialization applies to all RDF terms appearing in filters, including [shorthands](idiomatic-json#literals) for numeric values and literals with provable datatypes.

<p class="warning">Work in progress… specs to be improved and detailed.</p> 

## Graph Query

[Graph](../apidocs/com/metreeca/spec/queries/Graph.html) queries return the RDF description of container items matching a set of facet filters.

    <graph query> ::= { // all fields are optional
        "filter": <filter>,        
        "order": <criterion> | [<criterion>,(<criterion>)*],
        "offset": <integer>,
        "limit": <integer>
    }
    
    <criterion> :;= "[-+]?<path>"
    
    <graph response> ::= {
        "this": "<target-iri>"
        "contains": [<term>(, <term>)*]
    }

## Stats Query

[Stats](../apidocs/com/metreeca/spec/queries/Stats.html) queries return an RDF report detailing datatype, count and range stats for a facet specified by a target property path, taking into account applied filters.

```
<stats query> ::= {
    "stats": "<path>",
      "filter": <filter> // optional
}
```

```
<stats response> ::= {

    "this": "<target-iri>"
    
    // global stats 
    
    "count": <number>,
    "min": <term>,
    "max": <term>,
    
    // datatype-specific stats sorted by descending count
    
    "stats": [
        {
            "this": "<datatype-iri>",
            "count": <number>,
            "min": <term>,
            "max": <term>
        }
    ]
}
```

## Items Query

[Items](../apidocs/com/metreeca/spec/queries/Items.html) queries return an RDF report detailing option values and counts for a facet specified by a target property path, taking into account applied filters.

    <items query> ::= {
        "filter": <filter>,
        "items": "<path>" // optional
    }
    
    <items response> ::= {
    
        "this": "<target-iri>"
            
        // items sorted by descending count
        
        "items": [
            {
                "value": <term>, // resources are labelled
                "count": <number>
            }
        ]
    }

# Extended JSON

## Facet Filters


    <filter> ::= {

        "^": <datatype>, // datatype
        "@": <class>, // class
        
        ">": <term>, // minExclusive
        "<": <term>, // maxExclusive
        ">=": <term>, // minInclusive
        "<=": <term>, // maxInclusive
        
        ">#": <object>, // minLength
        "#<": <object>, // maxLength
                
        "*": "pattern", // pattern (regular expression matching)
        "~": "keywords", // like (stemmed word search)
    
        ">>": <term>, // minCount
        "<<": <term>, // maxCount
        
        "!": <term> | [<term>(, <term>)*], // all
        "?": <term> | [<term>(, <term>)*], // any
    
        "<path>": <filter>, // nested property filter
        
        "<path>": <term>, // nested filter shorthand for { "?": <term> }
        "<path>": [<term>(, <term>)*] // nested filter shorthand for { "?": [<term>(, <term>)*] }
        
    }

## Property Paths

```
<path> ::= (<iri> | ^<iri>)?
         | (<step> ('/' <step>)*)?
         | (<alias> ('.' <alias>)*)?
         
<step> ::= <<iri>> | ^<<iri>> | <alias>
```
