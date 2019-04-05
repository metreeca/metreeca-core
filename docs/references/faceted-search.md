---
title:      REST Faceted API Search Reference
excerpt:    REST faceted API search support and query serializations
---

REST APIs publishing model-driven [LDP Containers](https://www.w3.org/TR/ldp/#ldpc) support engine-managed faceted search capabilities, driven by structural and typing constraints specified in the underlying linked data model.

<!-- document support for other query serializations -->

Faceted searches return an RDF description of the query-specific result set. Standard content negotiation is supported for the RDF payload through the `Accept` HTTP request header. In the following sections, RDF structures for query responses are outlined using the [idiomatic JSON](idiomatic-json) format (`application/json`  MIME type).

# Queries

Linked data [queries](../javadocs/com/metreeca/form/Query.html) define what kind of results is expected from faceted searches on [readable](../how-to/interact-with-ldp-apis#read-operations) linked data REST APIs.

JSON query serialization extends the [idiomatic JSON](idiomatic-json) format with  query-specific objects for serializing facet [filters](#facet-filters) and property [paths](#property-paths). Standard JSON serialization applies to all RDF terms appearing in filters, including [shorthands](idiomatic-json#literals) for numeric values and literals with provable datatypes.

<p class="warning">Work in progress… specs to be improved and detailed.</p>

## Edges Query

[Edges](../javadocs/com/metreeca/form/queries/Edges.html) queries return the RDF description of container items matching a set of facet filters.

    <edges query> ::= { // all fields are optional and nullable
        "filter": <filter>,        
        "order": <criterion> | [<criterion>,(<criterion>)*],
        "offset": <integer>,
        "limit": <integer>
    }
    
    <criterion> :;= "[-+]?<path>"
    
    <edges response> ::= {
        "this": "<target-iri>"
        "contains": [<term>(, <term>)*]
    }

## Stats Query

[Stats](../javadocs/com/metreeca/form/queries/Stats.html) queries return an RDF report detailing datatype, count and range stats for a facet specified by a target property path, taking into account applied filters.

```
<stats query> ::= {
    "stats": "<path>",
    "filter": <filter> // optional and nullable
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

[Items](../javadocs/com/metreeca/form/queries/Items.html) queries return an RDF report detailing option values and counts for a facet specified by a target property path, taking into account applied filters.

    <items query> ::= {
        "items": "<path>",
        "filter": <filter> // optional and nullable
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
