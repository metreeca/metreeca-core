# Query String

- JSON Object
- URLEncoded JSON object
- URLSearchParams encoded JSON object (https://developer.mozilla.org/en-US/docs/Web/API/URLSearchParams)
    - beware of commas in values
    - numeric/boolean values must be specified as such in shape
    
# Items Query

    <items query> ::= { // all fields are optional and nullable
    
        "_order": <criterion> | [<criterion>,(<criterion>)*],
        "_offset": <integer>,
        "_limit": <integer>
        
        "<filter>": <value> | [<value>(, <value>)*],
        
        …
        
    }

    <criterion> ::= "[-+]?<path>"

    <items response> ::= {
        "id": "<target>"
        "contains": [<item>(, <item>)*]
    }
    
    
# Terms Query

    <items response> ::= {
        "id": "<target>"
        "terms": [<term>(, <term>)*]
    }
    
    <term> ::= {
        "value": item,
        "count": <count>
    }

# Stats Query
 
 
# Facet Filters

```
<filter> ::= {

    "^ <path>": "<datatype>", // datatype
    "@ <path>": "<class>", // class
    
    "> <path>": <value>, // minExclusive
    "< <path>: <value>, // maxExclusive
    ">= <path>": <value>, // minInclusive
    "<= <path>": <value>, // maxInclusive
    
    "$> <path>": <value>, // minLength
    "$< <path>": <value>, // maxLength
    
    "* <path>": "pattern", // pattern (regular expression matching)
    "~ <path>": "keywords", // like (stemmed word search)
    
    "#> <path>": <integer>, // minCount
    "#< <path>": <integer>, // maxCount
    
    "% <path>": <value> | [<value>(, <value>)*], // in
    "! <path>": <value> | [<value>(, <value>)*], // all
    "? <path>": <value> | [<value>(, <value>)*], // any
        
    "<path>": <value>, //  shorthand for { "?": <value> }
    "<path>": [<value>(, <value>)*] // shorthand for { "?": [<value>(, <value>)*] }
    
}
```

## Property Paths

```
<path> ::= (<step> (('.'|'/') <step>)*)? # whitespace around steps ignored

<step> ::= "[:\w]+" | "'[^']*'"
```
