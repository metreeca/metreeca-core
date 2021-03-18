---
title:		JSON-LD Serialization Reference
excerpt:	JSON-LD serialization format grammar
---

Metreeca/Link generates and consumes linked data using a compacted/framed [JSON-LD](https://json-ld.org) format, which streamlines resource descriptions taking into account the constraints specified by a target linked data [shape](spec-language.md#shapes).

!!! note
	This serialization format is intended to simplify front-end development by converting linked data descriptions to/from idiomatic JSON objects structured according to the conventions a JavaScript developer would expect from a typical REST/JSON API.

JSON objects are deserialized to the corresponding RDF payload performing a depth-first visit of the JSON value structure. References to previously visited blank nodes and IRI references are represented as simplified *back-references* to the complete representation, omitting predicate values.

	<rdf> ::= <iri>

The top-level object for the JSON serialization is a single RDF value describing the root resource.

# RDF Values

```
<value> ::= <bnode> | <iri> | <literal>
```

RDF values are serialized to different JSON value patterns according to their kind.

# Blank Nodes

	<blank> ::= {  "@id" : "_:<id>" (, <property>)* }

Blank nodes descriptions are serialized as JSON objects including a JSON field for the node identifier and a JSON field for each exposed property.

```
<blank> ::= { [<property> (, <property>)*] }
```

If there is no back-reference from a nested object, the `@id` id field may be left empty or omitted.

## Back-Links

```
<blank> ::= { "@id": "_:<id>" }
```

If the value is a back-link to an enclosing blank node, only the `@id` id field is included.

```
<blank> ::= "_:<id>"
```

If the value may be proved to be  a back-reference to an enclosing resource, the node id may be inlined.

# IRI References

```
<iri> ::= { "@id" : "<iri>" (, <property>)* }
```

IRI reference descriptions are serialized as JSON objects including a JSON field for the resource IRI and a JSON field for each exposed property.

```
<iri> ::= { [<property> (, <property>)*] }
```

If the value may be proved to be a constant known IRI reference, the `@id` id field may be omitted.

```
<iri> ::= "<iri>"
```

If the value may be proved to be an IRI reference without properties, the IRI may be inlined.

## Back-Links

```
<iri> ::= { "@id": "<iri>" }
```

If the value is a back-reference to an enclosing object, only the `@id` id field is included.

```
<iri> ::= "<iri>"
```

If the value may be proved to be  a back-reference to an enclosing resource, the IRI may be inlined.

## Decoding

When decoding, relative `<iri>` references are resolved against the provided base URI, which for HTTP REST operations equals the IRI of the request [item](../javadocs/com/metreeca/rest/Message.html#item--).

## Encoding

When writing, local `<iri>` references are relativized as root-relative IRIs against the provide base URI, which for HTTP REST operations equals the root IRI of the response [item](../javadocs/com/metreeca/rest/Message.html#item--).

# Properties

```
<property> ::= <label>: [<value>(, <value>)*]
```

Direct/inverse resource property values are serialized as JSON object fields including the property *label* and a JSON
array containing serialized property objects/subjects as value.

```
<label> ::= <shape-defined label> | <system-inferred-label>
```

Property labels are either defined in the target shape using
the [label](../javadocs/com/metreeca/json/shapes/Meta.html#label-java.lang.String-) annotation or inferred by the system
on the basis of the property IRI.

!!! warning JSON-LD keywords (i.e. object field names staring with `@`) are reserved for system use.

!!! warning Predicate IRIs with undefined or clashing labels are reported as errors.

```
<property> ::= <label>: <value>
```

If  the property value may be proved to be non-repeatable, it may be included as a single JSON value, rather than a JSON array.

# Literals

```
"<text>"^^<type> ::= { "@value": "<text>", "@type": "<type>" }
"<text>"@<lang>  ::= { "@value": "<text>", "@language": "<lang>" }
```

In the more general form, literals are serialized as JSON objects including the literal lexical representation and either the literal datatype IRI or the literal language tag.

## Typed Literals

```
"<text>"             ::= "<text>"
"<text>"^^xsd:string ::= "<text>
```

Simple literals and typed `xsd:string` literals are serialized as JSON string values.

```
"<integer>"^^xsd:integer ::= <integer> # no decimal part
"<decimal>"^^xsd:decimal ::= <decimal> # decimal part

"<number>"^^<type> ::= { "@value": "<number>", "@type": "<type>" } # explicit type
```

Typed `xsd:integer` and `xsd:decimal` literals are serialized as JSON numeric values using type-specific number formats. Other typed numeric literals are serialized in the extended form.

```
"boolean"^^xsd:boolean ::= <boolean>
```

Typed `xsd:boolean` literals are serialized as JSON boolean values.

	"<text>"^^<type> ::= "<text>"

If the datatype of the literal may be proved to be a constant known value, the literal may be serialized as a JSON string value including its lexical representation, omitting datatype info.

## Tagged Literals

```
"<text0>"@"<lang1>", "<text1>"@"<lang1>", "<text2>"@"<lang2>", … ::= { 
	"<lang1>" : ["<text0>", "<text1>"],
	"<lang2>" : ["<text2>"],
	…
} 
```

If collection of literals may be proved to be `rdf:langString`, the collections may be serialized as a JSON object mapping  language tags to lists of string values.

```
"<text1>"@"<lang1>", "<text2>"@"<lang2>", … ::= { 
	"<lang1>" : "<text1>",
	"<lang2>" : "<text2>",
	…
} 
```

If language tags may be proved to be unique in the collection, string values may be included without wraping them in a list.

```
"<text1>"@"<lang>", "<text2>"@"<lang>", … ::= ["<text1>","<text2>", …]
```

If the language tag may be proved to be a constant, string values may be serialized as a JSON list, omitting language tags.

```
"<text>"@"<lang>" ::= "<text"
```

If the tagged literal may be proved to be non-repeatable and with a known language tag, its string value may be included directly.