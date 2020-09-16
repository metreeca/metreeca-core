---
title:      REST Faceted API Search Reference
excerpt:    REST faceted API search support and query serializations
---

REST APIs publishing model-driven [LDP Containers](https://www.w3.org/TR/ldp/#ldpc) support engine-managed faceted search capabilities, driven by structural and typing constraints specified in the underlying linked data model.

Faceted searches return an RDF description of the query-specific result set. Standard content negotiation is supported for the RDF payload through the `Accept` HTTP request header. In the following sections, RDF structures for query responses are outlined using the [idiomatic JSON](idiomatic-json) format (`application/json`  MIME type).

# Queries

Linked data [queries](../javadocs/com/metreeca/tree/Query.html) define what kind of results is expected from faceted searches on [readable](../tutorials/interacting-with-ldp-apis#read-operations) linked data REST APIs.

JSON query serialization extends the [idiomatic JSON](idiomatic-json) format with  query-specific objects for serializing facet [filters](#facet-filters) and property [paths](#property-paths). Standard JSON serialization applies to all RDF terms appearing in filters, including [shorthands](idiomatic-json#literals) for numeric values and literals with provable datatypes.

<p class="warning">Work in progress… specs to be improved and detailed.</p>
JSON-based queries are appended to container IRIs using one of the following encodings:

- [URLEncoded](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURI) (e.g. `"http://example.com/container/?"+encodeURI({ <query> })`)
- [URLSearchParams](https://developer.mozilla.org/en-US/docs/Web/API/URLSearchParams) (e.g. `"http://example.com/container/?"+new URLSearchParams({ <query> })`)

The second form supports idiomatic collection filtering (e.g. `http://example.com/container/?<property>=<value>&…`, but requires:

- values to contain no comma;
- boolean, numeric and other literal properties to be specified as such in the driving shape.

## Items Query

[Items](../javadocs/com/metreeca/tree/queries/Items.html) queries return the RDF description of container items matching a set of facet filters.

    <items query> ::= { // all fields are optional and nullable
    
        "<filter>": <term> | [<term>, …],
        
        ⋮
        
        "_order": <criterion> | [<criterion>,(<criterion>)*],
        "_offset": <integer>,
        "_limit": <integer>
        
    }
    
    <criterion> :;= "[-+]?<path>"

```
<items response> ::= {
    "@id": "<target-iri>"
    "contains": [<term>(, <term>)*]
}
```

## Terms Query

[Terms](../javadocs/com/metreeca/tree/queries/Terms.html) queries return an RDF report detailing option values and counts for a facet specified by a target property path, taking into account applied filters.

    <terms query> ::= {
            
        "<filter>": <term> | [<term>, …],  // optional and nullable
        
        ⋮
    
        "_terms": "<path>"
    
    }

```
<terms response> ::= {

    "@id": "<target-iri>"
        
    "terms": [ // sorted by descending count
        {
            "value": { "@id": <iri>[, "label": "<label>"]} | <literal>,
            "count": <number>
        }
    ]
}
```

## Stats Query

[Stats](../javadocs/com/metreeca/tree/queries/Stats.html) queries return an RDF report detailing datatype, count and range stats for a facet specified by a target property path, taking into account applied filters.

```
<stats query> ::= {
    
    "<filter>": <term> | [<term>, …],  // optional and nullable
    
    ⋮

    "_stats": "<path>"
    
}
```

```
<stats response> ::= {

    "@id": "<target-iri>"
    
    // global stats 
    
    "count": <number>,
    "min": <term>,
    "max": <term>,
    
    // datatype-specific stats sorted by descending count
    
    "stats": [
        {
            "@id": "<datatype-iri>",
            "count": <number>,
            "min": <term>,
            "max": <term>
        }
    ]
}
```

# Extended JSON

## Facet Filters

```
<filter> ::= {

    "^ <path>": "<datatype>", // datatype
    "@ <path>": "<class>", // class
    "% <path>": <term> | [<term>(, <term>)*], // range


    "> <path>": <term>, // minExclusive
    "< <path>: <term>, // maxExclusive
    ">= <path>": <term>, // minInclusive
    "<= <path>": <term>, // maxInclusive
    
    "$> <path>": <term>, // minLength
    "$< <path>": <term>, // maxLength
    
    "* <path>": "pattern", // pattern (regular expression matching)
    "~ <path>": "keywords", // like (stemmed word search)
    
    "#> <path>": <integer>, // minCount
    "#< <path>": <integer>, // maxCount
    
    "! <path>": <term> | [<term>(, <term>)*], // all
    "? <path>": <term> | [<term>(, <term>)*], // any
        
    "<path>": <term>, //  shorthand for "? <path": <term>
    "<path>": [<term>(, <term>)*] // shorthand for "? <path>": [<term>(, <term>)*]
    
}
```

## Property Paths

```
<path> ::= (<alias> ('.' <alias>)*)?
```
