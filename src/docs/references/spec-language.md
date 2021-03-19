---
title:      Shape Specification Language Reference
---

[comment]: <> (excerpt:    Shape-based data modelling language semantics and components)


Most framework services are driven by declarative linked data models defined using a [shape](#shapes)-based specification
language.

Models are [programmaticaly](../tutorials/publishing-jsonld-apis.md) built using a Java‑based DSL and can eventually
automate a range of different tasks in the lifecycle of linked data REST APIs.

| task              | shape role                                                   |
| ----------------- | ------------------------------------------------------------ |
| data selection    | JSON-LD data retrieved from [readable](../tutorials/consuming-jsonld-apis.md#read-operations) linked data REST APIs is selected from the underlying graph storage layer by  queries derived from the associated linked data models |
| data validation   | JSON-LD data submitted to [writable](../tutorials/consuming-jsonld-apis.md#write-operations) linked data REST APIs on resource creation and updating is automatically validated against the associated linked data models |
| faceted search    | [faceted search](../tutorials/consuming-jsonld-apis.md#faceted-search) and ancillary facet-populating queries are managed by the system on the basis of structural and typing constraints specified in linked data models associated with target linked data REST APIs |
| API documentation | Human and machine readable docs for linked data REST APIs may be dynamically derived from associated linked data models and published as hypertexts and [OpenAPI](https://www.openapis.org)/[Swagger](https://swagger.io/specification/) specs |
| data ingestion    | Data ingested from external data sources may be dynamically mapped to and validated against the linked data models associated with target graphs |

!!! note Some of the described features aren't (yet ;-) supported by the framework.

# Shapes

Linked data [shapes](../javadocs/com/metreeca/json/Shape.html) define the expected structure of RDF resource descriptions in terms of **constraints** imposed on members of a **focus set** of JSON-LD values and on the values of their properties.

**Primitive** shapes specify declarative constraints to be meet by the focus set and its member values. **Composite** shapes recursively assemble other shapes into tree-structured complex constraints.

Linked data selection tasks identify the set of nodes in a graph whose descriptions are consistent with a possibly composite shape. Selection results are reported either as a graph or a structured report according to the choosen [query](faceted-search.md#queries) type.

Linked data validation tasks verify that the description of an initial focus set is consistent with the constraints specified by a possibly composite shape. During the process, derived focus sets connected by [structural constraints](#structural-constraints) may be recursively validated. Validation results are reported as a structured [focus validation trace](../javadocs/com/metreeca/json/Trace.html).

## Value Constraints

Primitive shapes specifying constraints to be individually satisfied by each value in the focus set.

| shape                                                        | constraint                                                   |
| :----------------------------------------------------------- | :----------------------------------------------------------- |
| [datatype](../javadocs/com/metreeca/json/shapes/Datatype.html)(IRI) | each value in the focus set has a given extended RDF datatype IRI; IRI references and blank nodes are considered to be respectively of [Values.IRI](../javadocs/com/metreeca/json/Values.html#IRIType) and [Values.BNode](../javadocs/com/metreeca/json/Values.html#BNodeType) datatype |
| [class](../javadocs/com/metreeca/json/shapes/Clazz.html)(IRI) | each value in the focus set is an instance of a given RDF class or one of its superclasses |
| [range](../javadocs/com/metreeca/json/shapes/Range.html)(value, …) | each value in the focus set is included in a given set of target values |
| [lang](../javadocs/com/metreeca/json/shapes/Lang.html)(tag, …)                                             | each value in the focus set is a tagged literal in a given set of target languages |
| [minExclusive](../javadocs/com/metreeca/json/shapes/MinExclusive.html)(value) | each value in the focus set is strictly greater than a given minum value, according to <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modOrderBy">SPARQL ordering</a> rules |
| [maxExclusive](../javadocs/com/metreeca/json/shapes/MaxExclusive.html)(value) | each value in the focus set is strictly less than a given maximum value, according to <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modOrderBy">SPARQL ordering</a> rules |
| [minInclusive](../javadocs/com/metreeca/json/shapes/MinInclusive.html)(value) | each value in the focus set is greater than or equal to a given minimum value, according to <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modOrderBy">SPARQL ordering</a> rules |
| [maxInclusive](../javadocs/com/metreeca/json/shapes/MaxInclusive.html)(value) | each value in the focus set is less than or equal to a given maximum value, according to <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modOrderBy">SPARQL ordering</a> rules |
| [minLength](../javadocs/com/metreeca/json/shapes/MinLength.html)(length) | the length of the lexical representation of each value in the focus set is greater than or equal to the given minimum value |
| [maxLength](../javadocs/com/metreeca/json/shapes/MaxLength.html)(length) | the length of the lexical representation of each value in the focus set is less than or equal to the given maximum value |
| [pattern](../javadocs/com/metreeca/json/shapes/Pattern.html)("pattern") | the lexical representation of each value in the focus set matches a given [regular expression](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#sum) pattern |
| [like](../javadocs/com/metreeca/json/shapes/Like.html)("keywords") | the lexical representation of each value in the focus set matches the given full-text keywords |
| [stem](../javadocs/com/metreeca/json/shapes/Stem.html)("prefix") | the lexical representation of each value in the focus set starts with the given prefix |

## Set Constraints

Primitive shapes specifying constraints to be collectively satisfied by the values in the focus set.

| shape                                                        | constraint                                                   |
| :----------------------------------------------------------- | :----------------------------------------------------------- |
| [minCount](../javadocs/com/metreeca/json/shapes/MinCount.html)(count) | the size of the focus set is greater than or equal to the given minimum value |
| [maxCount](../javadocs/com/metreeca/json/shapes/MaxCount.html)(count) | the size of the focus set is less than or equal to the given maximum value |
| [all](../javadocs/com/metreeca/json/shapes/All.html)(value, …) | the focus set includes all values from a given set of target values |
| [any](../javadocs/com/metreeca/json/shapes/Any.html)(value, …) | the focus set includes at least one value from a given set of target values |
| [localized](../javadocs/com/metreeca/json/shapes/Localized.html)()                                              | the focus set contains only tagged literals with at most one value for each language tag |

Common combinations of set constraints are directly available as shorthand shapes.

| shorthand shape                                              | equivalent shape                      | constraint                  |
| :----------------------------------------------------------- | :------------------------------------ | --------------------------- |
| [required()](../javadocs/com/metreeca/json/Shape.html#required--) | `and(minCount(1), maxCount(1))`       | exactly one                 |
| [optional()](../javadocs/com/metreeca/json/Shape.html#optional--) | `maxCount(1)`                         | at most one                 |
| [repeatable](../javadocs/com/metreeca/json/Shape.html#repeatable--) | `minCount(1)`                         | at least one                |
| [multiple()](../javadocs/com/metreeca/json/Shape.html#multiple--) | `and()`                               | any number                  |
| [exactly](../javadocs/com/metreeca/json/Shape.html#exactly-org.eclipse.rdf4j.model.Value...-)(value, …) | `and(all(value, …), range(value, …))` | constant pre-defined values |

## Structural Constraints

Composite shapes specifying constraints to be satisfied by a derived focus set generated by a path.

| shape                                                        | constraint                                                   |
| :----------------------------------------------------------- | :----------------------------------------------------------- |
| [field](../javadocs/com/metreeca/json/shapes/Field.html)(["label", ] IRI, [shape](../javadocs/com/metreeca/json/Shape.html), …) | the derived focus set generated by traversing a single step path is consistent with a given set of shapes |
| [link](../javadocs/com/metreeca/json/shapes/Link.html)(IRI, [shape](../javadocs/com/metreeca/json/Shape.html), …) | the derived focus set generated by virtually traversing a single step path linking a resource alias to its target is consistent with a given set of shapes |

## Logical Constraints

Composite shapes specifying logical combinations of shapes.

| shape                                                        | constraint                                                   |
| :----------------------------------------------------------- | :----------------------------------------------------------- |
| [guard](../javadocs/com/metreeca/json/shapes/Guard.html)(axis, value, …) | the focus set is consistent with this shape only if the value of an externally assigned [axis variable](../javadocs/com/metreeca/json/Shape.html#redact-java.lang.String-java.util.Collection-) is included in a given set of target values |
| [when](../javadocs/com/metreeca/json/shapes/When.html)([test](../javadocs/com/metreeca/json/Shape.html),[pass](../javadocs/com/metreeca/json/Shape.html) [, [fail](../javadocs/com/metreeca/json/Shape.html)]) | the focus set is consistent either with a `pass` shape, if consistent also with a `test` shape, or with a `fail` shape, otherwise; if omitted, the `fail` shape defaults to `and()`, that is it's always meet |
| [and](../javadocs/com/metreeca/json/shapes/And.html)([shape](../javadocs/com/metreeca/json/Shape.html), …) | the focus set is consistent with all shapes in a given target set |
| [or](../javadocs/com/metreeca/json/shapes/Or.html)([shape](../javadocs/com/metreeca/json/Shape.html), …) | the focus set is consistent with at least one shape in a given target set |

# Parameters

The combined use of conditional (`when`) and parametric (`guard`)  constraints supports the definition of **parametric** shapes, which specify different sets of constraints according to the current externally-assigned value of parametric **axis** variables.

Parametric axes may be specified for arbitrary custom variables, but the system relies on four pre‑defined parametric variables to support fine‑grained access control rules and role‑dependent read/write resource views.

Common combinations of parametric shapes on these axes are directly available as shorthand shapes.

Guards may be applied to groups of shapes either explicitely assembly a `when` shape, like:

```
when(<guard>, and(<shape>, …))
```

or using the shorthand [then](../javadocs/com/metreeca/json/Shape.html#then-com.metreeca.json.Shape...-) method, like:

```
<guard>.then(<shape>, …)
```

## Role Axis

Parametric guards for the [role](../javadocs/com/metreeca/json/shapes/Guard.html#role-java.lang.Object...-) axis selectively enable target shapes according to the roles of the (possibly authenticated) user performing HTTP/S operations on target linked data resources.

| shorthand | usage context |
|-------|-------------|
|[role](../javadocs/com/metreeca/json/shapes/Guard.html#role-java.lang.Object...-)(value, …)| target shapes are to be considered only if the sets of [roles](../javadocs/com/metreeca/rest/Request.html#roles--) enabled for the user performing a request contains at least one of the given role values |

## Task Axis

Parametric guards for the [task](../javadocs/com/metreeca/json/shapes/Guard.html#task-java.lang.Object...-) axis selectively enable target shapes according to the method of the HTTP/S operations performed on target linked data resources.

| shorthand                                                    | HTTP/S method   | usage context                                                |
| ------------------------------------------------------------ | --------------- | ------------------------------------------------------------ |
| [create](../javadocs/com/metreeca/json/shapes/Guard.html#create-com.metreeca.json.Shape...-)([[shape](../javadocs/com/metreeca/json/Shape.html), …]) | POST            | resource creation                                            |
| [relate](../javadocs/com/metreeca/json/shapes/Guard.html#relate-com.metreeca.json.Shape...-)([[shape](../javadocs/com/metreeca/json/Shape.html), …]) | GET             | resource retrieval                                           |
| [update](../javadocs/com/metreeca/json/shapes/Guard.html#update-com.metreeca.json.Shape...-)([[shape](../javadocs/com/metreeca/json/Shape.html), …]) | PUT             | resource updating                                            |
| [delete](../javadocs/com/metreeca/json/shapes/Guard.html#delete-com.metreeca.json.Shape...-)([[shape](../javadocs/com/metreeca/json/Shape.html), …]) | DELETE          | resouce deletion                                             |
| [client](../javadocs/com/metreeca/json/shapes/Guard.html#client-com.metreeca.json.Shape...-)([[shape](../javadocs/com/metreeca/json/Shape.html), …]) | POST+GET+DELETE | shorthand for client-managed data, specified at creation time, but not updated afterwards |
| [server](../javadocs/com/metreeca/json/shapes/Guard.html#server-com.metreeca.json.Shape...-)([[shape](../javadocs/com/metreeca/json/Shape.html), …]) | GET+DELETE      | shorthand for server-managed data, neither specified at creation time, nor updated afterwards |

## View Axis

Parametric guards for the [view](../javadocs/com/metreeca/json/shapes/Guard.html#view-java.lang.Object...-) axis selectively enable target shapes according to the usage context.

| shorthand                                                    | usage context                                                |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [digest](../javadocs/com/metreeca/json/shapes/Guard.html#digest-com.metreeca.json.Shape...-)([[shape](../javadocs/com/metreeca/json/Shape.html), …]) | short resource description, e.g. inside search result sets   |
| [detail](../javadocs/com/metreeca/json/shapes/Guard.html#detail-com.metreeca.json.Shape...-)([[shape](../javadocs/com/metreeca/json/Shape.html), …]) | detailed resource description, e.g. inside advanced resource utilities |

## Mode Axis

Parametric guards for the [mode](../javadocs/com/metreeca/json/shapes/Guard.html#mode-java.lang.Object...-) axis selectively enable target shapes according to the usage pattern.

| shorthand                                                    | usage pattern                                                |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [convey](../javadocs/com/metreeca/json/shapes/Guard.html#convey-com.metreeca.json.Shape...-)([[shape](../javadocs/com/metreeca/json/Shape.html), …]) | target shapes are to be considered only when validating incoming data or extracting outgoing data |
| [filter](../javadocs/com/metreeca/json/shapes/Guard.html#filter-com.metreeca.json.Shape...-)([[shape](../javadocs/com/metreeca/json/Shape.html), …]) | target shapes are to be considered only when selecting resources to be processed |
