---
title:     Idiomatic JSON Reference
excerpt:    "Idiomatic RDF JSON serialization format codecs and grammar"
tags:       Reference
module:     "Metreeca Shapes Engine"
version:    "0.0"
---

Beside the standardized  [JSON-LD](https://www.w3.org/TR/json-ld/) RDF serialization, the platform supports a simpler idiomatic JSON-based format, which streamlines resource descriptions taking into account the constraints specified by a target linked data [shape](spec-language#shapes).

Codecs for this serialization make heavy use of reasoning over linked data shapes to **prove** useful features of the RDF payload, like the expected value for a property being a IRI reference or a required non-repeatable string.

<p class="note">This serialization format is intended to simplify front-end development by converting RDF descriptions to/from idiomatic JSON objects structured according to the conventions a JavaScript developer would expect from a typical REST/JSON API. Unlike JSON-LD,  it doesn't cater to roundtrip de/serialization of RDF payloads without access to the target shape.</p>

<p class="note">Reformulation of the idiomatic JSON format as an application-specific JSON-LD profile is being explored.</p>

# RDF4J Codecs

The [shapes engine](/#module=Shapes%20Engine) library automatically [registers](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/common/lang/service/ServiceRegistry.html)  idiomatic JSON codecs with the RDF4J framework, using the `application/json` MIME type and the [JSON](../javadocs/com/metreeca/spec/codecs/JSONAdapter.html) RDF4J [RDF format](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/rio/RDFFormat.html).

Codec behaviour is controlled through the following  RDF4J [RioSetting](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/rio/RioSetting.html) [parser](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/rio/RDFParser.html#set-org.eclipse.rdf4j.rio.RioSetting-T-)/[writer](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/rio/RDFWriter.html#set-org.eclipse.rdf4j.rio.RioSetting-T-) configuration properties.

| setting                                  | type                                     | value                                    | default                             |
| ---------------------------------------- | ---------------------------------------- | ---------------------------------------- | ----------------------------------- |
| [Shape](../javadocs/com/metreeca/spec/codecs/JSONAdapter.html#Shape) | [Shape](../javadocs/com/metreeca/spec/shape) | the target shape for the resources to be de/serialized | `null` (will be inferred from data) |
| [Focus](../javadocs/com/metreeca/spec/codecs/JSONAdapter.html#Focus) | [Resource](http://docs.rdf4j.org/javadoc/latest/org/eclipse/rdf4j/model/Resource.html) | the entry point for the de/serializaton process | `null` (will be inferred from data) |

# JSON Serialization

JSON objects are deserialized to the corresponding RDF payload performing a depth-first visit of the JSON value structure. References to previously visited blank nodes and IRI references are represented as simplified *back-references* to the complete representation, omitting predicate values.

## Top-Level Object

```
<rdf> ::= [<term>(, <term>)*]
```

The top-level object for the JSON serialization is an array containing a JSON value for each subject in the RDF payload.

	<rdf> ::= <term>

If the payload can proved to always contain a single subject or if it contains a single subject equal to the *focus* of the serializing operation, the array is simplified to a single JSON value.

## RDF Terms

```
<term> ::= <blank> | <reference> | <literal>
```

RDF terms are serialized to different JSON value patterns according to their kind.

## Blank Nodes

	<blank> ::= {  "this" : "_:<id>" (, <property>)* }

Blank nodes descriptions are serialized as JSON objects including a JSON field for the node identifier and a JSON field for each exposed node property.

```
<blank> ::= {  "this" : "" (, <property>)* }
<blank> ::= {  "this" : null (, <property>)* }
```

if there is no back-reference from a nested object, the `this` id field may be left empty or null.

```
<blank> ::= {  <property>(, <property>)* }
```

If the term may be proved to be blank and there is no back-reference from a nested object, the `this` id field may be omitted.

```
<blank> ::= { "this": "_:<id>" }
```

If the object is a back-reference to an enclosing object, only the `this` id field is included.

## IRI References

```
<iri> ::= { "this" : "<iri>" (, <property>)* }
```

IRI reference descriptions are serialized as JSON objects including a JSON field for the term IRI and a JSON field for each exposed term property.

```
<iri> ::= {  <property>(, <property>)* }
```

If the term may be proved to be a constant known IRI reference, the `this` id field may be omitted.

```
<iri> ::= { "this": "<iri>" }
```

If the object is a back-reference to an enclosing object, only the `this` id field is included.

## Term Properties

```
<property> ::= <path>: [<term>(, <term>)*]
```

Direct/inverse RDF property values are serialized as JSON object fields including the property path as label and a JSON array containing serialized property objects/subjects as value.

If a target shape is provided to the codec, only direct/inverse properties specified in the shape are included in the description; otherwise,  only direct properties are included.

```
<property> ::= <path>: <term>
```

If  the property value may be proved to be non-repeatable, it may be included as a single JSON value, rather than a JSON array.


	<path> ::= "<<iri>>" | "^<<iri>>" | "<iri>" | "^<iri>" | "<alias>"

Predicate IRIs are represented as strings, in either plain or angle bracket notation. Predicate IRIs for inverse RDF properties are prefixed with a caret charatecter (`^`).

If a shape is provided to the codec, predicate IRIs are reported in a shortened form using user-defined or system-inferred  [aliases](spec-language#annotations). Predicate IRIs with clashing aliases are written in full using the angle bracket notation.

The `this` label is reserved for system use.

## Literals

```
"<text>"^^<type> ::= { "text": "<text>", "type": "<type>" }
"<text>"@<lang>  ::= { "text": "<text>", "lang": "<lang>" }
```

In the more general form, literals are serialized as JSON objects including the literal lexical representation and either the literal datatype IRI or the literal language tag.

```
"boolean"^^xsd:boolean ::= <boolean>
```

Typed `xsd:boolean` literals ar serialized as JSON boolean values.

```
"<text>"             ::= "<text>"
"<text>"^^xsd:string ::= "<text>
```

Simple literals and typed `xsd:string` literals ar serialized as JSON string values.

```
"<integer>"^^xsd:integer ::= <integer> # no decimal part
"<decimal>"^^xsd:decimal ::= <decimal> # decimal part
"<float>"^^xsd:float     ::= <float> # exponent notation

"<number>"^^<type> ::= { "text": "<number>", "type": "<type>" } # explicit type
```

Typed `xsd:integer`, `xsd:decimal` and `xsd:float` literals ar serialized as JSON numeric values using type-specific number formats. Other typed numeric literals are serialized in the extended form.

	"<text>"^^<type> ::= "<text>"

If the datatype of the literal may be proved to be a constant known value, the literal may be serialized as a JSON string value including its lexical representation, omitting datatype info.
