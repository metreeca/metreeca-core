---
title:      REST Faceted Search Reference
---

[comment]: <> (excerpt:    REST faceted search queries and serialization)


REST/JSON-LD APIs published with the Metreeca/Link framework support engine-managed faceted search capabilities, driven
by structural and typing constraints specified in the underlying linked data model.

# Queries

[Queries](../javadocs/com/metreeca/json/Query.html) define what kind of results is expected from faceted searches
on [readable](../tutorials/consuming-jsonld-apis.md#read-operations) linked data REST/JSON-LD APIs.

JSON query serialization extends the idiomatic [JSON-LD](jsonld-format.md) format with query-specific objects for
serializing facet [filters](#facet-filters) and property [paths](#property-paths). Standard JSON serialization applies to
all values appearing in filters, including [shorthands](jsonld-format.md#literals) for numeric values and literals with
provable datatypes.

!!! warning
	Work in progress… specs to be improved and detailed.

JSON-based queries are appended to container IRIs using one of the following encodings:

- [URLEncoded](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURI) (e.g. `"http://example.com/container/?"+encodeURI({ <query> })`)
- [URLSearchParams](https://developer.mozilla.org/en-US/docs/Web/API/URLSearchParams) (e.g. `"http://example.com/container/?"+new URLSearchParams({ <query> })`)

The second form supports idiomatic collection filtering (e.g. `http://example.com/container/?<property>=<value>&…`, but requires:

- values to contain no comma;
- boolean, numeric and other literal properties to be specified as such in the driving shape.

## Items Query

[Items](../javadocs/com/metreeca/json/queries/Items.html) queries return the description of collection items matching a set of facet filters.

    <items query> ::= { // all fields are optional and nullable
    
        "<filter>": <value> | [<value>, …],
        
        ⋮
        
        "_order": <criterion> | [<criterion>,(<criterion>)*],
        "_offset": <integer>,
        "_limit": <integer>
        
    }
    
    <criterion> :;= "[-+]?<path>"

```
<items response> ::= {
    "@id": "<target-iri>"
    "contains": [<value>(, <value>)*]
}
```

## Terms Query

[Terms](../javadocs/com/metreeca/json/queries/Terms.html) queries return a report detailing option values and counts for a facet specified by a target property path, taking into account applied filters.

    <terms query> ::= {
            
        "<filter>": <value> | [<value>, …],  // optional and nullable
        
        ⋮
    
        "_terms": "<path>",
        "_offset": <integer>,
        "_limit": <integer>
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

[Stats](../javadocs/com/metreeca/json/queries/Stats.html) queries return a report detailing datatype, count and range stats for a facet specified by a target property path, taking into account applied filters.

```
<stats query> ::= {
    
    "<filter>": <value> | [<value>, …],  // optional and nullable
    
    ⋮

    "_stats": "<path>",
    "_offset": <integer>,
    "_limit": <integer>
}
```

```
<stats response> ::= {

    "@id": "<target-iri>"
    
    // global stats 
    
    "count": <number>,
    "min": <value>,
    "max": <value>,
    
    // datatype-specific stats sorted by descending count
    
    "stats": [
        {
            "@id": "<datatype-iri>",
            "count": <number>,
            "min": <value>,
            "max": <value>
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
    "% <path>": <value> | [<value>(, <value>)*], // range


    "> <path>": <value>, // minExclusive
    "< <path>: <value>, // maxExclusive
    ">= <path>": <value>, // minInclusive
    "<= <path>": <value>, // maxInclusive
    
    "$> <path>": <value>, // minLength
    "$< <path>": <value>, // maxLength
    
    "* <path>": "pattern", // pattern (regular expression matching)
    "~ <path>": "keywords", // like (stemmed word search)
    "' <path>": "stem", // stem (prefix search)
    
    "#> <path>": <integer>, // minCount
    "#< <path>": <integer>, // maxCount
    
    "! <path>": <value> | [<value>(, <value>)*], // all
    "? <path>": <value> | [<value>(, <value>)*], // any
        
    "<path>": <value>, //  shorthand for "? <path": <value>
    "<path>": [<value>(, <value>)*] // shorthand for "? <path>": [<value>(, <value>)*]
    
}
```

## Property Paths

```
<path> ::= (<label> ('.' <label>)*)?
```
